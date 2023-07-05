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

    int countBySite(SiteEntity siteEntity);

    List<LemmaEntity> findBySiteAndLemmaIn(SiteEntity site, String[] queryLemmas);
}
