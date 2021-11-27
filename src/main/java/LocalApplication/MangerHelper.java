package LocalApplication;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.List;

public class MangerHelper {
    private Region region;
    private String key;
    private Ec2Client ec2;
    public  MangerHelper (){
        region = software.amazon.awssdk.regions.Region.US_EAST_1;
        ec2 = Ec2Client.builder()
                .region(region)
                .build();
    }
    public Region getRegion() {
        return region;
    }

    //check if the instance tag is "Manager"
    public void startManager() {
        try {
            String nextToken = null;
            do {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder().build();
                DescribeInstancesResponse response = ec2.describeInstances(request);

                for (Reservation reservation : response.reservations()) {
                    for (Instance instance : reservation.instances()) {
                        List<Tag> tags=instance.tags();
                        if(tags.size()>0){
                            if(tags.get(0).key().equals("Manager")){
                                if(!instance.state().name().toString().equals("running")){
                                    String instanceId=instance.instanceId();
                                    StartInstancesRequest request2 = StartInstancesRequest.builder()
                                            .instanceIds(instanceId)
                                            .build();
                                    ec2.startInstances(request2);
                                    System.out.printf("Successfully started Manager");
                                }else {
                                    System.out.printf("Manager is already running");
                                }
                                return;//only have one manager, can stop iterate
                            }
                        }
                    }
                }
                nextToken = response.nextToken();
            } while (nextToken != null);

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }

    }
}
