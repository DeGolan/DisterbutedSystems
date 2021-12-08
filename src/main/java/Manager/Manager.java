package Manager;

import Tools.MessageProtocol;
import Tools.SQSHelper;
import org.json.JSONObject;
import software.amazon.awssdk.services.sqs.model.*;

import java.io.*;
import java.util.List;


public class Manager {
    public static void main(String[] args) throws IOException {
        System.out.println("Manager is starting...");

        boolean isFinished = false;
        ManagerHelper managerHelper = new ManagerHelper("dsps12bucket");
        SQSHelper localManager = new SQSHelper("https://sqs.us-east-1.amazonaws.com/537488554861/LocalApp-Manager");

        String instanceId="";
        while (!isFinished) {
            List<Message> msgs = localManager.getMessages();
            for (Message msg : msgs) {
                MessageProtocol receivedMsg = new MessageProtocol(new JSONObject(msg.body()));
                String task = receivedMsg.getTask();
                if (task.equals("Terminate")) {
                    localManager.deleteMessage(msg);
                    System.out.println("Manager received Terminate msg");
                    managerHelper.terminate();
                    isFinished = true;
                    instanceId = receivedMsg.getStatus(); //The Manager ID in order to terminate him
                } else if (task.equals("Download PDF")) {
                    System.out.println("Received Download PDF msg \nstarting to distribute Work...");
                    managerHelper.distributeWork(receivedMsg);
                    localManager.deleteMessage(msg);
                }else{ //A message that meant for one of the local apps, so we release it by changing the visibility time out to 0
                    localManager.releaseMessage(msg);
                }
            }
        }
        System.out.println("Terminating my self...");
        localManager.close();
        managerHelper.terminateInstance(instanceId);
    }
}