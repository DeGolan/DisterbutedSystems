package LocalApplication;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.regions.Region;


public class LocalApplication {
    public static void main(String[] args) {

        String amiId = "ami-01cc34ab2709337aa";
        String name = "Manager";
        Region region= Region.US_EAST_1;

        Ec2Client ec2Client = Ec2Client.builder()
                .region(region)
                .build();

        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .instanceType(InstanceType.T2_MICRO)
                .imageId(amiId)
                .maxCount(1)
                .minCount(1)
                .keyName("dsps12")
                .build();

        RunInstancesResponse response = ec2Client.runInstances(runRequest);


        String instanceId = response.instances().get(0).instanceId();

        Tag tag = Tag.builder()
                .key("Name")
                .value(name)
                .build();

        CreateTagsRequest tagRequest = CreateTagsRequest.builder()
                .resources(instanceId)
                .tags(tag)
                .build();
        try {
            ec2Client.createTags(tagRequest);
            System.out.printf(
                    "Successfully started EC2 instance %s based on AMI %s",
                    instanceId, amiId);

        } catch (Ec2Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        System.out.println("Done!");


    }
}
