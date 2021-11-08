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
        String pdfName=fileURL.substring(fileURL.lastIndexOf('/')+1);

        //try to download and save the pdf file
        try {
            FileUtils.copyURLToFile(new URL(fileURL), new File("./src/main/resources/pdf/"+pdfName));
        }catch (Exception error) {//need to handle exceptions
            throw error;
        }
        //execute the command
        switch (command){
            case "ToImage":
                toImage();
                break;
            case "ToHTML":
                toHTML();
                break;
            case "ToText":
                toText();
                break;
        }
    }
    ///converts the first page of the PDF file to a "png" image.
    private static void toImage() throws IOException {
        PDDocument document = PDDocument.load(new File("./src/main/resources/pdf/test.pdf"));
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        BufferedImage bim = pdfRenderer.renderImageWithDPI(0,300);
        ImageIO.write(bim,"PNG",new File("./src/main/resources/png/test.png"));
        document.close();
    }
    private static void toText() {
    }
    private static void toHTML() {
    }
}
