import com.sun.org.apache.xpath.internal.SourceTree;
import javafx.scene.effect.SepiaTone;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.helper.Validate;

import javax.print.Doc;
import java.io.*;
import java.net.SocketTimeoutException;
import java.util.*;


/**
 * Created by pl on 15-5-25.
 */
public class news_Information {

    public static void main(String[] argc) {
        Document doc = null;                //解析获得一个Document实例对象
        try {
            doc = Jsoup.connect("http://news.163.com/domestic/").get();     //获取一个html文件并且开始解析
            String title = doc.title();                    //获取国内新闻页面的标题
            //System.out.println(title);
        }
        catch(IOException e) {
            e.printStackTrace();
        }catch (NullPointerException e){
            e.printStackTrace();
        }

        /*Element content = doc.getElementsByClass("list-page").first();
        Elements links = doc.select("a[href]");
        Elements imports = doc.select("link[href]");
        for (Element link : imports){
            System.out.print(link.tagName());
            System.out.print(link.attr("abs:href"));
            System.out.println( link.attr("rel"));
        }

        for (Element link : links){
            System.out.println(link.attr("abs:href"));
            //System.out.println(trim(link.text()));
        }*/
        List<String> pagelinks = new LinkedList();    //获得的页面的链接保存在LinkedList容器中

        //Element pageleft = doc.getElementsByClass("area-left").first();
        //Elements pagelinkselement = pageleft.select("list-page");
        //Elements elemA = pageleft.select("a");//选择a元素

        Element pagelinkselement = doc.getElementsByClass("list-page").first();//查找元素
        Elements elemA = pagelinkselement.select("a");//选择a元素

        int flag = -1;
        for (Element t : elemA) {
            //一共添加8个页面的链接
            if (flag == -1){        //第一个页面的链接不可用,输出结果是#
                flag++;
                continue;
            }
            pagelinks.add(t.attr("href"));     //将获得到的每个页面的链接放入到容器中
            //System.out.println(t.attr("href"));

            if (flag == 5){
                break;
            }
        }

        Set<String> articleLinks = new HashSet<String>();        //获取每个页面中文章的链接
        Document doc1 = null;
        for (int i = 0; i < pagelinks.size(); i++){
            try {
                doc1 = Jsoup.connect(pagelinks.get(i)).get();
            } catch(IOException e){
                    e.printStackTrace();
            }

            Elements divs = doc1.getElementsByClass("item-top");   //从每一个页面中获得,每个页面中有许多文章
            //Elements divs = doc1.getElementsByClass("list-item");   //从每一个页面中获得,每个页面中有许多文章
            int pagecount = 1;
            for (Element t : divs){
                //System.out.println("第"+pagecount+"个页面: ");
                Element a = t.select("a").first();  //查找第一个a元素
                articleLinks.add(a.attr("href"));       //添加进去的每篇文章的链接
                //System.out.println(a.attr("href"));      //文章里面包括的既有news还有评论
                //pagecount++;
            }
        }

        for (String links : articleLinks){
            Document articledoc = null;
            try {
                articledoc = Jsoup.connect(links).get();//首先链接到每篇文章上，然后从文章上获取title
            }catch (IOException e){
                e.printStackTrace();
            }catch (NullPointerException e){
                e.printStackTrace();
            }

            String title = null;
            try {
                title = articledoc.title();     //获取文章标题
                //System.out.println(title);
            }catch (NullPointerException e){
                e.printStackTrace();
            }

            List<String> pngAddr = new ArrayList<String>();
            Elements picture = null;
            //picture = articledoc.getElementsByClass("ep-content");
            picture = articledoc.getElementsByClass("f_center");
            for (Element t : picture){
                Element a = t.select("img").first();
                try {
                    pngAddr.add(a.attr("src"));
                }catch (NullPointerException e){
                    e.printStackTrace();
                }
                //System.out.println(a.attr("src"));
            }

            String time = null;
            try {
                Elements all = null;
                try {
                    Element wenzhang = null;
                    if (links.startsWith("http://news")){               //网易新闻
                        wenzhang = articledoc.getElementById("endText");
                        time = articledoc.getElementsByClass("ep-time-soure").first().text();
                    }else if (links.startsWith("http://view")){         //网易评论
                        wenzhang = articledoc.getElementsByClass("feed-text").first();
                        time = articledoc.getElementById("ptime").text();
                    }else {
                        System.out.println("即不是新闻也不是评论: "+links);
                        continue;
                    }
                    all = wenzhang.select("p");         //文章的正文
                }catch (NullPointerException e){
                    e.printStackTrace();
                }


            File path = new File("/home/pl/article");//首先在该目录下创建一个File对象，即一个文件
            if (!path.exists())
                path.mkdir();                    //若该目录不存在，则重新创建
            File file = new File(path,time);       //path目录下可创建多个文件对象，时间戳做文件名
            try {
                if (!file.exists())
                    file.createNewFile();     //若该文件不存在，则创建该文件
            }catch (IOException e){
                    e.printStackTrace();
            }
            try {
                int flagline = 0;
                BufferedWriter in = new BufferedWriter(new FileWriter(file));
                in.write("标题: " + title);
                in.newLine();
                in.write("时间戳: " + time);
                in.newLine();

                for (Element t : all){
                    in.write(t.text());  //将文章内容写入到新创建的文件中去
                }
                in.newLine();

                in.write("文章地址: " + links);
                in.newLine();

                for (String st: pngAddr){    //一片文章或许有多个图片
                    in.write("图片: " + st);
                }
                in.newLine();

                in.close();
            }catch (IOException e){
                    e.printStackTrace();
            }
            }catch (NullPointerException e){
                e.printStackTrace();
            }
        }
    }
}

