package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.services.StatisticsService;

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
}
