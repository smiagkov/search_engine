package searchengine.repositories;

import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;

import java.util.List;
@Repository
public interface LemmaRepositoryCustomized {
    void decrementFrequencyOrDelete(List<LemmaEntity> lemmas);
}
