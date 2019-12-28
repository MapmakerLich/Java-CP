import org.apache.commons.cli.*;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.*;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

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
        File input = new File(name);
        Document doc = Jsoup.parse(input, "UTF-8");
        W3CDom w3CDom = new W3CDom();
        org.w3c.dom.Document w3cDoc = w3CDom.fromJsoup(doc);
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            NodeList nodeList = (NodeList) xPath.compile("//@href").evaluate(w3cDoc, XPathConstants.NODESET);
            for (int i=0;i<nodeList.getLength();i++)
            {
                linkList.add(nodeList.item(i).getNodeValue());
            }
        }
        catch (XPathExpressionException e) { }
        try {
            NodeList nodeList = (NodeList) xPath.compile("//@src").evaluate(w3cDoc, XPathConstants.NODESET);
            for (int i=0;i<nodeList.getLength();i++)
            {
                linkList.add(nodeList.item(i).getNodeValue());
            }
        }
        catch (XPathExpressionException e) { }

    }

    static Map.Entry<Integer, String> checkLink(String link) throws IOException
    {
        URL url = new URL(link);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        int status = con.getResponseCode();
        String mess = con.getResponseMessage();
        con.disconnect();
        return new AbstractMap.SimpleEntry<>(status, mess);
    }

    static void addToReport(String link)
    {
        try
        {
            Map.Entry<Integer, String> map = checkLink(link);
            if ((map.getKey() < 200)||(map.getKey() >= 300))
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
            for (String string: fileList)
            {
                parseDocument(string);
            }
        }
        catch (IOException e)
        {
            System.out.println("Read error");
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
        for (String string: linkList)
        {
            executor.execute(()->{
                addToReport(string);
            });
        }
        executor.shutdown();
        try
        {
            executor.awaitTermination(3, TimeUnit.MINUTES);
        }
        catch (InterruptedException e) { }
        System.out.println("Found "+counter+" broken links, for details check file "+reportFile);
    }
}
