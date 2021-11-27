package Manager;

import Tools.SQSHelper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

public class WorkHelper {
    private Ec2Client ec2Client;
    private SQSHelper managerWorkersSQS;
    private SQSHelper workersMangerSQS;
    private Thread receiveMsgs;

    public WorkHelper(){
        ec2Client= Ec2Client.builder()
                .region(Region.US_EAST_1)
                .build();

        managerWorkersSQS=new SQSHelper("https://sqs.us-east-1.amazonaws.com/537488554861/LocalApp-Manager");//TODO check sqs url
        workersMangerSQS=new SQSHelper("");//TODO enter sqs url
        receiveMsgs=new Thread();

    }
}
