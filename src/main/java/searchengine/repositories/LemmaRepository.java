package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.*;

@Repository
public interface LemmaRepository extends ListCrudRepository<LemmaEntity, Integer>, LemmaRepositoryCustomized {
    @Query("SELECT l FROM LemmaEntity l WHERE l IN (SELECT lemma FROM IndexEntity i WHERE i.page = ?1)")
    List<LemmaEntity> findAllByPage(PageEntity page);

    @Query("SELECT l FROM LemmaEntity l WHERE l.site = ?1 AND l.lemma = ?2")
    Optional<LemmaEntity> findBySiteLemma(SiteEntity site, String key);

    @Query("SELECT COUNT(l) FROM LemmaEntity l WHERE l.site = ?1")
    int countBySite(SiteEntity siteEntity);

    @Query("SELECT l FROM LemmaEntity l WHERE l.site = ?1 AND l.lemma IN ?2")
    List<LemmaEntity> findAllByLemmasSite(SiteEntity site, String[] queryLemmas);
}
