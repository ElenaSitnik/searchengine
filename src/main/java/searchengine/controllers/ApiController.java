package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.intarfaces.IndexingService;
import searchengine.services.intarfaces.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public IndexingResponse indexing() {
        return indexingService.getStartIndexingResponse();
    }

    @GetMapping("/stopIndexing")
    public IndexingResponse stopIndexing() {
        return indexingService.getStopIndexingResponse();
    }

    @PostMapping("/indexPage")
    public IndexingResponse indexPage(@RequestBody String url) {
        return indexingService.getIndexingPageResponse(url);
    }

    //    @GetMapping("/startIndexing")
//    public ResponseEntity<IndexingResponse> indexing() {
//
//        return ResponseEntity.ok(indexingService.getIndexingResponse());
//    }

//    @GetMapping("/stopIndexing")
//    public ResponseEntity<IndexingResponse> stopIndexing() {
//        return ResponseEntity.ok(indexingService.getIndexingResponse());
//    }
}
