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

        //Send Message to sqs
        System.out.println("Send file to sqs");
        SQSHelper sqsHelper = new SQSHelper(url);
        MessageProtocol uploadSrc = new MessageProtocol("Download PDF", bucket, key, numOfPDFPerWorker,"","",localAppId);
        sqsHelper.sendMessageToSQS(uploadSrc);

        //start manager
        System.out.println("Starting Manager");
        String managerInstanceId = mangerHelper.startManager();

        //Check SQS queue for a finish message
        System.out.println("\nCheck SQS queue for a finish message");
        while(!gotResult)
        {
            List<Message> messages = sqsHelper.getMessages();
            for(Message msg : messages){
                MessageProtocol receivedMsg = new MessageProtocol(new JSONObject(msg.body()));
//                System.out.println("LocalApp got msg with task: "+receivedMsg.getTask());
                if(receivedMsg.getLocalApp().equals(localAppId)){ //Verify that this message belongs to this local app
                    if(receivedMsg.getTask().equals("finished")){
                        System.out.println("The manager finished his work");
                        s3Helper.downloadFile(outputFileName,receivedMsg.getBucketName() ,receivedMsg.getKey());
                        if(shouldTerminate){
                            System.out.println("Should terminate");
                            MessageProtocol terminateMsg = new MessageProtocol("Terminate", bucket,"",0,"",managerInstanceId,localAppId);
                            sqsHelper.sendMessageToSQS(terminateMsg);
                        }
                        gotResult = true;
                        sqsHelper.deleteMessage(msg);
                    }else{ //Not a finished message so it's supposed to arrive to the manager, so we release it for him by changing the visibility time out to 0
                        sqsHelper.releaseMessage(msg);
//                        System.out.println("LocalApp released msg: "+receivedMsg.getTask());
                        try {
//                            System.out.println("Going to sleep");
                            //To keep the local apps synchronized
                            Thread.sleep(5000);
//                            System.out.println("Good morning");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        sqsHelper.close();
        s3Helper.closeS3();
    }
}
