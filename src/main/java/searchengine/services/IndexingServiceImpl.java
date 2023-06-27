package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.springframework.stereotype.Service;
import searchengine.config.PagesCollectorConfig;
import searchengine.config.SiteConfig;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.*;
import searchengine.config.SitesList;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.LemmaUtils;
import searchengine.utils.PageParsingUtils;
import searchengine.utils.TextUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final PagesCollectorConfig params;
    private final PageParsingUtils pageParsingUtils;
    private final TextUtils textUtils;
    private final SitesList sitesList;
    private final LemmaUtils lemmaUtils;
    private final LemmaRepository lemmaRepository;
    @Getter
    private boolean isStartedIndexing;
    private final Object lock = new Object();

    public IndexingResponse startIndexing() {
        if (isStartedIndexing) {
            return new IndexingResponse(false, "Индексация уже запущена");
        }
        List<SiteConfig> sitesConfig = sitesList.getSites();
        deleteSitesFromDB(sitesConfig);
        initPagesCollector();
        isStartedIndexing = true;
        for (SiteConfig siteConfig : sitesConfig) {
            SiteEntity site = new SiteEntity(siteConfig.getName(),
                    pageParsingUtils.normalizeSiteUrl(siteConfig.getUrl()));
            siteRepository.save(site);
            parseSite(site);
        }
        return new IndexingResponse(true, "");
    }

    private void initPagesCollector() {
        PagesCollector.addRepositories(siteRepository, pageRepository,
                lemmaRepository, indexRepository);
        PagesCollector.setParams(params);
        PagesCollector.setUtils(pageParsingUtils, textUtils, lemmaUtils);
        PagesCollector.setStartedParsing(true);
        PagesCollector.setLemmaUtils(lemmaUtils);
    }

    private void parseSite(SiteEntity site) {
        ForkJoinPool.commonPool().execute(() -> {
            try {
                switch (ForkJoinPool.commonPool().invoke(new PagesCollector(site))) {
                    case COMPLETED -> site.update(SiteStatus.INDEXED);
                    case INTERRUPTED -> site.update("Индексация остановлена пользователем");
                }
            } catch (Exception e) {
                site.update(e.getMessage());
                System.out.println("Indexing of the ".concat(site.getName()).concat(" failed."));
            } finally {
                siteRepository.update(site, site.getStatusTime(), site.getStatus(), site.getLastError());
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
            Connection.Response response = pageParsingUtils.getPageResponse(new URL(url));
            if (response.statusCode() >= 400) {
                return new IndexingResponse(false,
                        "Страница не может быть проиндексирована. Ошибка: " + response.statusCode());
            }
            deletePageIfExists(relativePath, siteEntity);
            PageEntity page = new PageEntity(siteEntity, relativePath, response.statusCode(), response.parse().html());
            pageRepository.save(page);
            addLemmasAndIndexesToDB(siteEntity, page);
            return new IndexingResponse(true, "");
        } catch (IOException e) {
            return new IndexingResponse(false, e.getMessage());
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
            siteForSpecifiedUrl = new SiteEntity(siteConfig.getName(), siteConfig.getUrl(), SiteStatus.INDEXED);
            siteRepository.save(siteForSpecifiedUrl);
        } else {
            siteForSpecifiedUrl = optionalSite.get();
        }
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

    public void addLemmasAndIndexesToDB(SiteEntity site, PageEntity page){
        Map<String, Integer> lemmas = lemmaUtils.getLemmasStatistics(
                textUtils.removeHtmlTags(page.getContent()));
        for (Map.Entry<String, Integer> lemmaEntry : lemmas.entrySet()) {
            LemmaEntity lemma = addOrUpdateLemma(site, lemmaEntry);
            indexRepository.save(new IndexEntity(page, lemma, lemmaEntry.getValue()));
        }
    }

    private LemmaEntity addOrUpdateLemma(SiteEntity site, Map.Entry<String, Integer> lemmaEntry) {
        LemmaEntity lemma;
        synchronized (lock) {
            Optional<LemmaEntity> optionalLemma = lemmaRepository.findBySiteAndLemma(site, lemmaEntry.getKey());
            if (optionalLemma.isEmpty()) {
                lemma = new LemmaEntity(site, lemmaEntry.getKey());
            } else {
                lemma = optionalLemma.get();
                lemma.setFrequency(lemma.getFrequency() + 1);
            }
            lemmaRepository.save(lemma);
        }
        return lemma;
    }
}
