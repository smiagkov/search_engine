package searchengine.repositories;

import org.springframework.stereotype.Repository;
import searchengine.exceptions.DuplicatePageException;
import searchengine.model.PageEntity;

@Repository
public interface PageRepositoryCustomized {
    void saveIfNotExists(PageEntity page) throws DuplicatePageException;
}
