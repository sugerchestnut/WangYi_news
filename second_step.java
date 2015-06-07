package information;

/**
 * Created by pl on 15-6-3.
 */
import com.thoughtworks.xstream.XStream;
import com.mysql.jdbc.NotUpdatable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.lang.NullPointerException;
import java.io.*;
import java.security.interfaces.ECKey;
import java.util.*;

class news{

    public String title;   //新闻标题
    public String url;     //新闻的链接
    public String date;    //新闻时间
    public String media;   //来源媒体
    public String content;     //正文
    public List<String> img; //新闻中出现的图片的链接
}

class Getnews extends Thread{
    private List<news> newsList;      //保存新闻对象的容器
    private List<String> newsLinks;   //存储新闻的链接,多个页面的新闻都存储在这里
    private int number;      //多线程根据这个处理各篇文章

    public Getnews(){                      //构造函数
        newsList = new LinkedList<news>();
        number = -1;
    }

    public List<news> GetnewsList(){      //获取新闻内容，返回的是一个容器里面是news的一个个对象
        Document doc = null;
        try {
            doc = Jsoup.connect("http://news.163.com/domestic/").get();
            String title = doc.title();           //获取的是国内新闻首页的标题
        }catch (IOException e){
            e.printStackTrace();
        }catch (NullPointerException e){
            e.printStackTrace();
        }

        List<String> pagelinks = new LinkedList();       //获得的页面的链接保存在LinkedList容器中，也就是首页最底下的翻页的数字
        Element pagelinkselement = doc.getElementsByClass("list-page").first();  //查找元素
        Elements elemA = pagelinkselement.select("a");             //选择页面链接的html源代码中a元素

        int flag = -1;
        for (Element t : elemA) {
            //一共添加8个页面的链接
            if (flag == -1){             //第一个页面的链接不可用,输出结果是'#'
                flag++;
                continue;
            }
            pagelinks.add(t.attr("href"));     //将获得到的每个页面的链接放入到容器中，href属性下面保存的是链接

            if (flag == 7){
                break;
            }
        }

        //每个页面下面有好多的文章，需要获取到每篇文章的链接
        newsLinks = new LinkedList<String>();       //获取每个页面中文章的链接
        Document pagedoc = null;
        for (int i = 0; i < pagelinks.size(); i++){
            try {
                pagedoc = Jsoup.connect(pagelinks.get(i)).get();
            } catch(IOException e){
                e.printStackTrace();
            }

            Elements divs = pagedoc.getElementsByClass("item-top");   //从每一个页面中获得,每个页面中有许多文章,item-top代表的是文章内容所属的类
            int pagecount = 1;
            for (Element t : divs){

                Element a = t.select("a").first();  //查找第一个a元素
                newsLinks.add(a.attr("href"));       //把每篇文章的链接添加进去 ，item-top下面的a元素的href属性里面保存的是文章的链接

            }
        }

        for (int t = 0; t < newsLinks.size(); t++ ){
            Article m = new Article();
            m.start();
            try {
                m.join();
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        return newsList;
    }

    class Article extends Thread{         //使用多线程,每个线程解决一篇文章
        public void run(){             //多线程的方法重写
            number++;
            news singalNews = new news();
            String articleLink = newsLinks.get(number);     //获取文章链接
            Document articleDoc = null;

            singalNews.url = articleLink;
            try {
                articleDoc = Jsoup.connect(articleLink).get();   //链接到每篇文章
            }catch (IOException e){
                e.printStackTrace();
            }catch (NullPointerException e){
                e.printStackTrace();
            }

            String title = null;            //获取新闻的标题
            try {
                title = articleDoc.title();
            }catch (NullPointerException e){
                e.printStackTrace();
            }
            singalNews.title = title;

            String time =null;           //文章的时间
            Elements pageall = null;      //文章的正文保存在这里
                try {

                    Element articlewenzhang = null;
                    if (articleLink.startsWith("http://news") || articleLink.startsWith("http://wars")){
                        articlewenzhang = articleDoc.getElementById("endText");      //获取文章内容
                        time = articleDoc.getElementsByClass("ep-time-soure").first().text();   //获取时间
                        singalNews.date = time.substring(0,19);
                        singalNews.media = time.substring(20);//文章的来源

                    }else if(articleLink.startsWith("http://view")){
                        articlewenzhang = articleDoc.getElementsByClass("feed-text").first();
                        time = articleDoc.getElementById("ptime").text();
                        singalNews.date = time.substring(0,19);
                        singalNews.media = null;
                    }else if (articleLink.startsWith("http://focus.news")){
                        articlewenzhang = articleDoc.getElementById("endText");
                        singalNews.date = null;
                        singalNews.media = null;
                    } else {
                        System.out.println("其他类型文章");
                    }
                    pageall = articlewenzhang.select("p");   //文章正文

                }catch (NullPointerException e){
                    e.printStackTrace();
                }

            StringBuffer content = new StringBuffer();
            try {
                for (Element t : pageall) {
                    content.append(t.text());    //t.text()里面保存的是文章正文的内容
                }
            }catch (NullPointerException e){
                e.printStackTrace();
            }

            singalNews.content = content.toString();   //正文

            List<String> imgAddr = new LinkedList<String>();
            Elements jpgall = articleDoc.getElementsByClass("f_center");
            for (Element t:jpgall){
                Element img = t.select("img").first();
                try {
                    imgAddr.add(img.attr("src"));
                }catch (NullPointerException e){
                    e.printStackTrace();
                }

            }
            singalNews.img = imgAddr;
            newsList.add(singalNews);       //将新的news添加进newsList中
        }
    }
}

class Writefiles extends Thread{
    private news onenews;   //一次只写一篇文章,news代表一篇文章的相关知识

    public Writefiles(news onenews){      //构造函数
        this.onenews = onenews;
    }

    public void run(){
        XStream Stream = new XStream();
        String xmlfile = Stream.toXML(onenews);    //将onenews中的内容转换成xml文见的形式

        System.out.println(xmlfile);
        File dir = new File("/home/pl/newsget/");
        if (!dir.exists()){   //若该目录不存在，则创建目录
            dir.mkdir();
        }

        File file = new File(dir,onenews.title);   //在ｄｉｒ目录下创建文件，保存文章,文章题目做文件名
        try {
            if (!file.exists()){
                file.createNewFile();
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        FileOutputStream fw = null;
        OutputStreamWriter ow = null;
        try {
            fw = new FileOutputStream(file);
            ow = new OutputStreamWriter(fw);
            ow.write(new String(xmlfile.getBytes(), ow.getEncoding()));
            ow.close();
        }catch (IOException e){
            e.printStackTrace();
        }catch (NullPointerException e){
            e.printStackTrace();
        }

    }
}

public class NewsInformation2 {
    public static void main(String[] args) {

        List<news> newsList = new Getnews().GetnewsList();
        for (news t:newsList){
            Writefiles m = new Writefiles(t);
            m.start();
        }
    }
}
