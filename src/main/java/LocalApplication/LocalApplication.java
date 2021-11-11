package LocalApplication;

import java.io.IOException;
import java.nio.file.Paths;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.S3Client;


public class LocalApplication {
    public static void main(String[] args) {

        Region region = Region.US_EAST_1;

//        //uploading the pdf_src to the s3
//        S3Client s3 = S3Client.builder().region(region).build();
//        String bucket = "dsps12bucket";
//        String key = "pdf_src";
//        System.out.println("Uploading pdf source file to S3...");
//        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(),
//                RequestBody.fromFile(Paths.get("/home/vagrant/DistributedSystems/src/main/resources/input-sample-1.txt")));
//        System.out.println("Upload complete");
//        System.out.printf("%n");
//        System.out.println("Closing the connection to {S3}");
//        s3.close();
//        System.out.println("Connection closed");


        Ec2Client ec2 = Ec2Client.builder()
                .region(region)
                .build();
//        String instanceId="i-04645567067640ab5";
//        StartInstancesRequest request = StartInstancesRequest.builder()
//                .instanceIds(instanceId)
//                .build();
//            ec2.startInstances(request);
//            System.out.printf("Successfully stopped instance %s", instanceId);

        if(!findManager(ec2)){

        };
            ec2.close();
    }
    public static boolean findManager2(Ec2Client ec2) {
        Filter filter = Filter.builder().name("instance-state-name").values()
    }
    //check if the instance tag is "Manager"
    public static boolean findManager(Ec2Client ec2) {
        try {
            String nextToken = null;
            do {
                Filter filter = Filter.builder()
                        .name()
                        .values("running")
                        .build();

                DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                        .filters(filter)
                        .build();
                DescribeInstancesResponse response = ec2.describeInstances(request);
                for (Reservation reservation : response.reservations()) {
                    for (Instance instance : reservation.instances()) {
                       if(instance.tags().get(0).key().equals("Manager")){
                           return true;
                       }

                    }
                }
                nextToken = response.nextToken();
            } while (nextToken != null);

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return  false;
    }
}
