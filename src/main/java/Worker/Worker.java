package Worker;

import Tools.MessageProtocol;
import Tools.S3Helper;
import Tools.SQSHelper;

import org.json.JSONObject;

import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

public class Worker {
    public static void main(String[] args) {
        System.out.println("Worker is starting...");

        SQSHelper workerManager=new SQSHelper("https://sqs.us-east-1.amazonaws.com/537488554861/Workers-Manager");
        SQSHelper managerWorker=new SQSHelper("https://sqs.us-east-1.amazonaws.com/537488554861/Manager-Workers");
        WorkerHelper workerHelper=new WorkerHelper(workerManager);

//        boolean isFinished=false;
        while (true){
            List<Message> messages= managerWorker.getMessages();
            for (Message message:messages){
                MessageProtocol msg=new MessageProtocol(new JSONObject(message.body()));
                String task=msg.getTask();
                String url=msg.getUrl();
                String bucket=msg.getBucketName();
                String localAppId=msg.getLocalApp();
                System.out.println("GOT MSG of "+localAppId);

                System.out.println("Try Convert PDF "+url);
                String path=workerHelper.convertPDF(url,task,localAppId);

                System.out.println("Path returned "+path);
                if(!path.equals("")){//if path="" so error occurred
                    S3Helper s3Helper=new S3Helper();
                    String resultURL=s3Helper.uploadFileToS3(path,bucket,path);//key to the new object is the local path
                    MessageProtocol completeMessage =new MessageProtocol(task,"",url,0,resultURL,"complete",localAppId);
                    System.out.println("Send Complete msg for localAppId: "+localAppId);
                    workerManager.sendMessageToSQS(completeMessage);
                }
                System.out.println("Delete msg of: "+msg.getLocalApp());
                managerWorker.deleteMessage(message);
            }

        }

    }


}
