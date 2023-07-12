package searchengine.dto.parsing;

import searchengine.config.PagesCollectorConfig;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.LemmaUtils;
import searchengine.utils.PageParsingUtils;
import searchengine.utils.TextUtils;

public record ReposUtilsParams(SiteRepository siteRepository, PageRepository pageRepository, LemmaUtils lemmaUtils,
                               PageParsingUtils pageParsingUtils, TextUtils textUtils, PagesCollectorConfig collectorConfig) {
}
