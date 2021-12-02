package Manager;

import Tools.MessageProtocol;
import Tools.SQSHelper;
import org.json.JSONObject;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkersListener implements Runnable{
    private SQSHelper sqsHelper;
    private List<String> summaryFile;
    private int numOfResponses;
    private int numOfTasks;
    private String localAppId;



    public WorkersListener(SQSHelper sqsHelper,
                           int numOfTasks,String localAppId){
        this.sqsHelper=sqsHelper;
        this.summaryFile=new LinkedList<>();
        this.numOfResponses=0;
        this.numOfTasks=numOfTasks;
        this.localAppId=localAppId;
    }

    public void run() {
        System.out.println("Thread: "+localAppId+" has started running...");
        boolean isFinished=false;

        while (!isFinished){
            List<Message> receivedMessages=sqsHelper.getMessages();
            for(Message message :receivedMessages){
                MessageProtocol msg=new MessageProtocol(new JSONObject(message.body()));
                if(msg.getLocalApp().equals(localAppId)){
                    System.out.println("Thread: "+localAppId+" GOT MSG: TASK: "+msg.getTask()+" STATUS: "+msg.getStatus());
                    String status=msg.getStatus();
                    if(status.equals("complete")){
                        summaryFile.add(msg.getKey());
                        numOfResponses++;
                    } else if (status.equals("error")) {
                        numOfResponses++;
                    }
                    sqsHelper.deleteMessage(message);
                    System.out.println("Thread: "+localAppId+" numOfTasks: "+numOfTasks+" numOfResponses: "+numOfResponses);
                }
            }
            if(numOfTasks==numOfResponses){
                System.out.println("Thread: "+localAppId+" Finished work, calling upload summary and shutdown");
                ManagerHelper.uploadSummary(localAppId,summaryFile);
                isFinished=true;
            }
        }
    }
}
