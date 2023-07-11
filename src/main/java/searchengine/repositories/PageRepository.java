package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.Optional;
import java.util.Set;

@Repository
public interface PageRepository extends CrudRepository<PageEntity, Integer>, PageRepositoryCustomized {
    @Query("SELECT p.path FROM PageEntity p WHERE p.path IN ?1 AND p.site = ?2")
    Set<String> getAllPathsBySitePathIn(Set<String> paths, SiteEntity site);

    Optional<PageEntity> findByPathAndSite(String path, SiteEntity site);

    int countBySite(SiteEntity siteEntity);
}
