package searchengine.services;

import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.Map;


public interface LemmaService {
    void updateOnDeletePage(PageEntity page);

    void addLemmasAndIndexesToDB(SiteEntity site, PageEntity page);
    Map<String, Integer> getLemmasStatistics(String text);
}
