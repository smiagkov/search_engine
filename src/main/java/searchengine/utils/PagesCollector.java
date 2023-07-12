package searchengine.utils;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import searchengine.config.PagesCollectorConfig;
import searchengine.dto.parsing.ParsingResult;
import searchengine.dto.parsing.ReposUtilsParams;
import searchengine.exceptions.DuplicatePageException;
import searchengine.model.PageEntity;
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
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaUtils lemmaUtils;
    private PagesCollectorConfig params;
    private PageParsingUtils pageParsingUtils;
    private TextUtils textUtils;
    private String path;
    private final SiteEntity site;
    private ReposUtilsParams parameters;
    private static boolean isStartedIndexing;
    private final static Object lock = new Object();

    public PagesCollector(String path, SiteEntity site, ReposUtilsParams parameters) {
        this.path = path;
        this.site = site;
        this.siteRepository = parameters.siteRepository();
        this.pageRepository = parameters.pageRepository();
        this.lemmaUtils = parameters.lemmaUtils();
        this.textUtils = parameters.textUtils();
        this.pageParsingUtils = parameters.pageParsingUtils();
        this.params = parameters.collectorConfig();
        this.parameters = parameters;
    }

    public PagesCollector(SiteEntity rootPage, ReposUtilsParams parameters) {
        this(parameters.pageParsingUtils().getRelativePath(rootPage.getUrl()),
                rootPage,
                parameters);
        isStartedIndexing = true;
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
            lemmaUtils.addLemmasAndIndexesToDB(site, page);
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
                .map(e -> new PagesCollector(e, site, parameters))
                .toList();
    }

    public static void stopParsing() {
        isStartedIndexing = false;
    }
}
