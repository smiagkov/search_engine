package searchengine.services;

import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.io.IOException;
import java.util.Map;


public interface LemmaService {
    void updateOnDeletePage(PageEntity page);

    void addLemmasAndIndexesToDB(SiteEntity site, PageEntity page) throws IOException;
    Map<String, Integer> getLemmas(String text) throws IOException;
}
