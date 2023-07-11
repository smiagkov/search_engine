package searchengine.utils;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import searchengine.config.PagesCollectorConfig;
import searchengine.dto.parsing.ParsingResult;
import searchengine.exceptions.DuplicatePageException;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.model.SiteEntity;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Setter
@Getter
public class PagesCollector extends RecursiveTask<PagesCollectEndType> {
    private static SiteRepository siteRepository;
    private static PageRepository pageRepository;
    private static LemmaRepository lemmaRepository;
    private static IndexRepository indexRepository;
    private static LemmaUtils lemmaUtils;
    private static PagesCollectorConfig params;
    private static PageParsingUtils pageParsingUtils;
    private static TextUtils textUtils;
    private String path;
    private final SiteEntity site;
    private static boolean isStartedIndexing;
    private final static Object lock = new Object();

    public PagesCollector(String path, SiteEntity site) {
        this.path = path;
        this.site = site;
    }

    public PagesCollector(SiteEntity rootPage) {
        this(pageParsingUtils.getRelativePath(rootPage.getUrl()), rootPage);
    }

    @Override
    protected PagesCollectEndType compute() throws RuntimeException {
        ParsingResult response;
        String[] children = {};
        PageEntity page;
        if (!isStartedIndexing) {
            return PagesCollectEndType.INTERRUPTED;
        }
        try {
            page = getNewPageEntity(site, path);
        } catch(DuplicatePageException e) {
            return PagesCollectEndType.COMPLETED;
        }
        try {
            response = pageParsingUtils.getHttpResponse(path, new URL(site.getUrl()));
            page.update(response.statusCode(), response.document().html());
            if (response.statusCode() >= 400) {
                return PagesCollectEndType.COMPLETED;
            }
            addLemmasAndIndexesToDB(site, page);
            children = getChildPages(response.document());
        } catch (HttpStatusException e) {
            page.update(e.getStatusCode());
        } catch (IOException e) {
            page.update(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }  finally {
            pageRepository.save(page);
            site.updateTimestamp();
            siteRepository.save(site);
        }
        return getPagesCollectResult(children);
    }

    private PageEntity getNewPageEntity(SiteEntity site, String path) throws DuplicatePageException {
        Optional<PageEntity> optionalPage;
        synchronized (lock) {
            optionalPage = pageRepository.findByPathAndSite(path, site);
            if (optionalPage.isEmpty()) {
                PageEntity page = new PageEntity(site, path);
                pageRepository.save(page);
                return page;
            }
        }
        throw new DuplicatePageException();
    }

    private PagesCollectEndType getPagesCollectResult(String[] children) {
        return ForkJoinTask.invokeAll(new ArrayList<>(getSubTasks(children))).stream()
                .map(ForkJoinTask::join)
                .anyMatch(i -> i.equals(PagesCollectEndType.INTERRUPTED)) ?
                PagesCollectEndType.INTERRUPTED :
                PagesCollectEndType.COMPLETED;
    }

    private String[] getChildPages(Document document) {
        String[] filteredPages;
        Set<String> parsedPages = document.select("a[href]").stream()
                .map(e -> e.attr("href"))
                .filter(s -> pageParsingUtils.isValidChildLink(s, site.getUrl()))
                .map(s -> s.startsWith("/") ? s : pageParsingUtils.getRelativePath(s))
                .collect(Collectors.toSet());
        Set<String> savedPages = pageRepository.getAllPathsBySitePathIn(parsedPages, site);
        filteredPages = parsedPages.stream()
                .filter(Predicate.not(savedPages::contains))
                .toArray(String[]::new);
        return filteredPages;
    }

    private List<PagesCollector> getSubTasks(String[] pages) {
        return Arrays.stream(pages)
                .map(e -> new PagesCollector(e, site))
                .toList();
    }

    public static void addRepositories(SiteRepository siteRepository, PageRepository pageRepository,
                                       LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        PagesCollector.pageRepository = pageRepository;
        PagesCollector.siteRepository = siteRepository;
        PagesCollector.lemmaRepository = lemmaRepository;
        PagesCollector.indexRepository = indexRepository;
    }

    public static void setParams(PagesCollectorConfig params) {
        PagesCollector.params = params;
    }

    public static void setUtils(PageParsingUtils pageParsingUtils, TextUtils textUtils,
                                LemmaUtils lemmaUtils) {
        PagesCollector.pageParsingUtils = pageParsingUtils;
        PagesCollector.textUtils = textUtils;
        PagesCollector.lemmaUtils = lemmaUtils;
    }

    public static void setLemmaUtils(LemmaUtils lemmaUtils) {
        PagesCollector.lemmaUtils = lemmaUtils;
    }

    public static void setStartedParsing(boolean isStartedIndexing) {
        PagesCollector.isStartedIndexing = isStartedIndexing;
    }

    public static void stopParsing() {
        isStartedIndexing = false;
    }

    public void addLemmasAndIndexesToDB(SiteEntity site, PageEntity page) {
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
