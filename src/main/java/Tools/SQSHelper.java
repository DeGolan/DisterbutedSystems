package Tools;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

public class SQSHelper {
    private final SqsClient sqsClient;
    String url;
    public SQSHelper(String url){
        sqsClient = SqsClient.builder()
                .region(Region.US_EAST_1)
                .build();
        this.url=url;
    }
public void releaseMessage (Message m){
        sqsClient.changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
                .queueUrl(url)
                .receiptHandle(m.receiptHandle())
                .visibilityTimeout(0)
                .build());
}
    public void sendMessageToSQS (MessageProtocol msg){
        try {
            SendMessageRequest send_msg_request = SendMessageRequest.builder()
                    .queueUrl(url)
                    .messageBody(msg.getJson().toString())
                    .build();
            sqsClient.sendMessage(send_msg_request);
        } catch (QueueNameExistsException e) {
            throw e;
        }
    }

    public List<Message> getMessages(){
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(url)
                .visibilityTimeout(60)
                .build();
        return sqsClient.receiveMessage(receiveRequest).messages();
    }

    public  void deleteMessage(Message m){
        DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                .queueUrl(url)
                .receiptHandle(m.receiptHandle())
                .build();
        sqsClient.deleteMessage(deleteRequest);
    }

    public void close(){
        sqsClient.close();
    }
}
