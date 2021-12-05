package LocalApplication;

import Tools.S3Helper;

public class UploadJars {
    public static void main(String[] args){
        String bucket = "dsps12bucket";
        S3Helper s3Helper=new S3Helper();
        String managerJarPath="/home/vagrant/IdeaProjects/DisterbutedSystems/out/artifacts/Manager_jar/Manager.jar";
        String workerJarPath="/home/vagrant/IdeaProjects/DisterbutedSystems/out/artifacts/Worker_jar/Worker.jar";
        System.out.println("upload manager jar file to S3");
        s3Helper.uploadFileToS3(managerJarPath, bucket, "ManagerJar");
        System.out.println("upload worker jar file to S3");
        s3Helper.uploadFileToS3(workerJarPath, bucket, "WorkerJar");
        s3Helper.closeS3();
    }
}
