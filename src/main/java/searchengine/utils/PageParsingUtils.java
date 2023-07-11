package searchengine.utils;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import searchengine.config.PagesCollectorConfig;
import searchengine.dto.parsing.ParsingResult;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@Component
@RequiredArgsConstructor
public class PageParsingUtils {
    private final PagesCollectorConfig params;

    public String getRelativePath(String path) {
        if (path.startsWith("/")) {
            return path;
        }
        try {
            path = new URL(path).getFile();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return path.isBlank() ? "/" : path;
    }

    public boolean isValidChildLink(String url, String rootPageUrl) {
        if (!(url.startsWith("/") || url.startsWith(rootPageUrl))
                || url.contains("#") || url.startsWith("//")) {
            return false;
        }
        url = getRelativePath(url);
        return !url.contains(".") || url.matches(".+(\\.htm|\\.html)[/?]?.*");
    }

    public ParsingResult getHttpResponse(String url, URL rootPage) throws IOException {
        return getHttpResponse(new URL(rootPage, url));
    }

    public ParsingResult getHttpResponse(URL link) throws IOException {
        try {
            Thread.sleep(params.getDelay());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Connection.Response response =  Jsoup.newSession()
                .userAgent(params.getJsoupUserAgent())
                .referrer(params.getJsoupReferer())
                .url(link)
                .followRedirects(params.getRedirect())
                .execute();
        return new ParsingResult(response.statusCode(), response.parse());
    }

    public String getTitle(String html) {
        Document document = Jsoup.parse(html);
        return document.title();
    }

    public String normalizeSiteUrl(String urlPresentation) {
        URL url;
        try {
            url = new URL(urlPresentation);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return url.getProtocol()
                .concat("://")
                .concat(url.getHost());
    }
}
