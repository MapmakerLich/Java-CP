import org.apache.commons.cli.ParseException;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Properties;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.*;

public class HtmlLinkCheckerTest {

    @Test
    public void CLCheck()
    {
        String[] args = {"--f", "f1.html", "--out", "report.csv"};
        try {
            assertTrue(HtmlLinkChecker.CLCheck(args));
        }
        catch (ParseException e)
        {
            System.out.println("Command line check test failed");
        }
    }

    @Test
    public void checkLinkTest() {
        Map.Entry<Integer, String> entry = new AbstractMap.SimpleEntry<>(HTTP_OK, "OK");
        try {
            assertEquals(entry, HtmlLinkChecker.checkLink("file:///C:/3.mwb", "http://google.com"));
        }
        catch (IOException e)
        {
            System.out.println("Working link check test failed");
        }
    }

    @Test
    public void configProperties() {
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
}