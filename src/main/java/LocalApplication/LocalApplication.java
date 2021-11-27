package LocalApplication;


import java.nio.file.Paths;


import Tools.MessageProtocol;
import Tools.S3Helper;
import Tools.SQSHelper;
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
//        final String uniqueLocalId = "id1";
//        final String uniquePathLocalApp =  awsBundle.inputFolder+"/"+ uniqueLocalId;
        String inputFileName="";
        String outputFileName="";
        int numOfPDFPerWorker = 1;
        boolean shouldTerminate = false;
        boolean gotResult = false;
        String url="https://sqs.us-east-1.amazonaws.com/537488554861/LocalApp-Manager";
        String bucket = "dsps12bucket";
        String key = "pdf_src";
//        boolean gotResult = false;
        if(args.length == 3 || args.length == 4) {
            inputFileName=args[0];
            outputFileName=args[1];
            numOfPDFPerWorker=Integer.parseInt(args[2]);
            if (args.length == 4) {
                if (args[3].equals("terminate"))
                    shouldTerminate = true;
                else {
                    System.err.println("Invalid command line argument: " + args[4]);
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
        mangerHelper.startManager();

        //upload pdf source file to S3
        System.out.println("upload pdf source file to S3");
        S3Helper s3Helper=new S3Helper();
        s3Helper.uploadFileToS3(inputFileName, bucket, key);

        //Send Message to sqs
        System.out.println("Send file to sqs");
        SQSHelper sqsHelper = new SQSHelper(url);
        MessageProtocol uploadSrc = new MessageProtocol("Download PDF", bucket, key, numOfPDFPerWorker);
        sqsHelper.sendMessageToSQS(uploadSrc);

        //Check SQS queue for a finish message
        System.out.println("Check SQS queue for a finish message");
        while(!gotResult)
        {
            List<Message> messages = sqsHelper.getMessages(url);
            for(Message msg : messages){
                MessageProtocol recievedMsg = new MessageProtocol(new JSONObject(msg.body()));
                if(recievedMsg.getTask().equals("Finished")){
                    System.out.println("The manager finished his work");
                    s3Helper.downloadFile(outputFileName, recievedMsg.getBucketName() , recievedMsg.getKey()); //TODO downloadFile
                    if(shouldTerminate){
                        System.out.println("Should terminate");
                        MessageProtocol terminateMsg = new MessageProtocol("Terminate", "","",0);
                        sqsHelper.sendMessageToSQS(terminateMsg);
                    }
                    gotResult = true;
                }
            }
        }
//        startManager(mangerHelper);
//        ec2.close();






    }



}
