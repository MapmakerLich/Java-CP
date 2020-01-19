import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Properties;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.*;

public class HtmlLinkCheckerTest {

    @Test
    @DisplayName("Checking command line parsing")
    void CLCheckTest()
    {
        String[] args = {"--f", "f1.html", "--out", "report.csv"};
        try {
            assertTrue(HtmlLinkChecker.CLCheck(args));
        }
        catch (ParseException e)
        {
            System.out.println("Command line check test failed");
        }
        args = new String[] {"--f", "f1.html", "--out", "report.csv", "--g", "nothing.txt"};
        try {
            assertEquals(HtmlLinkChecker.CLCheck(args), false);
        }
        catch (ParseException e)
        {
            System.out.println("Command line check test passed");
        }
    }

    @Test
    @DisplayName("Document parse checking")
    void parseDocumentTest() {
        HtmlLinkChecker.reportFile = "report.csv";
        try{
            HtmlLinkChecker.reportStream = new PrintStream(HtmlLinkChecker.reportFile);
        }
        catch (FileNotFoundException e)
        {
            System.out.println("Document parse check failed");
        }
        Boolean flag = true;
        try {
            HtmlLinkChecker.parseDocument("f1.html");
        }
        catch (IOException e)
        {
            flag = false;
        }
        finally {
            assertTrue(flag);
        }
    }

    @Test
    @DisplayName("Testing link checker")
    void checkLinkTest() {
        Map.Entry<Integer, String> entry = new AbstractMap.SimpleEntry<>(HTTP_OK, "OK");
        Map.Entry<Integer, String> entry1 = new AbstractMap.SimpleEntry<>(HTTP_NOT_FOUND, "Not Found");
        try {
            assertEquals(entry, HtmlLinkChecker.checkLink("file:///C:/3.mwb", "http://google.com"));
            assertEquals(entry, HtmlLinkChecker.checkLink("file://localhost/C:/3.mwb", "http://google.com"));
            assertEquals(entry, HtmlLinkChecker.checkLink("/200", "http://httpstat.us"));
            assertEquals(entry1, HtmlLinkChecker.checkLink("file://localhost/C:/3.mw", "http://google.com"));
        }
        catch (IOException e)
        {
            System.out.println("Working link check test failed");
        }
    }

    @Test
    @DisplayName("Properties configuration test")
    void configPropertiesTest() {
        Properties props = new Properties(1);
        try
        {
            props.load(new FileInputStream("config.properties"));
        }
        catch (IOException e)
        {
            System.out.println("Error: properties read fail");
        }
        int pool = Integer.parseInt(props.getProperty("thread_number", "8"));
        try
        {
            assertEquals(pool, HtmlLinkChecker.configProperties());
        }
        catch (IOException e)
        {
            System.out.println("Properties config test failed");
        }
    }

    @Test
    @DisplayName("Main test")
    void MainTest() {
        HtmlLinkChecker.main(new String[] {"--f", "f1.html", "--out", "report.csv"});
    }
}