package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.*;

@Repository
public interface LemmaRepository extends CrudRepository<LemmaEntity, Integer>, LemmaRepositoryCustomized {
    @Query("SELECT l FROM LemmaEntity l WHERE l IN (SELECT lemma FROM IndexEntity i WHERE i.page = ?1)")
    List<LemmaEntity> findAllByPage(PageEntity page);

    Optional<LemmaEntity> findBySiteAndLemma(SiteEntity site, String lemma);

//    @Query("SELECT COUNT(l) FROM LemmaEntity l WHERE l.site = ?1")
    int countBySite(SiteEntity siteEntity);

//    @Query("SELECT l FROM LemmaEntity l WHERE l.site = ?1 AND l.lemma IN ?2")
    List<LemmaEntity> findBySiteAndLemmaIn(SiteEntity site, String[] queryLemmas);
}
