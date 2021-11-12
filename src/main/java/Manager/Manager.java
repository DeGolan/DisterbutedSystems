package Manager;

import com.google.gson.Gson;
import org.json.JSONObject;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParser;

import java.io.*;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;


public class Manager {
    public static void main(String[] args) throws IOException {
        System.out.println("Manager is starting...");
        Region region = Region.US_EAST_1;
        S3Client s3 = S3Client.builder().region(region).build();
        SqsClient sqsClient = SqsClient.builder()
                .region(region)
                .build();
        List<Message> messages =receiveMessages(sqsClient,"https://sqs.us-east-1.amazonaws.com/537488554861/LocalApp-Manager");
        JSONObject json = null;
        if (messages.size()>0){
            for (Message m: messages){
                json = new JSONObject(m.body());
                //System.out.println(json.get("task"));
//                DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
//                        .queueUrl("https://sqs.us-east-1.amazonaws.com/537488554861/LocalApp-Manager")
//                        .receiptHandle(m.receiptHandle())
//                        .build();
//                sqsClient.deleteMessage(deleteRequest);
            }
        }
        if(json!=null && ((String)json.get("task")).equals("download pdf")){
            String bucket= (String) json.get("bucketName");
            String key= (String) json.get("key");
            int number=(int)json.get("number");
            downloadPDFListFromS3(s3,bucket,key,region);

        }

    }

    public static List<Message> receiveMessages(SqsClient sqsClient, String queueUrl) {

        System.out.println("\nReceive messages");

        try {
            ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .build();
            List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();
            return messages;
        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return null;
    }

    public static void downloadPDFListFromS3(S3Client s3, String bucket, String key, Region region) throws IOException {
        try {
            System.out.println("Downloading pdf source file to S3...");
            GetObjectRequest objectRequest = GetObjectRequest
                    .builder()
                    .key(key)
                    .bucket(bucket)
                    .build();

            ResponseInputStream<GetObjectResponse> s3objectResponse=s3.getObject(objectRequest);
            BufferedReader reader = new BufferedReader(new InputStreamReader(s3objectResponse));

            List<Message> msgs=new LinkedList<Message>() ;

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            reader.close();
            s3objectResponse.close();


            System.out.println("Upload complete");
            System.out.printf("%n");
            System.out.println("Closing the connection to {S3}");
            s3.close();
            System.out.println("Connection closed");

        }catch (IOException ex) {
            ex.printStackTrace();
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }

    }
}

