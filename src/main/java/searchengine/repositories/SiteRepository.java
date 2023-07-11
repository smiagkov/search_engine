package searchengine.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends ListCrudRepository<SiteEntity, Integer> {
    Optional<SiteEntity> findByUrlLike(String url);

    @Modifying
    @Transactional
    @Query("DELETE FROM SiteEntity s WHERE s.name IN :names")
    void deleteByNameIn(String[] names);

    List<SiteEntity> findByNameIn(String[] names);
}

