package searchengine.service;

import org.junit.jupiter.api.Test;
import searchengine.services.StatisticsServiceImpl;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class StatisticsServiceImplTest {
    @Test
    void testExtractDomainWithHttp() {
        List<String > urls = new ArrayList<>();
        urls.add("https://subdomain.domain.zone/link/link");
        urls.add("http://subdomain.domain.zone/link/link");
        urls.add("https://domain.zone/link/link");
        urls.add("http://domain.zone/link/link");
        urls.add("https://domain.zone/?dasdasdasfaf/");
        urls.add("http://domain.zone/?dasdasdasfaf/");
        urls.add("http://domain.zone/zone/");
        urls.add("http://domain.zone/zone");
        urls.add("domain.zone/?dasdasdasfaf/");
        urls.add("domain.zone");
        urls.add("subdomain.domain.zone");
        urls.forEach(url -> assertEquals("domain.zone", StatisticsServiceImpl.extractDomain(url)));
    }
}
