package searchengine.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import searchengine.config.Config;
import searchengine.config.Site;
import searchengine.config.IndexingSettings;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.entities.Page;
import searchengine.model.entities.WebSite;
import searchengine.model.enums.Status;
import searchengine.model.repositories.IndexRepository;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.WebSiteRepository;
import searchengine.utils.WebSiteRecursiveTask;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@EnableTransactionManagement
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {
    @Value("${jsoup.useragent}")
    private String USERAGENT;
    @Value("${jsoup.referrer}")
    private String REFERRER;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final WebSiteRepository webSiteRepository;
    private final IndexRepository indexRepository;
    private final IndexingSettings sites;
    private final int processors = Runtime.getRuntime().availableProcessors();
    private ForkJoinPool fjp = new ForkJoinPool(processors);
    private static volatile Boolean indexingInProgress = false;
    private final static String DOMAINREGX = "([a-zA-Z0-9-]{1,123}\\.[a-zA-Z]{2,63})$";
    private final static String LINKREGX = "(https?://)?(www\\.)?([a-zA-Z1-9]{1,63}\\.){1,123}[a-zA-Z1-9]{2,8}/?.+";
    private final static String HTTP = "(http://).+";
    private final static String HTTPS = "(https://).+";
    //private static volatile Boolean updatingStatusTime = true;

    @Override
    public ResponseEntity<?> getStatistics() {
        List<WebSite> dbListWebSites = webSiteRepository.findAll();
        if(dbListWebSites.isEmpty()){
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("WebSites not indexed, please complete indexing first");
        }
        TotalStatistics total = new TotalStatistics();
        total.setSites(dbListWebSites.size());
        total.setIndexing(indexingInProgress);
        List<DetailedStatisticsItem> detailedStatistics = new ArrayList<>();

        for (WebSite site : dbListWebSites) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            int pages = pageRepository.countAllByWebSiteName(site.getName());
            int lemmas = lemmaRepository.countAllByWebSiteName(site.getName());
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(site.getStatus());
            item.setError(site.getLast_error());
            item.setStatusTime(site.getStatusTime());
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailedStatistics.add(item);
        }

        StatisticsData data = StatisticsData.builder()
                .detailed(detailedStatistics)
                .total(total)
                .build();
        StatisticsResponse response = StatisticsResponse.builder()
                .statistics(data)
                .result(true)
                .build();

        return ResponseEntity.ok().body(response);
    }

    @Override
    public ResponseEntity<?> startIndexing() {
        if (indexingInProgress) {
            return ResponseEntity.badRequest().body(Map.of("result", "false", "error", "Indexing already started"));
        }
        if (fjp.isShutdown()) {
            fjp = new ForkJoinPool(processors);
        }
        List<WebSite> webSiteList = new ArrayList<>();
        readConfig();
        indexingInProgress = true;
        // removing all data about sites (site records. pages, lemmas, indexes)
        indexRepository.removeAll();
        lemmaRepository.removeAll();
        pageRepository.removeAll();
        webSiteRepository.removeAll();
        for (Site site : sites.getSites()) {
            String url = site.getUrl();
            String name = site.getName();
            // creating in table Site new record with status Indexing
            WebSite webSite = WebSite.builder()
                    .statusTime(LocalDateTime.now())
                    .last_error(null)
                    .status(null)
                    .url(url)
                    .name(name)
                    .build();
            webSite = webSiteRepository.save(webSite);
            webSiteList.add(webSite);
        }
        // we are going through all pages of site List
        WebSiteRecursiveTask.USERAGENT = USERAGENT;
        WebSiteRecursiveTask.REFERRER = REFERRER;
        Set<Page> totalPages;

        for (WebSite webSite : webSiteList) {
            try {
                WebSiteRecursiveTask webSiteRecursiveTask = new WebSiteRecursiveTask(webSite, webSite.getUrl());
                WebSiteRecursiveTask.setWebSiteRepository(webSiteRepository);
                fjp.execute(webSiteRecursiveTask);
                totalPages = new HashSet<>(webSiteRecursiveTask.join());
                totalPages.forEach(page -> {
                    if (page.getCode() != 200) {
                        page.getWebSite().setLast_error(page.getContent());
                        webSiteRepository.save(page.getWebSite());
                    }
                });
                webSite.setStatus(Status.INDEXED);
                webSiteRepository.save(webSite);
                List<Page> repoPages = pageRepository.saveAll(totalPages);
                repoPages.forEach(MorphologyService::processPage);
            } catch (Throwable e) {
                log.error(Arrays.toString(e.getStackTrace()));
                webSite.setStatus(Status.FAILED);
                webSite.setLast_error(e.getMessage());
                webSiteRepository.save(webSite);
            }
        }

        WebSiteRecursiveTask.resetTotalLinkSet();
        indexingInProgress = false;

        log.info("Indexing finished successfully");
        return ResponseEntity.ok(true);
    }

    @Override
    public ResponseEntity<?> stopIndexing() {
        if (!indexingInProgress) {
            return ResponseEntity.badRequest().body(Map.of("result", "false", "error", "Indexing is not running"));
        }
        indexingInProgress = false;
        fjp.shutdown();
        try {
            // waiting until it will be finished
            if (!fjp.awaitTermination(60, TimeUnit.SECONDS)) {
                fjp.shutdownNow();
            }
        } catch (InterruptedException e) {
            String error = "Problem with stopping ForkJoinPool, please check error: " + e.getMessage();
            log.error(error);
            return ResponseEntity.badRequest().body(Map.of("result", "false", "error", error));
        }
        WebSiteRecursiveTask.resetTotalLinkSet();
        return ResponseEntity.ok(true);

    }

    @Override
    public ResponseEntity<?> indexPage(String url) {
        boolean webSiteListContainsPage = false;
        String domain;
        if (indexingInProgress) {
            return ResponseEntity.badRequest().body(Map.of(
                    "result", "false",
                    "error", "Indexing already started"));
        }
        if (!url.matches(LINKREGX)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "result", "false",
                    "error", "Wrong link type, it should looks like http(s)://www.subdomain.domain.zone/abc..."));
        }
        domain = extractDomain(url);
        readConfig();
        List<WebSite> webSiteList = webSiteRepository.findAll();
        WebSite currentWebSite = null;
        // is page links contain url domain
        for (WebSite webSite : webSiteList) {
            if (webSite.getUrl().contains(domain)) {
                webSiteListContainsPage = true;
                currentWebSite = webSite;
                break;
            }
        }

        // start indexing page
        if (webSiteListContainsPage) {
            indexingInProgress = true;
            Page currentPage;
            try {
                if (!url.matches(HTTP) && !url.matches(HTTPS)) {
                    url = "https://" + url;
                }
                Document document = Jsoup.connect(url)
                        .userAgent(USERAGENT)
                        .referrer(REFERRER)
                        .get();
                String content = document.outerHtml();
                currentPage = Page.builder()
                        .webSite(currentWebSite)
                        .code(200)
                        .path(url)
                        .content(content)
                        .build();
            } catch (IOException e) {
                String error = "Cannot get information through link: " + url + " error: " + e.getMessage();
                log.info(error);
                currentPage = Page.builder()
                        .webSite(currentWebSite)
                        .code(((HttpStatusException) e).getStatusCode())
                        .path(url)
                        .content(e.getMessage())
                        .build();
            }
            currentPage = pageRepository.save(currentPage);
            MorphologyService.processPage(currentPage);
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "result", "false",
                    "error", "This Url is located out of bound web sites, specified in configuration file"));
        }
        indexingInProgress = false;
        return ResponseEntity.ok(Map.of("result", true));
    }

    // extracts domain and zone from any link
    private static String extractDomain(String link) {
        String domain = link.replaceAll("\\?", "");
        try {
            if (!domain.matches(HTTP) && !domain.matches(HTTPS)) {
                domain = "https://" + domain;
            }
            URL url = new URL(domain);
            String host = url.getHost();
            Pattern pattern = Pattern.compile(DOMAINREGX);
            Matcher matcher = pattern.matcher(host);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return "Domain not found";
        } catch (MalformedURLException e) {
            log.error(Arrays.toString(e.getStackTrace()));
            return "";
        }
    }


    private void readConfig() {
        log.debug("Start to indexing web sites");
        sites.setSites(new ArrayList<>());
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            log.debug("Start reading config file");
            Config config = mapper.readValue(new File("src/main/resources/config.yaml"), Config.class);
            if (config != null) {
                for (Site site : config.getIndexingSettings().getSites()) {
                    sites.getSites().add(site);
                }
                log.debug("Config file read successfully");
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Problem with reading config file. " + Arrays.toString(e.getStackTrace()));
        }
    }
}
