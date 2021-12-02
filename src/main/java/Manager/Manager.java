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
        ManagerHelper workHelper = new ManagerHelper("dsps12bucket");//TODO create bucket
        SQSHelper localManager = new SQSHelper("https://sqs.us-east-1.amazonaws.com/537488554861/LocalApp-Manager");


        System.out.println("Starting work loop...");
        String instanceId="";
        while (!isFinished) {
            List<Message> msgs = localManager.getMessages();
            for (Message msg : msgs) {
                MessageProtocol receivedMsg = new MessageProtocol(new JSONObject(msg.body()));
                String task = receivedMsg.getTask();
                System.out.println("Manager got msg with task:"+task);
                if (task.equals("Terminate")) {
                    localManager.deleteMessage(msg);
                    System.out.println("Manager received Terminate msg");
                    workHelper.terminate();
                    isFinished = true;
                    instanceId = receivedMsg.getStatus();
                    System.out.println("Deleting Terminate msg");

                } else if (task.equals("Download PDF")) {
                    System.out.println("Received Download PDF msg \nstarting to distribute Work...");
                    workHelper.distributeWork(receivedMsg);
                    localManager.deleteMessage(msg);
                }else{
                    localManager.releaseMessage(msg);
                }
            }
        }
        System.out.println("Terminating my self...");
        localManager.close();
        workHelper.terminateInstance(instanceId);
    }
}