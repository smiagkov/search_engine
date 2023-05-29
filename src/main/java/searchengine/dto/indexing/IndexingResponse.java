package searchengine.dto.indexing;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record IndexingResponse(boolean result, String error) {
}
