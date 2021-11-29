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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

public class WorkerHelper {
    SQSHelper workersManger;
    public  WorkerHelper(SQSHelper workersManger){
        this.workersManger=workersManger;

    }
    //uploading the pdf_src to the s3

    public String convertPDF(String fileURL, String task) {
        String returnPath="";
        try {
            if(!fileURL.contains("https")){//handle the case of http
                fileURL=fileURL.substring(0,fileURL.indexOf(':'))+'s'+fileURL.substring((fileURL.indexOf(':')));
                System.out.println(fileURL);
            }
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
            System.out.println("Sending error msg");
            MessageProtocol msg=new MessageProtocol(error.toString(),"","",0,"","error");
            workersManger.sendMessageToSQS(msg);

        }

        return returnPath;
    }
    private String toImage(PDDocument document,String filename) throws IOException {
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        BufferedImage bim = pdfRenderer.renderImageWithDPI(0,300);
        String path="./src/main/resources/png/"+filename+".png";
        ImageIO.write(bim,"PNG",new File(path));
        System.out.println("The first page of "+filename+" has converted to Image");
        return path;
    }
    private String toText(PDDocument document,String filename) throws IOException {
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
    private String toHTML(PDDocument document,String filename) throws IOException {
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
}
