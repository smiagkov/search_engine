package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.Test1;
import searchengine.services.TextUtils;

@RestController
public class TestController {
    private final Test1 test1;
    private final TextUtils textUtils;
    private final SiteRepository siteRepository;
    private final SitesList sitesList;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private IndexRepository indexRepository;

    public TestController(Test1 test1, TextUtils textUtils, SiteRepository siteRepository, SitesList sitesList) {
        this.test1 = test1;
        this.textUtils = textUtils;
        this.siteRepository = siteRepository;
        this.sitesList = sitesList;
    }

    @GetMapping("/test")
    public ResponseEntity<StatisticsResponse> test() {
        test1.testUpsert();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/delete")
    public ResponseEntity<StatisticsResponse> delete() {
//        test1.deleteTestObjects();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/update")
    public ResponseEntity<StatisticsResponse> update() {
//        test1.update();
        return ResponseEntity.ok().build();
    }


}
