import java.io.IOException;

public interface LinkSaver {
    static void addToReport(String link, String name)
    {
        try
        {
            HtmlLinkChecker.checkLink(link, name);
        }
        catch (IOException ex)
        {
            System.out.println("Link checking failed");
        }
    };
}
