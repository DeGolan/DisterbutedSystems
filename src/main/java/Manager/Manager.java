package Manager;

import com.google.gson.Gson;
import org.json.JSONObject;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
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
                System.out.println(json.get("task"));
                DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                        .queueUrl("https://sqs.us-east-1.amazonaws.com/537488554861/LocalApp-Manager")
                        .receiptHandle(m.receiptHandle())
                        .build();
                sqsClient.deleteMessage(deleteRequest);
            }
        }
        if(json!=null && ((String)json.get("task")).equals("download pdf")){
            String bucket= (String) json.get("bucketName");
            String key= (String) json.get("key");
            //int number=(int)json.get("number");
            downloadPDFListFromS3(s3,bucket,key,region);

        }

        //create one worker for now
        Ec2Client ec2 = Ec2Client.builder()
                .region(region)
                .build();
        System.out.println("Creating 1 worker for now");
      String workerID=createWorker(ec2,"ami-01cc34ab2709337aa");


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


            String line;
            SqsClient sqsClient = SqsClient.builder()
                    .region(region)
                    .build();
            while ((line = reader.readLine()) != null) {
                JSONObject json=new JSONObject();
                String task=line.substring(0,line.indexOf('\t'));
                String url=line.substring(line.indexOf('\t')+1);
                json.put("task",task);
                json.put("url",url);
                System.out.println("sending msg to sqs");
                sendMessageToSQS(sqsClient,json.toString());
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

    public static void sendMessageToSQS (SqsClient sqsClient, String msg){
        try {
            SendMessageRequest send_msg_request = SendMessageRequest.builder()
                    .queueUrl("https://sqs.us-east-1.amazonaws.com/537488554861/Manager-Workers")
                    .messageBody(msg)
                    //  .delaySeconds(5)
                    .build();
            sqsClient.sendMessage(send_msg_request);
        } catch (QueueNameExistsException e) {
            throw e;
        }

    }

    //need to think about security, need to add IAM Role
    public static String createWorker(Ec2Client ec2,String amiId){
        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .imageId(amiId)
                .instanceType(InstanceType.T2_MICRO)
                .maxCount(1)
                .minCount(1)
                .build();

        RunInstancesResponse response = ec2.runInstances(runRequest);
        String instanceId = response.instances().get(0).instanceId();

        Tag tag = Tag.builder()
                .key("Worker")
                .value("")
                .build();

        CreateTagsRequest tagRequest = CreateTagsRequest.builder()
                .resources(instanceId)
                .tags(tag)
                .build();

        try {
            ec2.createTags(tagRequest);
            System.out.printf(
                    "Successfully started EC2 Instance %s based on AMI %s",
                    instanceId, amiId);

            return instanceId;

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }

        return "";

    }

    public static void startWorkers(Ec2Client ec2,int numToStart) {
        if(numToStart>10){
            System.out.println("YOU CAN'T CREATE 19 INSTANCES");
            return;
        }
        try {
            String nextToken = null;
            do {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder().build();
                DescribeInstancesResponse response = ec2.describeInstances(request);

                for (Reservation reservation : response.reservations()) {
                    for (Instance instance : reservation.instances()) {
                        List <Tag> tags=instance.tags();
                        if(tags.size()>0 && numToStart>0){
                            if(tags.get(0).key().equals("Worker")){
                                if(!instance.state().name().toString().equals("running")){
                                    String instanceId=instance.instanceId();
                                    StartInstancesRequest request2 = StartInstancesRequest.builder()
                                            .instanceIds(instanceId)
                                            .build();
                                    ec2.startInstances(request2);
                                    numToStart--;
                                    System.out.printf("Successfully started Worker");
                                }else {
                                    System.out.printf("Worker is already running");
                                    numToStart--;
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
}

