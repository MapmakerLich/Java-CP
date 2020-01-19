import java.io.IOException;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_MULT_CHOICE;
import static java.net.HttpURLConnection.HTTP_OK;

public class Saver implements LinkSaver {
    static public void addToReport(String link, String name)
    {
        try
        {
            Map.Entry<Integer, String> map = HtmlLinkChecker.checkLink(link, name);
            if ((map.getKey() < HTTP_OK)||(map.getKey() >= HTTP_MULT_CHOICE))
            {
                HtmlLinkChecker.reportStream.println(link+","+map.getKey()+","+map.getValue());
                HtmlLinkChecker.counter++;
            }
        }
        catch (IOException e)
        {
            HtmlLinkChecker.reportStream.println(link+",-1,No Such Host");
            HtmlLinkChecker.counter++;
        }
    }
}
