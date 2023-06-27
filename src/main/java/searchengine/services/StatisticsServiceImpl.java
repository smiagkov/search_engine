package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.model.SiteStatus;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    public static final String INDEXING_NOT_YET_STARTED = "Индексация ещё не запущена.";
    private final IndexingService indexingService;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final PageParsingUtils pageParsingUtils;
    private final SitesList sites;

    @Override
    public StatisticsResponse getStatistics() {
        String[] errors = {
                "Ошибка индексации: главная страница сайта не доступна",
                "Ошибка индексации: сайт не доступен",
                ""
        };
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(indexingService.isStartedIndexing());
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<SiteConfig> sitesList = sites.getSites();
        for (SiteConfig site : sitesList) {
            DetailedStatisticsItem item = getDetailedStatistics(site);
            total.setPages(total.getPages() + item.getPages());
            total.setLemmas(total.getLemmas() + item.getLemmas());
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    private DetailedStatisticsItem getDetailedStatistics (SiteConfig site) {
        DetailedStatisticsItem item = new DetailedStatisticsItem();
        item.setName(site.getName());
        item.setUrl(site.getUrl());
        String normalizedUrl = pageParsingUtils.normalizeSiteUrl(site.getUrl());
        int pages = 0;
        int lemmas = 0;
        SiteStatus siteStatus;
        String lastError;
        LocalDateTime statusTime;
        try {
            SiteEntity siteEntity = siteRepository.findByUrl(normalizedUrl).orElseThrow();
            pages = pageRepository.countBySite(siteEntity);
            lemmas = lemmaRepository.countBySite(siteEntity);
            siteStatus = siteEntity.getStatus();
            lastError = siteEntity.getLastError();
            statusTime = siteEntity.getStatusTime();
        } catch(NoSuchElementException e) {
            siteStatus = SiteStatus.FAILED;
            lastError = INDEXING_NOT_YET_STARTED;
            statusTime = LocalDateTime.now();
        }
        item.setPages(pages);
        item.setLemmas(lemmas);
        item.setStatus(siteStatus.toString());
        item.setError(lastError);
        item.setStatusTime(statusTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        return item;
    }
}
