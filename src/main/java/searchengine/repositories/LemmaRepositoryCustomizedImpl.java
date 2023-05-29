package searchengine.repositories;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaEntity;

import java.util.List;

@Transactional
@RequiredArgsConstructor
public class LemmaRepositoryCustomizedImpl implements LemmaRepositoryCustomized {
    private final EntityManager entityManager;
    @Override
    public void decrementFrequencyOrDelete(List<LemmaEntity> lemmas) {
        String updateHql = "UPDATE LemmaEntity l SET l.frequency = l.frequency - 1 WHERE l IN :lemmas";
        String deleteHql = "DELETE FROM LemmaEntity l WHERE l IN :lemmas AND l.frequency < 1";
        entityManager.createQuery(updateHql).setParameter("lemmas", lemmas).executeUpdate();
        entityManager.createQuery(deleteHql).setParameter("lemmas", lemmas).executeUpdate();
    }
}
