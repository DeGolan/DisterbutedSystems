package LocalApplication;

import Tools.MessageProtocol;
import Tools.S3Helper;
import Tools.SQSHelper;
import org.json.JSONObject;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;


public class LocalApplication {
    public static void main(String[] args) {

        String url="https://sqs.us-east-1.amazonaws.com/537488554861/LocalApp-Manager";
        String bucket = "dsps12bucket";
        String key = "pdf_src";
        String inputFileName=""; //"/home/vagrant/DistributedSystems/src/main/resources/input-sample-1.txt"
        String outputFileName="";
        String managerJarPath="/home/vagrant/DisterbutedSystems/out/artifacts/Assignment1_jar/Assignment1.jar";
        int numOfPDFPerWorker = 1;
        boolean shouldTerminate = false;
        boolean gotResult = false;

        if(args.length == 3 || args.length == 4) {
            inputFileName="./src/main/resources/input-sample-1.txt";//args[0];
            outputFileName=args[1];
            numOfPDFPerWorker=Integer.parseInt(args[2]);
            if (args.length == 4) {
                if (args[3].equals("terminate"))
                    shouldTerminate = true;
                else {
                    System.err.println("Invalid command line argument: " + args[3]);
                    System.exit(1);
                }
            }
        }
        else {
            System.err.println("Invalid number of command line arguments");
            System.exit(1);
        }

        System.out.println("Local Application is running...");

        //Check if manager exists and if not start him
        MangerHelper mangerHelper = new MangerHelper();
        //mangerHelper.startManager();

        //upload files to S3
        S3Helper s3Helper=new S3Helper();
        System.out.println("upload pdf source file to S3");
        s3Helper.uploadFileToS3(inputFileName, bucket, key);
        System.out.println("upload manager jar file to S3");
        //s3Helper.uploadFileToS3(managerJarPath, bucket, "ManagerJar");//TODO upload if need only

        //Send Message to sqs
        System.out.println("Send file to sqs");
        SQSHelper sqsHelper = new SQSHelper(url);
        MessageProtocol uploadSrc = new MessageProtocol("Download PDF", bucket, key, numOfPDFPerWorker,"","");
        sqsHelper.sendMessageToSQS(uploadSrc);

        //Check SQS queue for a finish message
        System.out.println("Check SQS queue for a finish message");
        while(!gotResult)
        {
            List<Message> messages = sqsHelper.getMessages();
            for(Message msg : messages){
                MessageProtocol receivedMsg = new MessageProtocol(new JSONObject(msg.body()));
                if(receivedMsg.getTask().equals("finished")){
                    System.out.println("The manager finished his work");
                    s3Helper.downloadFile(outputFileName, receivedMsg.getBucketName() , receivedMsg.getKey()); //TODO downloadFile
                    if(shouldTerminate){
                        System.out.println("Should terminate");//TODO also delete manager instance
                        MessageProtocol terminateMsg = new MessageProtocol("Terminate", bucket,"",0,"","");
                        sqsHelper.sendMessageToSQS(terminateMsg);
                    }
                    gotResult = true;
                }
            }
        }
    }
}
