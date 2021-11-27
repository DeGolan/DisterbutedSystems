package Tools;

import org.json.JSONObject;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

public class S3Helper {
    private S3Client s3;
    public S3Helper(){
        s3 = S3Client.builder().region(Region.US_EAST_1).build();
    }

    //uploading the pdf_src to the s3
    public void uploadFileToS3(String filePath, String bucket, String key){
        System.out.println("Uploading file path " + filePath + "to S3...");
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(),
                RequestBody.fromFile(Paths.get(filePath)));
        System.out.println("Upload complete");
    }

    public void downloadFile(String outputFileName, String bucket, String key){
       //TBD
    }
    public void closeS3(){
        System.out.printf("%n");
        System.out.println("Closing the connection to {S3}");
        s3.close();
        System.out.println("Connection closed");
    }
}

//"/home/vagrant/DistributedSystems/src/main/resources/input-sample-1.txt"