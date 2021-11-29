package testing;

import Tools.MessageProtocol;
import Tools.S3Helper;
import Tools.SQSHelper;
import Worker.WorkerHelper;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.tools.PDFText2HTML;
import org.json.JSONObject;
import software.amazon.awssdk.services.sqs.model.Message;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class Testing {
    public static void main(String[] args) {
        System.out.println("Worker is starting...");
        String[]urls={"https://www.jewishfederations.org/local_includes/downloads/39497.pdf",
            "http://www.st.tees.org.uk/assets/Downloads/Passover-service.pdf",
            "http://www.chabad.org/media/pdf/42/kUgi423322.pdf",
            "http://www.bethelnewton.org/images/Passover_Guide_BOOKLET.pdf",
            "http://scheinerman.net/judaism/pesach/haggadah.pdf",
            "http://www.crcweb.org/Passover/cRcPassoverGuide13_FINAL%20-%20WITHOUT%20ADS.pdf",
            "http://www.kidswithfoodallergies.org/KFA-Celebrates-Passover.pdf"};
        String [] tasks={"ToImage","ToHTML","ToText","ToImage","ToImage","ToImage","ToImage"};
        boolean isFinished=false;

            for(int i=0;i<urls.length;i++)
            {
                if(!urls[i].contains("https")){
                    urls[i]=urls[i].substring(0,urls[i].indexOf(':'))+'s'+urls[i].substring((urls[i].indexOf(':')));
                    System.out.println(urls[i]);
                }
                String task=tasks[i];
                String url=urls[i];
                System.out.println("Try Convert PDF "+url);
                String path=convertPDF2(url,task);
                System.out.println("Path returned "+path);
            }



    }
    public static String convertPDF2(String fileURL, String task) {
        String returnPath="";
        try {

            String fileName=fileURL.substring(fileURL.lastIndexOf('/')+1,fileURL.lastIndexOf('.'));
            FileUtils.copyURLToFile(new URL(fileURL), new File("./src/main/resources/pdf/"+fileName+".pdf"));
            PDDocument document = PDDocument.load(new File("./src/main/resources/pdf/"+fileName+".pdf"));

            switch (task){
                case "ToImage":
                    returnPath=toImage2(document,fileName);
                    break;
                case "ToHTML":
                    returnPath=toHTML2(document,fileName);
                    break;
                case "ToText":
                    returnPath=toText2(document,fileName);
                    break;
            }
            document.close();
        }catch (Exception error) {//need to handle exceptions
            System.out.println("Sending error msg");
            MessageProtocol msg=new MessageProtocol(error.toString(),"","",0,"","error");


        }
        return returnPath;
    }
    private static String toImage2(PDDocument document,String filename) throws IOException {
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        BufferedImage bim = pdfRenderer.renderImageWithDPI(0,300);
        String path="./src/main/resources/png/"+filename+".png";
        ImageIO.write(bim,"PNG",new File(path));
        System.out.println("The first page of "+filename+" has converted to Image");
        return path;
    }
    private static String toText2(PDDocument document,String filename) throws IOException {
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
    private static String toHTML2(PDDocument document,String filename) throws IOException {
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
