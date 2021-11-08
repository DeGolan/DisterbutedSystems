package PDFBox;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

public class PDFReader {

    public static void main(String[] args) throws IOException {

        System.out.println("Start working...");
        //example for a possible msg from sqs
        //ToImage	https://www.bethelnewton.org/images/Passover_Guide_BOOKLET.pdf
        String command="ToImage";
        String fileURL="https://www.bethelnewton.org/images/Passover_Guide_BOOKLET.pdf";

        //get the name of the pdf
        String fileName=fileURL.substring(fileURL.lastIndexOf('/')+1,fileURL.lastIndexOf('.'));

        //try to download and save the pdf file
        try {
            FileUtils.copyURLToFile(new URL(fileURL), new File("./src/main/resources/pdf/"+fileName+".pdf"));
            PDDocument document = PDDocument.load(new File("./src/main/resources/pdf/"+fileName+".pdf"));
            switch (command){
                case "ToImage":
                    toImage(document,fileName);
                    break;
                case "ToHTML":
                    toHTML(document,fileName);
                    break;
                case "ToText":
                    toText(document,fileName);
                    break;
            }
            document.close();
        }catch (Exception error) {//need to handle exceptions
            throw error;
        }
    }
    ///converts the first page of the PDF file to a "png" image.
    private static void toImage(PDDocument document,String filename) throws IOException {
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        BufferedImage bim = pdfRenderer.renderImageWithDPI(0,300);
        ImageIO.write(bim,"PNG",new File("./src/main/resources/png/"+filename+".png"));
        System.out.println("The first page of "+filename+" has converted to Image");
    }
    private static void toText(PDDocument document,String filename) throws IOException {
        PDFTextStripper stripper=new PDFTextStripper();
        stripper.setStartPage(2);//need to change to page 0
        stripper.setEndPage(2);//need to change to page 0
        String text=stripper.getText(document);
        FileWriter file=new FileWriter("./src/main/resources/text/"+filename+".txt");
        file.write(text);
        file.close();
        System.out.println("The first page of "+filename+" has converted to Text");
    }
    private static void toHTML(PDDocument document,String filename) {
    }
}
