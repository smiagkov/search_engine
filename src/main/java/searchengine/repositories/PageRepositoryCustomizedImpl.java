package searchengine.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.exceptions.DuplicatePageException;
import searchengine.model.PageEntity;
import searchengine.repositories.PageRepositoryCustomized;

@Repository
@RequiredArgsConstructor
public class PageRepositoryCustomizedImpl implements PageRepositoryCustomized {
    private final EntityManager entityManager;

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void saveIfNotExists(PageEntity page) throws DuplicatePageException {
        String hql = """
                SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END\s
                FROM PageEntity p\s
                WHERE p.path = :path AND p.site = :site
                """;
        Query query = entityManager.createQuery(hql);
        query.setParameter("path", page.getPath());
        query.setParameter("site", page.getSite());
        if ((boolean) query.getSingleResult()) {
            throw new DuplicatePageException();
        }
        entityManager.persist(page);
    }
}
