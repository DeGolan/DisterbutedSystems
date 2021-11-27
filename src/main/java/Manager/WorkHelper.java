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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkHelper {
    private Ec2Client ec2Client;
    private SQSHelper managerWorkersSQS;
    private SQSHelper workersMangerSQS;
    private static SQSHelper localManagerSQS;
    private Thread receiveMsgs;
    private AtomicInteger numOfWorkers;
    private String amiId="ami-01cc34ab2709337aa";
    private  AtomicBoolean terminateAll;
    private static CopyOnWriteArrayList<String> summaryFile;
    private List<String> instancesId;
    private AtomicInteger numOfResponses;
    private  AtomicInteger numOfTasks;
    private  AtomicBoolean sendSummary;
    private static String bucket;

    public WorkHelper(AtomicBoolean terminateAll,String bucket){
        ec2Client= Ec2Client.builder()
                .region(Region.US_EAST_1)
                .build();
        this.bucket=bucket;
        managerWorkersSQS=new SQSHelper("https://sqs.us-east-1.amazonaws.com/537488554861/LocalApp-Manager");//TODO check sqs url
        workersMangerSQS=new SQSHelper("");//TODO enter sqs url
        localManagerSQS=new  SQSHelper("");
        instancesId=new LinkedList<>();

        numOfWorkers=new AtomicInteger(0);
        numOfResponses=new AtomicInteger(0);
        numOfTasks=new AtomicInteger(-1);
        sendSummary.set(true);
        summaryFile=new CopyOnWriteArrayList<>();

        this.terminateAll=terminateAll;

        receiveMsgs=new Thread(new WorkersControl(workersMangerSQS,summaryFile,numOfResponses,numOfTasks,terminateAll,sendSummary));//can init more than 1 if needed
        receiveMsgs.start();

    }

    public void terminate(){
        try {
            receiveMsgs.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //TODO delete Qs
        for(String id:instancesId){
            terminateWorker(id);
        }

    }
    public static void uploadSummary(){
        S3Helper s3Helper=new S3Helper();

        try {
            String path="./src/main/resources/text/summaryFile.txt";
            FileWriter file=new FileWriter(path);
            for(String str:summaryFile){
                file.write(str+System.lineSeparator());
            }
            file.close();
            s3Helper.uploadFileToS3(path,bucket,"summaryFile");
            MessageProtocol finishMsg=new MessageProtocol("finished",bucket,"summaryFile",0,"","");
            localManagerSQS.sendMessageToSQS(finishMsg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void terminateWorker(String instanceId){
        try {
            TerminateInstancesRequest request = TerminateInstancesRequest.builder()
                    .instanceIds(instanceId).build();
            ec2Client.terminateInstances(request);
        }
        catch (Exception e){
        }
    }
    public void distributeWork(MessageProtocol receivedMessage) throws IOException {
        S3Helper s3Helper=new S3Helper();
        String bucket=receivedMessage.getBucketName();
        String key=receivedMessage.getKey();
        int numOfPDFPerWorker=receivedMessage.getNumOfPDFPerWorker();

         List<MessageProtocol> msgs= s3Helper.downloadPDFList(key,bucket);
         int numOfMsgs=msgs.size();
         numOfTasks.set(numOfMsgs);

         //TODO make sure there is no more then 19 workers!
         int numOfWantedWorkers = numOfMsgs/numOfPDFPerWorker;
         if(numOfWorkers.get()<numOfWantedWorkers){
             int numOfWorkersToAdd=numOfWorkers.get()-numOfWantedWorkers;
            for(int i=0;i<numOfWorkersToAdd;i++){
                instancesId.add(createWorker());
                numOfWorkers.incrementAndGet();
            }
         }
         for (MessageProtocol msg:msgs){
             managerWorkersSQS.sendMessageToSQS(msg);
         }
     }

    private String createWorker(){
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
        return  instanceId;

    }


}
