package searchengine.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteEntity;
import searchengine.model.SiteStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends ListCrudRepository<SiteEntity, Integer> {
    @Query("SELECT s FROM SiteEntity s WHERE s.url LIKE ?1%")
    Optional<SiteEntity> findByUrl(String url);

    @Modifying
    @Transactional
    @Query("DELETE FROM SiteEntity s WHERE s.name IN :names")
    void deleteBulkByNames(@Param("names") List<String> names);


    @Modifying
    @Transactional
    @Query("UPDATE SiteEntity s SET statusTime = ?2 WHERE s = ?1")
    void updateStatusTime(SiteEntity rootPage, LocalDateTime statusTime);

    @Modifying
    @Transactional
    @Query("UPDATE SiteEntity s SET statusTime = ?2, status = ?3, lastError = ?4 WHERE s = ?1")
    void update(SiteEntity site, LocalDateTime statusTime, SiteStatus status, String error);
}

