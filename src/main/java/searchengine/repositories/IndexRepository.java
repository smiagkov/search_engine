package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
public interface IndexRepository extends ListCrudRepository<IndexEntity, Integer> {
    @Query("SELECT i.page FROM IndexEntity i WHERE i.lemma = ?1")
    List<PageEntity> findPagesByLemma(LemmaEntity lemma);

    @Query("SELECT i.page FROM IndexEntity i WHERE i.lemma = ?1 AND i.page IN ?2")
    List<PageEntity> findPagesByLemmaInPages(LemmaEntity lemma, List<PageEntity> pages);

    @Query("SELECT i FROM IndexEntity i WHERE i.page = ?1 AND i.lemma IN ?2")
    List<IndexEntity> findAllByPageLemmaIn(PageEntity page, List<LemmaEntity> lemmas);

    @Query("SELECT COUNT(i) FROM IndexEntity i WHERE i.page IN (SELECT p FROM PageEntity p WHERE p.site = ?1)")
    int countIndexesBySite(SiteEntity siteEntity);
}
