package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.PagesCollectorConfig;
import searchengine.config.SiteConfig;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.parsing.ParsingResult;
import searchengine.dto.parsing.ReposUtilsParams;
import searchengine.model.*;
import searchengine.config.SitesList;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.LemmaUtils;
import searchengine.utils.PageParsingUtils;
import searchengine.utils.PagesCollector;
import searchengine.utils.TextUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final PagesCollectorConfig params;
    private final PageParsingUtils pageParsingUtils;
    private final TextUtils textUtils;
    private final SitesList sitesList;
    private final LemmaUtils lemmaUtils;
    private final LemmaRepository lemmaRepository;
    @Getter
    private boolean isStartedIndexing;

    @Override
    public IndexingResponse startIndexing() {
        if (isStartedIndexing) {
            return new IndexingResponse(false, "Индексация уже запущена");
        }
        List<SiteConfig> sitesConfig = sitesList.getSites();
        deleteSitesFromDB(sitesConfig);
//        initPagesCollector();
        isStartedIndexing = true;
        for (SiteConfig siteConfig : sitesConfig) {
            SiteEntity site = new SiteEntity(siteConfig.getName(),
                    pageParsingUtils.normalizeSiteUrl(siteConfig.getUrl()));
            siteRepository.save(site);
            parseSite(site);
        }
        return new IndexingResponse(true, "");
    }

    private void parseSite(SiteEntity site) {
        ForkJoinPool.commonPool().execute(() -> {
            try {
                pageParsingUtils.getHttpResponse(new URL(site.getUrl()));
                ReposUtilsParams parameters = new ReposUtilsParams(siteRepository, pageRepository,lemmaUtils,
                        pageParsingUtils, textUtils, params);
                switch (ForkJoinPool.commonPool().invoke(new PagesCollector(site, parameters))) {
                    case COMPLETED -> site.update(SiteStatus.INDEXED);
                    case INTERRUPTED -> site.update("Индексация остановлена пользователем");
                }
            } catch (IOException e) {
                site.update("Ошибка индексации. Сайт недоступен.");
            } finally {
                siteRepository.save(site);
                if (isStartedIndexing && isAllSitesParsingEnded()) {
                    isStartedIndexing = false;
                }
            }
        });
    }

    private boolean isAllSitesParsingEnded() {
        return siteRepository.findAll().stream()
                .allMatch(Predicate.not(
                        i -> i.getStatus().equals(SiteStatus.INDEXING)));
    }

    private void deleteSitesFromDB(List<SiteConfig> sites) {
        String[] siteNames = sites.stream()
                .map(SiteConfig::getName)
                .toArray(String[]::new);
        siteRepository.deleteByNameIn(siteNames);

    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!isStartedIndexing) {
            return new IndexingResponse(false, "Индексация не запущена");
        }
        isStartedIndexing = false;
        PagesCollector.stopParsing();
        return new IndexingResponse(true, "");
    }

    @Override
    public IndexingResponse indexPage(String url) {
        try {
            SiteEntity siteEntity;
            String relativePath = pageParsingUtils.getRelativePath(url);
            Optional<SiteConfig> optionalSiteConfigForSpecifiedUrl = searchSiteInConfigFile(url);
            if (optionalSiteConfigForSpecifiedUrl.isEmpty()) {
                return new IndexingResponse(false,
                        "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            }
            SiteConfig siteConfigForSpecifiedUrl = optionalSiteConfigForSpecifiedUrl.get();
            siteEntity = findOrAddSite(siteConfigForSpecifiedUrl);
            ParsingResult response = pageParsingUtils.getHttpResponse(new URL(url));
            if (response.statusCode() >= 400) {
                return new IndexingResponse(false,
                        "Страница не может быть проиндексирована. Ошибка: " + response.statusCode());
            }
            deletePageIfExists(relativePath, siteEntity);
            PageEntity page = new PageEntity(siteEntity, relativePath,
                    response.statusCode(), response.document().html());
            pageRepository.save(page);
            lemmaUtils.addLemmasAndIndexesToDB(siteEntity, page);
            siteEntity.update(SiteStatus.INDEXED);
            siteRepository.save(siteEntity);
            return new IndexingResponse(true, "");
        } catch (IOException e) {
            return new IndexingResponse(false, "Ошибка индексации. Страница недоступна.");
        }
    }

    private Optional<SiteConfig> searchSiteInConfigFile(String url) {
        return sitesList.getSites().stream()
                .filter(site -> url.startsWith(site.getUrl()))
                .findAny();
    }

    private SiteEntity findOrAddSite(SiteConfig siteConfig) {
        SiteEntity siteForSpecifiedUrl;
        Optional<SiteEntity> optionalSite = siteRepository.findByUrlLike(siteConfig.getUrl());
        if (optionalSite.isEmpty()) {
            siteForSpecifiedUrl = new SiteEntity(siteConfig.getName(), siteConfig.getUrl());
        } else {
            siteForSpecifiedUrl = optionalSite.get();
            siteForSpecifiedUrl.update(SiteStatus.INDEXING);
        }
        siteRepository.save(siteForSpecifiedUrl);
        return siteForSpecifiedUrl;
    }

    private void deletePageIfExists(String path, SiteEntity site) {
        Optional<PageEntity> optionalPage = pageRepository.findByPathAndSite(path, site);
        optionalPage.ifPresent(page -> {
            List<LemmaEntity> lemmas = lemmaRepository.findAllByPage(page);
            lemmaRepository.decrementFrequencyOrDelete(lemmas);
            pageRepository.delete(page);
        });
    }
}
