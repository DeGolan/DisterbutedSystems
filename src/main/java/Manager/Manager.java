package Manager;

import Tools.MessageProtocol;
import Tools.SQSHelper;
import org.json.JSONObject;
import software.amazon.awssdk.services.sqs.model.*;

import java.io.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class Manager {
    public static void main(String[] args) throws IOException {
        System.out.println("Manager is starting...");

        boolean isFinished = false;
        AtomicBoolean terminateAll=new AtomicBoolean(false);
        ManagerHelper workHelper = new ManagerHelper(terminateAll,"dsps12bucket");//TODO create bucket
        SQSHelper localManager = new SQSHelper("https://sqs.us-east-1.amazonaws.com/537488554861/LocalApp-Manager");


        System.out.println("Starting work loop...");
        String instanceId="";
        while (!isFinished) {
            List<Message> msgs = localManager.getMessages();
            for (Message msg : msgs) {
                MessageProtocol receivedMsg = new MessageProtocol(new JSONObject(msg.body()));
                String task = receivedMsg.getTask();
                if (task.equals("Terminate")) {
                    System.out.println("Received Terminate msg");
                    terminateAll.compareAndSet(false,true);
                    workHelper.terminate();
                    isFinished = true;
                    instanceId = receivedMsg.getStatus();
                    localManager.deleteMessage(msg);
                } else if (task.equals("Download PDF")) {
                    System.out.println("Received Download PDF msg: starting to distribute Work...");
                    workHelper.distributeWork(receivedMsg);
                    localManager.deleteMessage(msg);
                }
            }
        }
        workHelper.terminateInstance(instanceId);
    }
}