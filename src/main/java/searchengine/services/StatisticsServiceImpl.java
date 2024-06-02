package searchengine.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@EnableTransactionManagement
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {
    @Value("${jsoup.useragent}")
    private String USERAGENT;
    @Value("${jsoup.referrer}")
    private String REFERRER;
    private static final Long UPDATE_INDEXING_TIME = 1000L;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final WebSiteRepository webSiteRepository;
    private final Random random = new Random();
    private final IndexingSettings sites;
    private final int processors = Runtime.getRuntime().availableProcessors();
    private ForkJoinPool fjp = new ForkJoinPool(processors);
    private static volatile Boolean indexingInProgress = false;
    //private static volatile Boolean updatingStatusTime = true;

    @Override
    public ResponseEntity<?> getStatistics() {
        String[] statuses = {"INDEXED", "FAILED", "INDEXING"};
        String[] errors = {
                "Ошибка индексации: главная страница сайта не доступна",
                "Ошибка индексации: сайт не доступен",
                ""
        };

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for (int i = 0; i < sitesList.size(); i++) {
            Site site = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            int pages = random.nextInt(1_000);
            int lemmas = pages * random.nextInt(1_000);
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(statuses[i % 3]);
            item.setError(errors[i % 3]);
            item.setStatusTime(System.currentTimeMillis() -
                    (random.nextInt(10_000)));
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
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
        for (Site site : sites.getSites()) {
            // removing all data about site (records from site and page tables)
            String url = site.getUrl();
            String name = site.getName();
            pageRepository.removeAllPagesOfSite(url, name);
            webSiteRepository.removeSiteInfo(url, name);
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
        Set<Page> totalPages = new HashSet<>();

        for (WebSite webSite : webSiteList) {
            try {
                WebSiteRecursiveTask webSiteRecursiveTask = new WebSiteRecursiveTask(webSite, webSite.getUrl());
                WebSiteRecursiveTask.setWebSiteRepository(webSiteRepository);
                fjp.execute(webSiteRecursiveTask);
                totalPages.addAll(webSiteRecursiveTask.join());
                webSite.setStatus(Status.INDEXED);
                webSiteRepository.save(webSite);
            } catch (Throwable e) {
                log.error(e.getMessage());
                webSite.setStatus(Status.FAILED);
                webSite.setLast_error(e.getMessage());
                webSiteRepository.save(webSite);
            }
        }
        totalPages.forEach(page -> {
            if (page.getCode() != 200) {
                page.getWebSite().setLast_error(page.getContent());
            }
        });

        pageRepository.saveAll(totalPages);
        indexingInProgress = false;
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
            log.error("Problem with reading config file. " + e.getMessage());
        }
    }
}
