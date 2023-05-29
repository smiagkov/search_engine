package searchengine.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface LemmaRepository extends ListCrudRepository<LemmaEntity, Integer>, LemmaRepositoryCustomized {
    @Query("SELECT l FROM LemmaEntity l WHERE l IN (SELECT lemma FROM IndexEntity i WHERE i.page = ?1)")
//            "JOIN IndexEntity i ON i.lemma = l.id WHERE i.page = ?1")
    List<LemmaEntity> findAllByPage(PageEntity page);

    @Modifying
    @Transactional
    @Query("UPDATE LemmaEntity l SET l.frequency = l.frequency + 1 WHERE l IN ?1")
    void incrementFrequencyAll(List<LemmaEntity> lemmas);

    @Query("SELECT l FROM LemmaEntity l WHERE l.site = ?1 AND l.lemma IN ?2")
    List<LemmaEntity> findAllBySite(SiteEntity site, Set<String> word);

    @Query("SELECT l FROM LemmaEntity l WHERE l.site = ?1 AND l.lemma = ?2")
    Optional<LemmaEntity> findBySiteLemma(SiteEntity site, String key);

    @Query("SELECT COUNT(l) FROM LemmaEntity l WHERE l.site = ?1")
    int countBySite(SiteEntity siteEntity);
}
