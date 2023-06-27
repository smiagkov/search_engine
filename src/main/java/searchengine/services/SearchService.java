package searchengine.services;

import searchengine.dto.search.Query;
import searchengine.dto.search.SearchResponse;

public interface SearchService {
     SearchResponse getQueryResponse(Query query);
}
