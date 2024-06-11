package searchengine.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import searchengine.config.IndexingSettings;
import searchengine.model.repositories.IndexRepository;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.WebSiteRepository;
import searchengine.services.MorphologyService;
import searchengine.services.StatisticsServiceImpl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class StatisticsServiceImplTest {
    @Mock
    private IndexRepository indexRepository;

    @Mock
    private LemmaRepository lemmaRepository;

    @Mock
    private PageRepository pageRepository;

    @Mock
    private WebSiteRepository webSiteRepository;

    @Mock
    private MorphologyService morphologyService;

    @Mock
    private IndexingSettings indexingSettings;

    @InjectMocks
    private StatisticsServiceImpl statisticsServiceImpl;

    @BeforeEach
    void setUp() {

    }

    @Test
    void testExtractDomainWithHttp() throws Exception {
        List<String> urls = new ArrayList<>();
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
        Class<?> reflection = StatisticsServiceImpl.class;
        Method method = reflection.getDeclaredMethod("extractDomain", String.class);
        method.setAccessible(true);
        urls.forEach(url -> {
            try {
                assertEquals("domain.zone", method.invoke(null, url));
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        });
    }
}
