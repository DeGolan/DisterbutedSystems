package Tools;

import org.json.JSONObject;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


import java.io.*;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class S3Helper {
    private S3Client s3;
    public S3Helper(){
        s3 = S3Client.builder().region(Region.US_EAST_1).build();
    }

    //uploading the pdf_src to the s3
    public void uploadFileToS3(String filePath, String bucket, String key){
        System.out.println("Uploading file path " + filePath + " to S3...");
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(),
                RequestBody.fromFile(Paths.get(filePath)));
        System.out.println("Upload complete");
    }

    public void downloadFile(String output,String bucket, String key){
       try{
       System.out.println("downloading the summary file from s3");
        GetObjectRequest objectRequest = GetObjectRequest
                .builder()
                .key(key)
                .bucket(bucket)
                .build();

        ResponseInputStream<GetObjectResponse> s3objectResponse=s3.getObject(objectRequest);
        BufferedReader br = new BufferedReader(new InputStreamReader(s3objectResponse));

        System.out.println("Writing the lines from the summary file to "+output+".html");
        String line;
        FileOutputStream fs = new FileOutputStream(output+".html");
        OutputStreamWriter out = new OutputStreamWriter(fs);
        out.write("<html>");
        out.write("<head>");
        out.write("<title>");
        out.write("Summary File");
        out.write("</title>");
        out.write("</head>");
        out.write("<body>");
        while((line=br.readLine()) != null) {
            out.write(line);
            out.write("<br>");
        }
        out.write("</body>");
        out.write("</html>");
       s3objectResponse.close();
        out.close();
        br.close();
    } catch (IOException e) {
        e.printStackTrace();
    }

    }
    public List<MessageProtocol> downloadPDFList(String key, String bucket) throws IOException {
        List<MessageProtocol> msgList=new LinkedList<MessageProtocol>();
        GetObjectRequest objectRequest = GetObjectRequest
                .builder()
                .key(key)
                .bucket(bucket)
                .build();

        ResponseInputStream<GetObjectResponse> s3objectResponse=s3.getObject(objectRequest);
        BufferedReader reader = new BufferedReader(new InputStreamReader(s3objectResponse));

        String line;
        while ((line = reader.readLine()) != null) {
            JSONObject json=new JSONObject();
            String task=line.substring(0,line.indexOf('\t'));
            String url=line.substring(line.indexOf('\t')+1);
            MessageProtocol msg=new MessageProtocol(task,bucket,key,0,url,"");
            msgList.add(msg);
        }
        reader.close();
        s3objectResponse.close();
        return  msgList;

    }
    public void closeS3(){
        System.out.printf("%n");
        System.out.println("Closing the connection to {S3}");
        s3.close();
        System.out.println("Connection closed");
    }
}

//"/home/vagrant/DistributedSystems/src/main/resources/input-sample-1.txt"
