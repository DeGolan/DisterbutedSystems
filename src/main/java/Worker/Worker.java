package Worker;

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

        Region region = Region.US_EAST_1;

        String queueUrl="https://sqs.us-east-1.amazonaws.com/537488554861/Manager-Workers";


        SqsClient sqsClient = SqsClient.builder()
                .region(region)
                .build();
        S3Client s3 = S3Client.builder().region(region).build();
        String bucket = "dsps12bucket";

        List<Message> msgs=receiveMessages(sqsClient,queueUrl);

        System.out.println("msgs size is: "+msgs.size());

        JSONObject json=null;

        if(msgs.size()>0){
            json=new JSONObject(msgs.get(0).body());
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(msgs.get(0).receiptHandle())
                    .build();
            sqsClient.deleteMessage(deleteRequest);
            String task=(String)json.get("task");
            String url=(String)json.get("url");
            try{
                String path=convertPDF(url,task,sqsClient);
                uploadPDFListToS3(s3,bucket,path);
                JSONObject returnJson=new JSONObject();
                returnJson.put("for","Manager");
                returnJson.put("status","complete");
                returnJson.put("url",url);
                returnJson.put("task",task);
                returnJson.put("s3Key",path);
                sendMessageToSQS(sqsClient,returnJson.toString());



            }
            catch (Exception error){//
               System.out.println(error.toString());
            }



        }

    }



    //uploading the pdf_src to the s3
    public static void uploadPDFListToS3(S3Client s3, String bucket, String key){
        System.out.println("Uploading file  to S3...");
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(),
                RequestBody.fromFile(Paths.get(key)));
        System.out.println("Upload complete");
        System.out.printf("%n");
        System.out.println("Closing the connection to {S3}");
        s3.close();
        System.out.println("Connection closed");
    }

    public static String convertPDF(String fileURL,String task,SqsClient sqsClient) throws IOException {
        String returnPath="";
        try {

            String fileName=fileURL.substring(fileURL.lastIndexOf('/')+1,fileURL.lastIndexOf('.'));
            FileUtils.copyURLToFile(new URL(fileURL), new File("./src/main/resources/pdf/"+fileName+".pdf"));
            PDDocument document = PDDocument.load(new File("./src/main/resources/pdf/"+fileName+".pdf"));
            switch (task){
                case "ToImage":
                    returnPath=toImage(document,fileName);
                    break;
                case "ToHTML":
                    returnPath=toHTML(document,fileName);
                    break;
                case "ToText":
                    returnPath=toText(document,fileName);
                    break;
            }
            document.close();
        }catch (Exception error) {//need to handle exceptions
            JSONObject json =new JSONObject();
            json.put("task",task);
            json.put("url",fileURL);
            json.put("error",error.toString());
            json.put("status","failed");
            json.put("for","Manager");
            sendMessageToSQS(sqsClient,json.toString());
            throw error;
        }
        return returnPath;
    }
    public static void sendMessageToSQS (SqsClient sqsClient, String msg){
        try {
            SendMessageRequest send_msg_request = SendMessageRequest.builder()
                    .queueUrl("https://sqs.us-east-1.amazonaws.com/537488554861/Manager-Workers")
                    .messageBody(msg)
                    //  .delaySeconds(5)
                    .build();
            sqsClient.sendMessage(send_msg_request);
        } catch (QueueNameExistsException e) {
            throw e;
        }

    }
    ///converts the first page of the PDF file to a "png" image.
    private static String toImage(PDDocument document,String filename) throws IOException {
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        BufferedImage bim = pdfRenderer.renderImageWithDPI(0,300);
        String path="./src/main/resources/png/"+filename+".png";
        ImageIO.write(bim,"PNG",new File(path));
        System.out.println("The first page of "+filename+" has converted to Image");
        return path;
    }
    private static String toText(PDDocument document,String filename) throws IOException {
        PDFTextStripper stripper=new PDFTextStripper();
        stripper.setStartPage(2);//need to change to page 0
        stripper.setEndPage(2);//need to change to page 0
        String text=stripper.getText(document);
        String path="./src/main/resources/text/"+filename+".txt";
        FileWriter file=new FileWriter(path);
        file.write(text);
        file.close();
        System.out.println("The first page of "+filename+" has converted to Text");
        return path;
    }
    private static String toHTML(PDDocument document,String filename) throws IOException {
        PDFTextStripper stripper= new PDFText2HTML();
        stripper.setStartPage(2);
        stripper.setEndPage(2);
        String text=stripper.getText(document);
        String path="./src/main/resources/HTML/"+filename+".html";
        FileWriter file=new FileWriter(path);
        file.write(text);
        file.close();
        System.out.println("The first page of "+filename+" has converted to HTML");
        return path;
    }

    public static List<Message> receiveMessages(SqsClient sqsClient, String queueUrl) {

        System.out.println("\nReceive messages");

        try {
            ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .build();
            List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();

            return messages;
        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return null;
    }
}
