package searchengine.services;

import org.springframework.http.ResponseEntity;

public interface StatisticsService {
    ResponseEntity<?> getStatistics();

    ResponseEntity<?> startIndexing();

    ResponseEntity<?> stopIndexing();
}
