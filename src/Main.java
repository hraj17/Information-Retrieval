import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by harishrajagopal on 9/16/14.
 */
public class Main {

    public static PrintWriter writer;
    public static Integer pagesVisited = 0;
    public static HashMap<Integer, Queue<String>> unvisitedLinks = new HashMap<Integer, Queue<String>>();
    //public static ArrayList<Queue<String>> unvisitedLinks = new ArrayList<Queue<String>>();

    public static Queue<String> visited = new LinkedList<String>();

    public static String baseURL = "http://en.wikipedia.org";
    public static String keyPhrase = "";
    public static final Integer levels = 3;
    public static String seed ="";

    public static void addLink(int n, String Link)
    {
        //check if url has already been visited
        if(visited.contains(Link))
        {
            //already visited sorry
            return;
        }
        else
        {
            Queue temp;
            if(n+1 > levels){
                //doesnt matter what links we have here as we dont need to add it to our queue
                return;
            }
            //has the link already been added to the queue?
            for(int i=n ; i<levels ; i++) {
                temp = unvisitedLinks.get(i);
                if (temp.contains(Link)) {
                   //added already, will visit soon
                    return;
                }
            }
            //you wanna go ahead only if the next level is required
                temp = unvisitedLinks.get(n + 1);
                temp.add(Link);
                unvisitedLinks.put(n + 1, temp);
        }
    }
    public static void removeLink(int n, String Link)
    {
        if(visited.contains(Link))
        {
            //already visited sorry
            visited.remove(Link);
        }
    }
    public static int scanPage(int n,Document page,String qEntry)
    {
        Elements links = page.select("a[href]");
        Elements can = page.select("link[rel]");
        String canonical = "";
        for(Element c : can)
        {
            if(c.attr("rel").equals("canonical")) {
                canonical = c.attr("href");
                if(!canonical.equals(qEntry))
                {
                    //you need to remove url and consider the new one
                    removeLink(n, qEntry);
                    addLink(n-1, canonical);
                    return 0;
                }
            }
        }
        for (Element link : links) {
            String url = link.attr("href");
            if ((!url.contains("/wiki/Main_Page")) && !(link.toString().contains(":"))) {
                if(url.startsWith("./"))
                {
                    continue;
                }
                //relative path
                if (url.startsWith("/wiki")) {
                    //add to q
                    //have reached limit?
                    if (n >= levels - 1) {
                        break;
                    }
                    else {
                        //trimming any bookmarks
                        if(url.contains("#"))
                        {
                            url = url.substring(0,url.indexOf("#"));
                        }
                        addLink(n, baseURL + url);
                    }
                }
                //absolutepath
                else if (url.startsWith("http://en.wikipedia.org/wiki/")) {
                    addLink(n, url);
                }
            }
        }
        return 1;
    }

    public static void Fcrawl() throws IOException, InterruptedException
    {
        int pagesvisited = 1;
        int valid =0;
        int n =0 ;
        Queue<String> crawlable = unvisitedLinks.get(n);
        while(true)
        {
            crawlable = unvisitedLinks.get(n);
            if(crawlable.isEmpty()){
                if(n>=levels-1)
                {
                    break;
                }
                n++;
                continue;
            }
            String qEntry = crawlable.remove();
            Document page = null;
            try {
                Thread.sleep(1000);
                page = Jsoup.connect(qEntry).timeout(0).get();


            }catch (Exception e)
            {
                //System.out.println(page);
                continue;
            }
            if(page == null)
            {
                continue;
            }
            visited.add(qEntry);
            pagesvisited++;

            //write to file also
           // writer.println(qEntry);

                if (!keyPhrase.isEmpty()) {
                    if (page.text().toLowerCase().contains(keyPhrase)) {
                        //page seems to be relevant so will crawl the links in it
                        valid = scanPage(n, page,qEntry);
                        if(valid==1){
                            writer.println(qEntry);
                        }
                        //processed all links in this page
                        //check to see if the crawlable in this level is empty, if yes goto next level
                        if (crawlable.isEmpty()) {
                            if (n >= levels - 1) {
                                break;
                            }
                            n++;
                        }
                        //if not continue onto next link in crawlable
                    } else {
                        //ignore
                        if (crawlable.isEmpty()) {
                            if (n >= levels - 1) {
                                break;
                            }
                            n++;
                        }
                        continue;
                    }
                } else {
                    //No key phrase so will have to crawl it anyway
                    valid = scanPage(n, page, qEntry);
                    if(valid==1){
                        writer.println(qEntry);
                    }
                    if (crawlable.isEmpty()) {
                        if (n >= levels - 1) {
                            break;
                        }
                        n++;
                    }
                }
            }
    }


    public static void main(String[] args) throws IOException, InterruptedException {

        writer = new PrintWriter("crawlerfinal.txt", "UTF-8");
        seed = args[0];
        if(args[1] !=null)
            keyPhrase = args[1];
        else
            keyPhrase = "".toLowerCase();


        //keyPhrase = "".toLowerCase();
        //seed = "http://en.wikipedia.org/wiki/Gerard_Salton";

        for (int i =0 ; i< levels;i++){
            unvisitedLinks.put(i, new LinkedList<String>());
        }

        Queue<String> temp = new LinkedList<String>();
        temp.add(seed);
        unvisitedLinks.put(0, temp);

        Fcrawl();
        writer.close();


    }
}
