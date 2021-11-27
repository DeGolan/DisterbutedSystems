package Worker;

import Tools.MessageProtocol;
import Tools.S3Helper;
import Tools.SQSHelper;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.tools.PDFText2HTML;
import org.json.JSONObject;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

public class Worker {
    public static void main(String[] args) {
        System.out.println("Worker is starting...");

        SQSHelper workerManager=new SQSHelper("");//TODO ENTER URL
        SQSHelper managerWorker=new SQSHelper("");//TODO ENTER URL
        WorkerHelper workerHelper=new WorkerHelper(workerManager);

        boolean isFinished=false;
        while (!isFinished){//always true?
            List<Message> messages= managerWorker.getMessages();
            for (Message message:messages){
                MessageProtocol msg=new MessageProtocol(new JSONObject(message.body()));
                String task=msg.getTask();
                String url=msg.getUrl();
                String bucket=msg.getBucketName();
                String path=workerHelper.convertPDF(url,task);

                if(!path.equals("")){//if path="" so error occurred
                    S3Helper s3Helper=new S3Helper();
                    s3Helper.uploadFileToS3(path,bucket,path);//key to the new object is the local path
                    MessageProtocol completeMessage =new MessageProtocol(task,bucket,path,0,"","complete");
                    workerManager.sendMessageToSQS(completeMessage);
                }
                managerWorker.deleteMessage(message);
            }

        }

    }


}
