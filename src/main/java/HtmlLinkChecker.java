import org.apache.commons.cli.*;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import static java.net.HttpURLConnection.*;

public class HtmlLinkChecker {
    static ArrayList<String> fileList = new ArrayList<String>();
    static String reportFile;
    static ArrayList<String> linkList = new ArrayList<String>();
    static int pool;
    static PrintStream reportStream;
    static int counter = 0;

    static boolean CLCheck(String[] args) throws ParseException {
        Options options = new Options();
        Option filesOption = new Option("f", "files", true, "Имена проверочных файлов");
        filesOption.setRequired(true);
        options.addOption(filesOption);
        Option outOption = new Option("o", "out", true, "Имя выходного файла");
        outOption.setRequired(true);
        options.addOption(outOption);
        DefaultParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);
        fileList.addAll(Arrays.asList(commandLine.getOptionValue("f").split(",")));
        reportFile = commandLine.getOptionValue("o");
        if (commandLine.getArgs().length > 0)
        {
            return false;
        }
        return true;
    }

    static void parseDocument(String name) throws IOException
    {
        Document doc;
        if ((name.startsWith("http://"))&&(name.startsWith("https://"))&&(name.startsWith("ftp://"))&&(name.startsWith("file://")))
        {
            doc = Jsoup.connect(name).get();
        }
        else
        {
            File input = new File(name);
            doc = Jsoup.parse(input, "UTF-8");
        }
        W3CDom w3CDom = new W3CDom();
        org.w3c.dom.Document w3cDoc = w3CDom.fromJsoup(doc);
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            NodeList nodeList = (NodeList) xPath.compile("//@href").evaluate(w3cDoc, XPathConstants.NODESET);
            for (int i=0;i<nodeList.getLength();i++)
            {
                addToReport(nodeList.item(i).getNodeValue(), name);
            }
        }
        catch (XPathExpressionException e) {
            System.out.println("Link adding failed");
        }
        try {
            NodeList nodeList = (NodeList) xPath.compile("//@src").evaluate(w3cDoc, XPathConstants.NODESET);
            for (int i=0;i<nodeList.getLength();i++)
            {
                addToReport(nodeList.item(i).getNodeValue(), name);
            }
        }
        catch (XPathExpressionException e) {
            System.out.println("Link adding failed");
        }

    }

    static Map.Entry<Integer, String> checkLink(String link, String name) throws IOException
    {
        URL url;
        if (link.startsWith("/"))
        {
            String str = name+link;
            url = new URL(str);
        }
        else if (link.startsWith("file://"))
        {
            String str;
            if (link.startsWith("file://localhost/"))
            {
                str = link.substring(16);
            }
            else
            {
                str = link.substring(8);
            }
            if (Files.exists(Paths.get(str)))
            {
                return new AbstractMap.SimpleEntry<>(HTTP_OK, "OK");
            }
            else
            {
                return new AbstractMap.SimpleEntry<>(HTTP_NOT_FOUND, "Not Found");
            }
        }
        else
        {
            url = new URL(link);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            int status = con.getResponseCode();
            String mess = con.getResponseMessage();
            con.disconnect();
            return new AbstractMap.SimpleEntry<>(status, mess);
        }
        return null;
    }

    static void addToReport(String link, String name)
    {
        try
        {
            Map.Entry<Integer, String> map = checkLink(link, name);
            if ((map.getKey() < HTTP_OK)||(map.getKey() >= HTTP_MULT_CHOICE))
            {
                reportStream.println(link+","+map.getKey()+","+map.getValue());
                counter++;
            }
        }
        catch (IOException e)
        {
            reportStream.println(link+",-1,No Such Host");
            counter++;
        }
    }

    static int configProperties() throws IOException
    {
        Properties props = new Properties(1);
        props.load(new FileInputStream("config.properties"));
        int pool = Integer.parseInt(props.getProperty("thread_number", "8"));
        return pool;
    }

    static public void main(String[] args)
    {
        try
        {
            if (!CLCheck(args))
            {
                System.out.println("Command line reading error");
                return;
            }
        }
        catch (ParseException e)
        {
            System.out.println("Parsing failed");
            return;
        }
        try
        {
            reportStream = new PrintStream(reportFile);
        }
        catch (IOException e)
        {
            System.out.println("Failed report creation");
            return;
        }
        try
        {
            pool = configProperties();
        }
        catch (IOException e)
        {
            System.out.println("Config error");
            return;
        }
        ExecutorService executor = Executors.newFixedThreadPool(pool);
        for (String string: fileList)
        {
            executor.execute(()->{
                try {
                    parseDocument(string);
                }
                catch (IOException e)
                {
                    System.out.println("Read error");
                    return;
                }
            });
        }
        executor.shutdown();
        try
        {
            executor.awaitTermination(3, TimeUnit.MINUTES);
        }
        catch (InterruptedException e) {
            System.out.println("Thread error");
        }
        System.out.println("Found "+counter+" broken links, for details check file "+reportFile);
    }
}
