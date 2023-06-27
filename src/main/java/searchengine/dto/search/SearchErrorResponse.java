package searchengine.dto.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpStatus;

@JsonIgnoreProperties("httpStatus")
public record SearchErrorResponse(boolean result, HttpStatus httpStatus, String error) implements SearchResponse{
    @Override
    public boolean getResult() {
        return result;
    }
}
