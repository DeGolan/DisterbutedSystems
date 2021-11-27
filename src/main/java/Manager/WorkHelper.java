package Manager;

import Tools.MessageProtocol;
import Tools.S3Helper;
import Tools.SQSHelper;
import org.json.JSONObject;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkHelper {
    private Ec2Client ec2Client;
    private SQSHelper managerWorkersSQS;
    private SQSHelper workersMangerSQS;
    private Thread receiveMsgs;
    private AtomicInteger numOfWorkers;
    private String amiId="ami-01cc34ab2709337aa";
    private  AtomicBoolean terminateAll;
    CopyOnWriteArrayList<String> summaryFile;

    public WorkHelper(AtomicBoolean terminateAll){
        ec2Client= Ec2Client.builder()
                .region(Region.US_EAST_1)
                .build();

        managerWorkersSQS=new SQSHelper("https://sqs.us-east-1.amazonaws.com/537488554861/LocalApp-Manager");//TODO check sqs url
        workersMangerSQS=new SQSHelper("");//TODO enter sqs url

        numOfWorkers=new AtomicInteger(0);
        summaryFile=new CopyOnWriteArrayList<>();

        receiveMsgs=new Thread(new WorkersControl(workersMangerSQS,summaryFile,numOfWorkers));//can init more than 1 if needed
        receiveMsgs.start();

        this.terminateAll=terminateAll;
    }

    public void terminate(){
        try {
            receiveMsgs.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void distributeWork(MessageProtocol receivedMessage) throws IOException {
        S3Helper s3Helper=new S3Helper();
        String bucket=receivedMessage.getBucketName();
        String key=receivedMessage.getKey();
        int numOfPDFPerWorker=receivedMessage.getNumOfPDFPerWorker();

         List<MessageProtocol> msgs= s3Helper.downloadPDFList(key,bucket);
         int numOfMsgs=msgs.size();

         //TODO make sure there is no more then 19 workers!
         int numOfWantedWorkers = numOfMsgs/numOfPDFPerWorker;
         if(numOfWorkers.get()<numOfWantedWorkers){
             int numOfWorkersToAdd=numOfWorkers.get()-numOfWantedWorkers;
            for(int i=0;i<numOfWorkersToAdd;i++){
                createWorker();
                numOfWorkers.incrementAndGet();
            }
         }
         for (MessageProtocol msg:msgs){
             managerWorkersSQS.sendMessageToSQS(msg);
         }
     }

    private void createWorker(){
        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .imageId(amiId)
                .instanceType(InstanceType.T2_MICRO)
                .maxCount(1)
                .minCount(1)
                .build();

        RunInstancesResponse response = ec2Client.runInstances(runRequest);
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
            ec2Client.createTags(tagRequest);
            System.out.printf(
                    "Successfully started EC2 Instance %s based on AMI %s",
                    instanceId, amiId);

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }

    }


}
