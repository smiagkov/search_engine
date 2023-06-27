package searchengine.dto.search;

import java.util.List;

public record SearchSuccessfulResponse(boolean result, int count, List<SearchResult> data)
        implements SearchResponse {

    @Override
    public boolean getResult() {
        return result;
    }
}
