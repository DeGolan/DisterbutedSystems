package Manager;

import Tools.MessageProtocol;
import Tools.S3Helper;
import Tools.SQSHelper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ManagerHelper {

    private Ec2Client ec2Client;
    private SQSHelper managerWorkersSQS;
    private SQSHelper workersMangerSQS;
    private static SQSHelper localManagerSQS;
    private List<Thread> workerListeners;
    private AtomicInteger numOfWorkers;
    private String amiId="ami-00e95a9222311e8ed";
    private List<String> instancesId;
    private static String bucket;
    String script = "#!/bin/bash\n"+
            "mkdir WorkerFiles\n"+
            "aws s3 cp s3://dsps12bucket/WorkerJar ./WorkerFiles/Worker.jar\n"+
            "java -jar /WorkerFiles/Worker.jar\n";

    public ManagerHelper(String bucket){
        ec2Client= Ec2Client.builder()
                .region(Region.US_EAST_1)
                .build();
        this.bucket=bucket;
        managerWorkersSQS=new SQSHelper("https://sqs.us-east-1.amazonaws.com/537488554861/Manager-Workers");
        workersMangerSQS=new SQSHelper("https://sqs.us-east-1.amazonaws.com/537488554861/Workers-Manager");
        localManagerSQS=new  SQSHelper("https://sqs.us-east-1.amazonaws.com/537488554861/LocalApp-Manager");
        instancesId=new LinkedList<>();
        numOfWorkers=new AtomicInteger(0);
        workerListeners=new LinkedList<>();

    }

    public void startNewJob(int numOfTasks,String localAppId){
        System.out.println("Starting new job of "+localAppId+"...");
        Thread thread=new Thread(new WorkersListener(workersMangerSQS,numOfTasks,localAppId));//can init more than 1 if needed
        workerListeners.add(thread);
        thread.start();
    }
    public void terminate(){
        System.out.println("In terminate");
        int count=1;
        try {
            for(Thread thread : workerListeners){
                System.out.println("Join thread number "+count++);
                thread.join();
            }
            System.out.println("All threads are done");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Starting terminate all instances");
        for(String id:instancesId){
            terminateInstance(id);
            System.out.println("instance "+id+"has been terminated");
        }
        System.out.println("All instances have been terminated");
        managerWorkersSQS.close();
        workersMangerSQS.close();
        localManagerSQS.close();

    }
    public static void uploadSummary(String localAppId,List<String> summaryFile){
        S3Helper s3Helper=new S3Helper();
        try {
            String path="/ManagerFiles/"+localAppId+"/"+summaryFile+".txt";
            FileWriter file=new FileWriter(path);
            for(String str:summaryFile){
                file.write(str+System.lineSeparator());
            }
            file.close();
            s3Helper.uploadFileToS3(path,bucket,path);
            MessageProtocol finishMsg=new MessageProtocol("finished",bucket,path,0,"","",localAppId);
            localManagerSQS.sendMessageToSQS(finishMsg);
        } catch (IOException e) {
            e.printStackTrace();
        }
        s3Helper.closeS3();
    }
    public void terminateInstance(String instanceId){
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
        String localAppId=receivedMessage.getLocalApp();
        System.out.println("Starting distributeWork with LocalAppId: "+localAppId);
        int numOfPDFPerWorker=receivedMessage.getNumOfPDFPerWorker();

         List<MessageProtocol> msgs= s3Helper.downloadPDFList(key,bucket,localAppId);
         int numOfTasks=msgs.size();


         //TODO make sure there is no more then 19 workers!
         int numOfWantedWorkers = numOfTasks/numOfPDFPerWorker;
         if(numOfWantedWorkers==0){
             numOfWantedWorkers=1;
         }

         if(numOfWorkers.get()<numOfWantedWorkers){
             int numOfWorkersToAdd=numOfWantedWorkers-numOfWorkers.get();
            for(int i=0;i<numOfWorkersToAdd;i++){
                if(numOfWorkers.get()>12){
                    break;
                }
                instancesId.add(createWorker());
                numOfWorkers.incrementAndGet();
            }
         }
         for (MessageProtocol msg:msgs){
             managerWorkersSQS.sendMessageToSQS(msg);
         }
        System.out.println("Starting new job with LocalAppId: "+localAppId);
         startNewJob(numOfTasks,localAppId);
         s3Helper.closeS3();
     }

    private String createWorker(){
        IamInstanceProfileSpecification role = IamInstanceProfileSpecification.builder().name("LabInstanceProfile").build();
        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .imageId(amiId)
                .userData(Base64.getEncoder().encodeToString(script.getBytes()))
                .iamInstanceProfile(role)
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
