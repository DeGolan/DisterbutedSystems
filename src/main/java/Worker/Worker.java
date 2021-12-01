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

        SQSHelper workerManager=new SQSHelper("https://sqs.us-east-1.amazonaws.com/537488554861/Workers-Manager");//TODO ENTER URL
        SQSHelper managerWorker=new SQSHelper("https://sqs.us-east-1.amazonaws.com/537488554861/Manager-Workers");
        WorkerHelper workerHelper=new WorkerHelper(workerManager);
        int i=0;
//        boolean isFinished=false;
        while (true){
            List<Message> messages= managerWorker.getMessages();
            for (Message message:messages){
                System.out.println("GOT MSG number "+i++);
                MessageProtocol msg=new MessageProtocol(new JSONObject(message.body()));
                String task=msg.getTask();
                String url=msg.getUrl();
                String bucket=msg.getBucketName();
                System.out.println("Try Convert PDF "+url);
                String path=workerHelper.convertPDF(url,task);
                System.out.println("Path returned "+path);
                if(!path.equals("")){//if path="" so error occurred
                    S3Helper s3Helper=new S3Helper();
                    s3Helper.uploadFileToS3(path,bucket,path);//key to the new object is the local path
                    MessageProtocol completeMessage =new MessageProtocol(task,bucket,path,0,"","complete");
                    System.out.println("Send Complete msg");
                    workerManager.sendMessageToSQS(completeMessage);
                }
                managerWorker.deleteMessage(message);
            }

        }

    }


}
