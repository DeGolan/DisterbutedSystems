package LocalApplication;


import java.nio.file.Paths;


import org.json.JSONObject;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import java.util.List;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.S3Client;


public class LocalApplication {
    public static void main(String[] args) {
        System.out.println("Local Application is running...");

        Region region = Region.US_EAST_1;
        S3Client s3 = S3Client.builder().region(region).build();
        String bucket = "dsps12bucket";
        String key = "pdf_src";
        int numOfMsgsPerWorker=4;
        uploadPDFListToS3(s3, bucket, key ,region);
        SqsClient sqsClient = SqsClient.builder()
                .region(region)
                .build();
//        Ec2Client ec2 = Ec2Client.builder()
//                .region(region)
//                .build();
//        startManager(ec2);
//        ec2.close();


        JSONObject json=new JSONObject();
        json.put("task","download pdf");
        json.put("bucketName",bucket);
        json.put("key",key);
        json.put("number",numOfMsgsPerWorker);
        sendMessageToSQS(sqsClient,json.toString());

        sqsClient.close();
    }

    //check if the instance tag is "Manager"
    public static void startManager(Ec2Client ec2) {
        try {
            String nextToken = null;
            do {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder().build();
                DescribeInstancesResponse response = ec2.describeInstances(request);

                for (Reservation reservation : response.reservations()) {
                    for (Instance instance : reservation.instances()) {
                       List <Tag> tags=instance.tags();
                       if(tags.size()>0){
                           if(tags.get(0).key().equals("Manager")){
                              if(!instance.state().name().toString().equals("running")){
                                  String instanceId=instance.instanceId();
                                  StartInstancesRequest request2 = StartInstancesRequest.builder()
                                        .instanceIds(instanceId)
                                        .build();
                                  ec2.startInstances(request2);
                                  System.out.printf("Successfully started Manager");
                              }else {
                                  System.out.printf("Manager is already running");
                              }
                              return;//only have one manager, can stop iterate
                           }
                       }
                    }
                }
                nextToken = response.nextToken();
            } while (nextToken != null);

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }

    }

    //uploading the pdf_src to the s3
    public static void uploadPDFListToS3(S3Client s3, String bucket, String key, Region region){
        System.out.println("Uploading pdf source file to S3...");
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(),
                RequestBody.fromFile(Paths.get("/home/vagrant/DistributedSystems/src/main/resources/input-sample-1.txt")));
        System.out.println("Upload complete");
        System.out.printf("%n");
        System.out.println("Closing the connection to {S3}");
        s3.close();
        System.out.println("Connection closed");
    }

    public static void sendMessageToSQS (SqsClient sqsClient, String msg){
        try {
            SendMessageRequest send_msg_request = SendMessageRequest.builder()
                    .queueUrl("https://sqs.us-east-1.amazonaws.com/537488554861/LocalApp-Manager")
                    .messageBody(msg)
                  //  .delaySeconds(5)
                    .build();
            sqsClient.sendMessage(send_msg_request);
        } catch (QueueNameExistsException e) {
            throw e;
        }

    }
}
