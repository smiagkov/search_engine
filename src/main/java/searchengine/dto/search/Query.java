package searchengine.dto.search;

import java.util.Objects;

public record Query(String queryText, int offset, int limit, String site) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Query query = (Query) o;
        return Objects.equals(queryText, query.queryText) && Objects.equals(site, query.site);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queryText, site);
    }
}
