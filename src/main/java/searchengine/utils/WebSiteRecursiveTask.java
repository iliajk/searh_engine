package searchengine.utils;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.model.entities.Page;
import searchengine.model.entities.WebSite;
import searchengine.model.enums.Status;
import searchengine.model.repositories.WebSiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.RecursiveTask;

@Slf4j
public class WebSiteRecursiveTask extends RecursiveTask<HashSet<Page>> {
    private static WebSiteRepository webSiteRepository;
    public static String USERAGENT;
    public static String REFERRER;
    // all link which we found
    private static HashSet<String> totalLinksSet = new HashSet<>();
    // regular expression for web link https://www.subdomian.domain.domainzone/....
    private final String WEBLINKREG;
    // regular expression for situation when we get href from website and receive "/something"
    private final String SUBDOMAINLINK = "/([a-zA-Z\\d/_\\-]?+)+/?";
    // main website provided recursive task
    private WebSite webSite;
    private final String link;

    public WebSiteRecursiveTask(WebSite webSite, String link) {
        WEBLINKREG = "https://" + "([a-zA-Z0-9-_]+\\.)+?" + link.substring(8) + "[a-zA-Z0-9-_/]+";
        this.webSite = webSite;
        this.link = link;
    }

    @Override
    protected HashSet<Page> compute() {
        Document document;
        // pages which we found on current web-page
        HashSet<Page> localPages = new HashSet<>();
        // links which we found on current web-page
        HashSet<String> localLinkSet = new HashSet<>();
        // array of future fork of recursive tasks
        ArrayList<WebSiteRecursiveTask> arrayOfTasks = new ArrayList<>();

        //add some delay for passing security of some web-sites
        try {
            long milisec = 500L + (long) (4500 * Math.random());
            Thread.sleep(milisec);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        webSite.setStatusTime(LocalDateTime.now());
        webSite.setStatus(Status.INDEXING);
        webSite = webSiteRepository.save(webSite);
        // here we will get content of page, create page entity and add it in local pages
        try {
            document = Jsoup.connect(link)
                    .userAgent(USERAGENT)
                    .referrer(REFERRER)
                    .get();
            String content = document.outerHtml();
            Page currentPage = Page.builder()
                    .webSite(webSite)
                    .code(200)
                    .path(link)
                    .content(content)
                    .build();
            localPages.add(currentPage);
        } catch (IOException e) {
            document = null;
            String error = "Cannot get information through link: " + link + " error: " + e.getMessage();
            log.info(error);
            Page currentPage = Page.builder()
                    .webSite(webSite)
                    .code(((HttpStatusException) e).getStatusCode())
                    .path(link)
                    .content(e.getMessage())
                    .build();
            localPages.add(currentPage);
        }
        // search all href in document
        if (document != null) {
            Elements elements = document.select("a[href]");
            elements.forEach((e) -> {
                String str = e.attr("href");
                if (str.matches(WEBLINKREG)) {
                    if (!totalLinksSet.contains(str)) {
                        localLinkSet.add(str);
                        totalLinksSet.add(str);
                    }
                } else if (str.matches(SUBDOMAINLINK)) {
                    String siteUrl = webSite.getUrl();
                    str = str.charAt(0) == '/' && siteUrl.charAt(siteUrl.length() - 1) == '/' ?
                            webSite.getUrl() + str.substring(1) :
                            webSite.getUrl() + str;
                    if (!totalLinksSet.contains(str)) {
                        localLinkSet.add(str);
                        totalLinksSet.add(str);
                    }
                }
            });
            if (!localLinkSet.isEmpty()) {
                localLinkSet.forEach(link -> {
                    WebSiteRecursiveTask task = new WebSiteRecursiveTask(this.webSite, link);
                    task.fork();
                    arrayOfTasks.add(task);
                });
                arrayOfTasks.forEach((task) -> localPages.addAll(task.join()));
            }
        }
        return localPages;
    }

    public static void setWebSiteRepository(WebSiteRepository webSiteRepository) {
        WebSiteRecursiveTask.webSiteRepository = webSiteRepository;
    }

    public static void resetTotalLinkSet() {
        totalLinksSet = new HashSet<>();
    }
}
