package LocalApplication;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Base64;
import java.util.List;

public class MangerHelper {
    private Region region;
    private Ec2Client ec2;
    private String amiId="ami-00e95a9222311e8ed";
    private String script="#!/bin/bash\n"+"set-x\n"+"echo hello world";
 /*   String script = "#!/bin/bash"+
            "export AWS_ACCESS_KEY_ID=ASIAX2JGQPNWQ7XGT3Q6"+
            "export AWS_SECRET_ACCESS_KEY=a+kXhchCNu86EiEqO873JkME/JivxHy451PgKqqn" +
            "AWS_SESSION_TOKEN=FwoGZXIvYXdzEB8aDOyX2h9xgCSdAtP68SLFAZoLUgsrFVDqLFhLI+h5UGVkl8fa0qbSqnWgwaUuySXTafI409+TvcTzQAFUDMf6gfIW71xalbDfYB3SsCUXUI5RSW+7/hUCg9sZkH9YXaOfWnwSydZ5k+R+kwzxaQF8mAlHSbuOSeihxeJKrEiloY8BG3EZ0N5burS/XvbY/6oalxQCv9Qjo1iBb32bXWT35jABwL/avOCXZtrPbD12HW6DJwzYIJ9vrjO6ivslvINLa/5xvd+TxYbI4tjSbr7F2Ug0O1IDKK6zk40GMi0shilkeB4WtKS1LaBuqsN7z7cyv9dfBoiEWZdMiPcGkKDIGkGUBPj/SgiiV5E="+
            "export AWS_DEFAULT_REGION=us-east-1"+
            "aws s3 cp s3://<bucket_name>/<file_name>.jar <file_name>.jar"+
            "java -jar <file_name>.jar arg1 arg2 arg3";*/

    public  MangerHelper () {
        region = software.amazon.awssdk.regions.Region.US_EAST_1;
        ec2 = Ec2Client.builder()
                .region(region)
                .build();
    }
    //check if the instance tag is "Manager"
    public void startManager() {
        try {
            boolean startNewManager=true;
            String nextToken = null;
            do {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder().build();
                DescribeInstancesResponse response = ec2.describeInstances(request);

                for (Reservation reservation : response.reservations()) {
                    for (Instance instance : reservation.instances()) {
                        List<Tag> tags=instance.tags();
                        if(tags.size()>0){
                            if(tags.get(0).key().equals("Manager")){
                                if(instance.state().name().toString().equals("stopped")){
                                    String instanceId=instance.instanceId();
                                    StartInstancesRequest request2 = StartInstancesRequest.builder()
                                            .instanceIds(instanceId)
                                            .build();
                                    ec2.startInstances(request2);
                                    System.out.printf("Successfully started Manager");
                                    startNewManager=false;
                                }else if(instance.state().name().toString().equals("running")){
                                    System.out.printf("Manager is already running");
                                    startNewManager=false;
                                }
                            }
                        }
                    }
                }
                nextToken = response.nextToken();
            } while (nextToken != null);
            if(startNewManager){
                System.out.printf("Creating a new Manager");
                createManager();
            }

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }

    }

    private String createManager(){
        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .imageId(amiId)
                .userData(Base64.getEncoder().encodeToString(script.getBytes())
                )
                .instanceType(InstanceType.T2_MICRO)
                .maxCount(1)
                .minCount(1)
                .build();

        RunInstancesResponse response = ec2.runInstances(runRequest);
        String instanceId = response.instances().get(0).instanceId();

        Tag tag = Tag.builder()
                .key("Manager")
                .value("")
                .build();

        CreateTagsRequest tagRequest = CreateTagsRequest.builder()
                .resources(instanceId)
                .tags(tag)
                .build();

        try {
            ec2.createTags(tagRequest);
            System.out.printf(
                    "Successfully started EC2 Instance %s based on AMI %s",
                    instanceId, amiId);


        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return  instanceId;

    }
}
