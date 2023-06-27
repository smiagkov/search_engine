package searchengine.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.Optional;
import java.util.Set;

@Repository
public interface PageRepository extends CrudRepository<PageEntity, Integer>, PageRepositoryCustomized {
    @Query("SELECT p.path FROM PageEntity p WHERE p.path IN ?1 AND p.site = ?2")
    Set<String> findAllBySitePathIn(Set<String> paths, SiteEntity site);

//    @Query("SELECT p FROM PageEntity p WHERE p.path = ?1 AND p.site = ?2")
    Optional<PageEntity> findByPathAndSite(String path, SiteEntity site);

    @Modifying
    @Transactional
    @Query("UPDATE PageEntity p SET p.code = ?2, p.content = ?3 WHERE p = ?1")
    void update(PageEntity page, int code, String content);

//    @Query("SELECT COUNT(p) FROM PageEntity p WHERE p.site = ?1")
    int countBySite(SiteEntity siteEntity);
}
