package Manager;

import Tools.MessageProtocol;
import Tools.SQSHelper;
import org.json.JSONObject;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.LinkedList;
import java.util.List;

public class WorkersListener implements Runnable{
    private final SQSHelper workersMangerSQS;
    private final List<String> summaryFile;
    private int numOfResponses;
    private final int numOfTasks;
    private final String localAppId;



    public WorkersListener(SQSHelper workersMangerSQS,
                           int numOfTasks,String localAppId){
        this.workersMangerSQS =workersMangerSQS;
        this.summaryFile=new LinkedList<>();
        this.numOfResponses=0;
        this.numOfTasks=numOfTasks;
        this.localAppId=localAppId;
    }

    public void run() {
        System.out.println("Thread: "+localAppId+" has started running...");
        boolean isFinished=false;

        while (!isFinished){
            List<Message> receivedMessages= workersMangerSQS.getMessages();
            for(Message message :receivedMessages){
                MessageProtocol msg=new MessageProtocol(new JSONObject(message.body()));
                if(msg.getLocalApp().equals(localAppId)){
                    System.out.println("Thread: "+localAppId+" GOT MSG: TASK: "+msg.getTask()+" STATUS: "+msg.getStatus());
                    String status=msg.getStatus();
                    if(status.equals("complete")){
                        String task=msg.getTask();
                        String outputURL= msg.getUrl();
                        String oldURL=msg.getKey();
                        summaryFile.add(task+"\t"+oldURL+"\t"+outputURL);
                        numOfResponses++;
                    } else if (status.equals("error")) {
                        String task=msg.getTask();
                        String error= msg.getKey();
                        String oldURL=msg.getUrl();
                        summaryFile.add(task+"\t"+oldURL+"\t"+error);
                        numOfResponses++;
                    }
                    workersMangerSQS.deleteMessage(message);
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
