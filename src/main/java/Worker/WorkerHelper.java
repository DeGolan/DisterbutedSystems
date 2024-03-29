package Worker;

import Tools.MessageProtocol;
import Tools.SQSHelper;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.tools.PDFText2HTML;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

public class WorkerHelper {
    SQSHelper workersManger;
    public  WorkerHelper(SQSHelper workersManger){
        this.workersManger=workersManger;

    }
    //uploading the pdf_src to the s3
    public String convertPDF(String fileURL, String task,String localAppId) {
        String returnPath="";
        try {
            if(!fileURL.contains("https")){//handle the case of http
                fileURL=fileURL.substring(0,fileURL.indexOf(':'))+'s'+fileURL.substring((fileURL.indexOf(':')));
                System.out.println(fileURL);
            }
            String fileName=fileURL.substring(fileURL.lastIndexOf('/')+1,fileURL.lastIndexOf('.'));
            FileUtils.copyURLToFile(new URL(fileURL), new File("/WorkerFiles/"+fileName+".pdf"),15000,15000);
            PDDocument document = PDDocument.load(new File("/WorkerFiles/"+fileName+".pdf"));

            switch (task){
                case "ToImage":
                    returnPath=toImage(document,fileName,localAppId);
                    break;
                case "ToHTML":
                    returnPath=toHTML(document,fileName,localAppId);
                    break;
                case "ToText":
                    returnPath=toText(document,fileName,localAppId);
                    break;
            }
            document.close();
        }catch (Exception error) {//need to handle exceptions
            System.out.println("Sending error msg");
            MessageProtocol msg=new MessageProtocol(task,"",error.getMessage(),0,fileURL,"error",localAppId);
            workersManger.sendMessageToSQS(msg);
        }
        return returnPath;
    }
    private String toImage(PDDocument document,String filename,String localAppId) throws IOException {
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        BufferedImage bim = pdfRenderer.renderImageWithDPI(0,300);
        String path="/WorkerFiles/"+filename+localAppId+".png";
        ImageIO.write(bim,"PNG",new File(path));
        System.out.println("The first page of "+filename+" has converted to Image");
        return path;
    }
    private String toText(PDDocument document,String filename,String localAppId) throws IOException {
        PDFTextStripper stripper=new PDFTextStripper();
        stripper.setStartPage(1);
        stripper.setEndPage(1);
        String text=stripper.getText(document);
        String path="/WorkerFiles/"+filename+localAppId+".txt";
        FileWriter file=new FileWriter(path);
        file.write(text);
        file.close();
        System.out.println("The first page of "+filename+" has converted to Text");
        return path;
    }
    private String toHTML(PDDocument document,String filename,String localAppId) throws IOException {
        PDFTextStripper stripper= new PDFText2HTML();
        stripper.setStartPage(1);
        stripper.setEndPage(1);
        String text=stripper.getText(document);
        String path="/WorkerFiles/"+filename+localAppId+".html";
        FileWriter file=new FileWriter(path);
        file.write(text);
        file.close();
        System.out.println("The first page of "+filename+" has converted to HTML");
        return path;
    }
}
