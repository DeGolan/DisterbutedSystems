package LocalApplication;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.io.*;
import java.util.Base64;
import java.util.List;

public class LocalHelper {
    private Region region;
    private Ec2Client ec2;
    private String amiId="ami-00e95a9222311e8ed";
    String script = "#!/bin/bash\n"+
            "mkdir ManagerFiles\n" +
            "aws s3 cp s3://dsps12bucket/ManagerJar ./ManagerFiles/Manager.jar\n"+
            "java -jar /ManagerFiles/Manager.jar\n";

    public LocalHelper() {
        region = software.amazon.awssdk.regions.Region.US_EAST_1;
        ec2 = Ec2Client.builder()
                .region(region)
                .build();
    }
    //check if the instance tag is "Manager"
    public String startManager() {
        String instanceId="";
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
                                    instanceId=instance.instanceId();
                                    StartInstancesRequest request2 = StartInstancesRequest.builder()
                                            .instanceIds(instanceId)
                                            .build();
                                    ec2.startInstances(request2);
                                    System.out.printf("Successfully started Manager");
                                    startNewManager=false;
                                }else if(instance.state().name().toString().equals("running")||
                                        instance.state().name().toString().equals("pending")){
                                    System.out.printf("Manager is already running");
                                    instanceId=instance.instanceId();
                                    startNewManager=false;
                                }
                            }
                        }
                    }
                }
                nextToken = response.nextToken();
            } while (nextToken != null);
            if(startNewManager){
                System.out.println("Creating a new Manager");
                instanceId=createManager();
            }

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return instanceId;
    }

    private String createManager(){
        IamInstanceProfileSpecification role = IamInstanceProfileSpecification.builder().name("LabInstanceProfile").build();
        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .imageId(amiId)
                .userData(Base64.getEncoder().encodeToString(script.getBytes()))
                .iamInstanceProfile(role)
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
