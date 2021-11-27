package Manager;

import Tools.MessageProtocol;
import Tools.SQSHelper;
import org.json.JSONObject;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkersControl implements Runnable{
    private SQSHelper sqsHelper;
    CopyOnWriteArrayList<String> summaryFile;
    private AtomicInteger numOfWorkers;

    public  WorkersControl(SQSHelper sqsHelper,CopyOnWriteArrayList summaryFile,AtomicInteger numOfWorkers){
        this.sqsHelper=sqsHelper;
        this.summaryFile=summaryFile;
    }

    public void run() {
        boolean isFinished=false;
        while (!isFinished){
            List<Message> receivedMessages=sqsHelper.getMessages();
            for(Message message :receivedMessages){

                MessageProtocol msg=new MessageProtocol(new JSONObject(message.body()));
                String status=msg.getStatus();

                if(status.equals("complete")){
                    summaryFile.add(msg.getKey());
                }
                else if (status.equals("dead")){
                    isFinished = numOfWorkers.decrementAndGet() == 0;
                }
                sqsHelper.deleteMessage(message);
            }
        }
    }
}
