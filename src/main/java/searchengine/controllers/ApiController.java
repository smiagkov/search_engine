package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.Query;
import searchengine.dto.search.SearchErrorResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        IndexingResponse answer = indexingService.startIndexing();
        HttpStatus status = answer.result() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(answer);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        IndexingResponse response = indexingService.stopIndexing();
        HttpStatus status = response.result() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestParam(name = "url") String url) {
        HttpStatus statusCode;
        IndexingResponse response;
        try {
            response = indexingService.indexPage(url);
            statusCode = response.result() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        } catch (Exception e) {
            statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
            response = new IndexingResponse(false, e.getMessage());
        }
        return ResponseEntity.status(statusCode).body(response);
    }

    @GetMapping(value = "/search")
    public ResponseEntity<SearchResponse> search(@RequestParam("query") String query,
                                                 @RequestParam("offset") int offset,
                                                 @RequestParam("limit") int limit,
                                                 @RequestParam(value = "site", required = false) String site) {
        SearchResponse response = searchService.getQueryResponse(new Query(query, offset, limit, site));
        if(!response.getResult()) {
            SearchErrorResponse errorResponse = (SearchErrorResponse) response;
            return ResponseEntity.status(errorResponse.httpStatus()).body(errorResponse);
        }
        return ResponseEntity.ok().body(response);
    }
}
