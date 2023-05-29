package searchengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "parsing-settings")
@Data
public class PagesCollectorConfig {
    private String jsoupReferer;
    private String jsoupUserAgent;
    private int delay;
    private Boolean redirect;
}
