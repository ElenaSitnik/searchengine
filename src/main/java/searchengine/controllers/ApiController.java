package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.intarfaces.IndexingService;
import searchengine.services.intarfaces.SearchService;
import searchengine.services.intarfaces.StatisticsService;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
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

    @GetMapping("/search")
    public SearchResponse searchInformation(HttpServletRequest request) {
        return searchService.getSearchResponse(request);
    }

}
