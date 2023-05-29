package searchengine.dto.statistics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class DetailedStatisticsItem {
    private String url;
    private String name;
    private String status;
    private long statusTime;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String error;
    private int pages;
    private int lemmas;
}
