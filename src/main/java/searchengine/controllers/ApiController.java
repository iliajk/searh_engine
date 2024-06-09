package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.services.StatisticsService;
import searchengine.utils.Lemmatization;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;

    public ApiController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> statistics() {
        return statisticsService.getStatistics();
    }

    @GetMapping("/api/startIndexing")
    public ResponseEntity<?> startIndexing() {
        return statisticsService.startIndexing();
    }

    @GetMapping("/api/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        return statisticsService.stopIndexing();
    }

    @PostMapping("/api/IndexPage")
    public ResponseEntity<?> indexPage(@RequestParam String url) {
        return statisticsService.indexPage(url);
    }

    @GetMapping("/api/lemma")
    public ResponseEntity<?> lemma() {
        return ResponseEntity.ok(Lemmatization.countLemma("Повторное появление леопарда в Осетии позволяет предположить,\n" +
                "что леопард постоянно обитает в некоторых районах Северного\n" +
                "Кавказа.\n"));
    }
}
