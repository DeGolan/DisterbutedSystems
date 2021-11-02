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
    //ToText	http://www.chabad.org/media/pdf/42/kUgi423322.pdf
    public static void downloadFile(URL url, String fileName) throws IOException {
        FileUtils.copyURLToFile(url, new File(fileName));
    }
    //ToImage - convert the first page of the PDF file to a "png" image.

    public static void main(String[] args) throws IOException {

        System.out.println("hello");
        //ToImage	https://www.bethelnewton.org/images/Passover_Guide_BOOKLET.pdf
        try {
            downloadFile(new URL("https://www.bethelnewton.org/images/Passover_Guide_BOOKLET.pdf"),"./src/main/resources/pdf/test.pdf");
        }catch (Exception error) {//need to handle exceptions
            throw error;
        }
        PDDocument pdf=PDDocument.load(new File("./src/main/resources/pdf/test.pdf"));
//        Writer output = new PrintWriter("./src/output/pdf.html","utf-8");
        PDFTextStripper stripper=null;
        stripper.setStartPage(0);
        stripper.setEndPage(0);
        FileOutputStream fos = new FileOutputStream("./src/output/pdf.html");
//        stripper.writeText(pdf,);
        fos.close();
        //need to move to function
//        PDDocument document = PDDocument.load(new File("./src/main/resources/pdf/test.pdf"));
//        PDFRenderer pdfRenderer = new PDFRenderer(document);
//        BufferedImage bim = pdfRenderer.renderImageWithDPI(0,300);
//        ImageIO.write(bim,"PNG",new File("./src/main/resources/png/test.png"));
//        document.close();


    }
}
