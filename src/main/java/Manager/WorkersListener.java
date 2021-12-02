package Manager;

import Tools.MessageProtocol;
import Tools.SQSHelper;
import org.json.JSONObject;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkersListener implements Runnable{
    private SQSHelper sqsHelper;
    private CopyOnWriteArrayList<String> summaryFile;
    private AtomicInteger numOfResponses;
    private AtomicInteger numOfTasks;
    private AtomicBoolean terminateAll;
    private AtomicBoolean sendSummary;
    private ConcurrentHashMap<String,String> summaryFiles;


    public WorkersListener(SQSHelper sqsHelper, CopyOnWriteArrayList summaryFile, ConcurrentHashMap summaryFiles, AtomicInteger numOfResponses,
                           AtomicInteger numOfTasks, AtomicBoolean terminateAll, AtomicBoolean sendSummary){
        this.sqsHelper=sqsHelper;
        this.summaryFile=summaryFile;
        this.summaryFiles=summaryFiles;
        this.numOfResponses=numOfResponses;
        this.numOfTasks=numOfTasks;
        this.terminateAll=terminateAll;
        this.sendSummary=sendSummary;
    }

    public void run() {
        System.out.println("Workers Control has started running...");
        boolean isFinished=false;

        while (!isFinished){
            List<Message> receivedMessages=sqsHelper.getMessages();
            for(Message message :receivedMessages){
                MessageProtocol msg=new MessageProtocol(new JSONObject(message.body()));
                System.out.println("GOT MSG: TASK: "+msg.getTask()+" STATUS: "+msg.getStatus());

                String status=msg.getStatus();
                if(status.equals("complete")){
                    summaryFile.add(msg.getKey());
                    numOfResponses.incrementAndGet();
                } else if (status.equals("error")) {
                    numOfResponses.incrementAndGet();
                }
                sqsHelper.deleteMessage(message);
            }
            System.out.println("numOfTasks: "+numOfTasks.get()+" numOfResponses: "+numOfResponses.get());
            if(numOfTasks.get()==numOfResponses.get()){
                if(sendSummary.compareAndSet(true,false)){
                    ManagerHelper.uploadSummary();
                }
                if(terminateAll.compareAndSet(true,true)){
                    System.out.println("FINISHED LISTEN LOOP");
                    isFinished=true;
                }
            }
        }
    }
}
