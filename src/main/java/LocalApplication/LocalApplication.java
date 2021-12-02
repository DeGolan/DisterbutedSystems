package LocalApplication;

import Tools.MessageProtocol;
import Tools.S3Helper;
import Tools.SQSHelper;
import org.json.JSONObject;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.Date;
import java.util.List;


public class LocalApplication {
    public static void main(String[] args) {
        String localAppId= String.valueOf((new Date()).getTime());
        String url="https://sqs.us-east-1.amazonaws.com/537488554861/LocalApp-Manager";
        String bucket = "dsps12bucket";
        String key = "pdf_src"+localAppId;
        String inputFileName="";
        String outputFileName="";
        String managerJarPath="/home/vagrant/IdeaProjects/DisterbutedSystems/out/artifacts/Manager_jar/Manager.jar";
        String workerJarPath="/home/vagrant/IdeaProjects/DisterbutedSystems/out/artifacts/Worker_jar/Worker.jar";
        int numOfPDFPerWorker = 1;
        boolean shouldTerminate = false;
        boolean gotResult = false;

        if(args.length == 3 || args.length == 4) {
            inputFileName=args[0];
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
        LocalHelper mangerHelper = new LocalHelper();

        //upload files to S3
        S3Helper s3Helper=new S3Helper();
        System.out.println("upload pdf source file to S3");
        s3Helper.uploadFileToS3(inputFileName, bucket, key);
        System.out.println("upload manager jar file to S3");
        s3Helper.uploadFileToS3(managerJarPath, bucket, "ManagerJar");//TODO upload if need only
        System.out.println("upload worker jar file to S3");
        s3Helper.uploadFileToS3(workerJarPath, bucket, "WorkerJar");//TODO upload if need only

        //Send Message to sqs
        System.out.println("Send file to sqs");
        SQSHelper sqsHelper = new SQSHelper(url);
        MessageProtocol uploadSrc = new MessageProtocol("Download PDF", bucket, key, numOfPDFPerWorker,"","",localAppId);
        sqsHelper.sendMessageToSQS(uploadSrc);

        //start manager
        System.out.println("Starting Manager");
        String managerInstanceId = mangerHelper.startManager();

        //Check SQS queue for a finish message
        System.out.println("Check SQS queue for a finish message");
        while(!gotResult)
        {
            List<Message> messages = sqsHelper.getMessages();
            for(Message msg : messages){
                MessageProtocol receivedMsg = new MessageProtocol(new JSONObject(msg.body()));
                if(receivedMsg.getLocalApp().equals(localAppId)){
                    if(receivedMsg.getTask().equals("finished")){
                        System.out.println("The manager finished his work");
                        s3Helper.downloadFile(outputFileName,receivedMsg.getBucketName() ,receivedMsg.getKey());
                        if(shouldTerminate){ //TODO handle terminate
                            System.out.println("Should terminate");
                            MessageProtocol terminateMsg = new MessageProtocol("Terminate", bucket,"",0,"",managerInstanceId,localAppId);
                            sqsHelper.sendMessageToSQS(terminateMsg);
                        }
                        gotResult = true;
                        sqsHelper.deleteMessage(msg);
                    }
                }
            }
        }
    }
}
