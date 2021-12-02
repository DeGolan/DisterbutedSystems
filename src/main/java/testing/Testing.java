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
import java.io.*;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class Testing {
    public static void main(String[] args) {
        /*System.out.println("Worker is starting...");
        String[]urls={"https://www.jewishfederations.org/local_includes/downloads/39497.pdf",
            "http://www.st.tees.org.uk/assets/Downloads/Passover-service.pdf",
            "http://www.chabad.org/media/pdf/42/kUgi423322.pdf",
            "http://www.bethelnewton.org/images/Passover_Guide_BOOKLET.pdf",
            "http://scheinerman.net/judaism/pesach/haggadah.pdf",
            "http://www.crcweb.org/Passover/cRcPassoverGuide13_FINAL%20-%20WITHOUT%20ADS.pdf",
            "http://www.kidswithfoodallergies.org/KFA-Celebrates-Passover.pdf",
                "http://www.fairwaymarket.com/wp-content/uploads/2013/09/Kosher-Passover-Menu_FINAL.pdf",
        "http://www.jewishcamp.org/sites/default/files/u8/Camp%20Passover%202013.pdf",
                "http://www.bnaitikvah.net/prayer_ritual/documents/PassoverGuidelines.pdf",
        "http://www.jsor.org/2014Passoverlist.pdf",
                "http://www.rabbinicalassembly.org/sites/default/files/public/jewish-law/holidays/pesah/why-do-we-sing-the-song-of-songs-on-passover.pdf",
                "http://www.betamshalom.org/sites/default/files/site_pdfs/a_passover_prayer.pdf",
                "http://www.jewishmuseummilwaukee.org/docs/educators/SederActivities.pdf",
        "http://www.hebrew4christians.com/Holidays/Spring_Holidays/Pesach/H4C_Passover_Seder.pdf",
        "http://ajws.org/what_we_do/education/publications/holiday_resources/in_search_of_freedom_passover_seder.pdf",
        "http://www.chosenpeople.com/main/pdf/HowToPrepareForAPassoverDinner.pdf",
        "http://www.russoscateringboston.com/passover.pdf",
        "http://www.tbshalom.com/newsletter.pdf",
        "http://www.bethelsudbury.org/wp-content/uploads/2009/12/Kitniyot-Quinoa-and-Kashrut.pdf",
        "http://tbel.jvillagenetwork.com/uploadedFiles/site/Sitewide/Right_Column/Rabbinical%20Assembly%20Pesah%20Guide%205774.pdf",
        "http://www.federationcja.org/media/mediaContent/haggadah_e.pdf",
        "http://www.maxandbennys.com/MENU/Passover%202013%20MENU.pdf",
        "http://ramahdarom.org/wp-content/uploads/2013/03/passover-book-v10_website.pdf",
        "http://elearning.huc.edu/jhvrc/docs/JQ%20Haggadah%20Final%20no%20pics%204%209%2008.pdf",
        "http://rcdow.org.uk/att/files/faith/catechesis/eucharist/passoverguidelines.pdf",
        "http://www.fbci.org/ministries/isra_docs/Passover.pdf",
        "https://www.jewishlibraries.org/main/Portals/0/AJL_Assets/documents/bib_bank/PassoverResources.pdf",
        "http://www.dgsdelicatessen.com/assets/DGS_Passover_Menu.pdf",
        "http://www.thebagelemporium.com/images/BE-Holiday-Menu.pdf",
        "http://www.sjanj.net/PassoverandEaster.pdf",
        "http://yahweh.com/pdf/Booklet_Passover.pdf",
        "http://www.theisraelofgod.com/Text%20Lessons/Is%20this%20not%20the%20Lord's%20Passover.pdf",
        "http://www.ccss.org/Resources/Documents/Passover%20_%20Easter%20in%20Public%20Schools.pdf",
        "http://www.adventurevalleydaycamp.com/Passover.pdf",
        "http://foodmannosh.com/wp-content/uploads/2013/02/Matzolah-article-Aus-Jewish-Outlook2.pdf",
        "http://www.crumbs.com/media/inline/HolidayMenu_Passover_10.pdf",
        "http://www.cmu.edu/dining/pdf/passover-meal-plan-2014.pdf",
        "http://www.thejewishcollection.com/passoverjokes.pdf",
        "http://www.comingoutofegypt.com/Chronology-Passover-Week.pdf",
        "http://www.elijahrocks.net/pdf/PassoverBlessing.pdf",
        "http://www.biblicalfoundations.org/wp-content/uploads/2013/01/Supper_6-30.pdf",
        "http://www.yeshuatyisrael.com/pdf/passover%20recipes.pdf",
        "http://www.state.nj.us/oag/ca/kosher/Passover%20Disclosure.pdf",
        "http://www.mallinckrodt.com/uploadedFiles/Content/Specialty_Pharmaceuticals/Active_Pharmaceutical_Ingredients/Products/Stearates-Phosphates/5712.pdf",
        "http://www.templefjc.org/pubs/Passover.pdf",
        "http://www.kashrut.com/Passover/pdf/OK_PassoverFoodGuide2014.pdf",
        "http://www.zingermansdeli.com/wp-content/uploads/2013/03/Passover13.pdf",
        "http://www.barrylou.com/books/TellingTheStoryInside.pdf",
        "http://www.jfrankhenderson.com/pdf/jesusandseder.PDF",
        "http://avirtualpassover.com/pdf/introduction.pdf"};
        String [] tasks={"ToImage","ToHTML","ToText","ToImage","ToImage","ToImage","ToImage","ToImage","ToHTML","ToHTML","ToHTML","ToHTML","ToHTML"};
        String task=tasks[1];
            for(int i=0;i<urls.length;i++)
            {
//                String task=tasks[0];
                String url=urls[i];
                System.out.println("Try Convert PDF "+url);
                String path=convertPDF2(url,task);
                System.out.println("Path returned "+path);
            }
*/
//        fileToHTML("./src/main/resources/text/summaryFile.txt","./src/main/resources/text/summaryFile.html");
        String path = convertPDF2("http://yahweh.com/pdf/Booklet_Passover.pdf","ToText");
        System.out.println("Path returned "+path);

    }
    public static String convertPDF2(String fileURL, String task) {
        String returnPath="";
        try {
            String fileName=fileURL.substring(fileURL.lastIndexOf('/')+1,fileURL.lastIndexOf('.'));
            FileUtils.copyURLToFile(new URL(fileURL), new File("./src/main/resources/pdf/",fileName+".pdf"));
            PDDocument document = PDDocument.load(new File("./src/main/resources/pdf/"+fileName+".pdf"));
            System.out.println("PDF downloaded");

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
    private static void fileToHTML(String filename, String output){
        try {
            BufferedReader br=new BufferedReader(new FileReader(filename));
            String line;

            FileOutputStream fs = new FileOutputStream(output);
            OutputStreamWriter out = new OutputStreamWriter(fs);
            out.write("<html>");
            out.write("<head>");
            out.write("<title>");
            out.write("Summary File");
            out.write("</title>");
            out.write("</head>");
            out.write("<body>");
            while((line=br.readLine()) != null) {
                out.write(line);
                out.write("<br>");
            }
            out.write("</body>");
            out.write("</html>");
            out.close();
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
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
        stripper.setStartPage(1);//need to change to page 0
        stripper.setEndPage(1);//need to change to page 0
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
        stripper.setStartPage(1);
        stripper.setEndPage(1);
        String text=stripper.getText(document);
        String path="./src/main/resources/HTML/"+filename+".html";
        FileWriter file=new FileWriter(path);
        file.write(text);
        file.close();
        System.out.println("The first page of "+filename+" has converted to HTML");
        return path;
    }
}
