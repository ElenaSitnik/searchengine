package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.PageModel;

@Repository
public interface PageRepository extends JpaRepository<PageModel, Long> {

    @Query(value = """
            SELECT * FROM page WHERE path = ?1 AND site_id = ?2
            """, nativeQuery = true)
    PageModel findByPathAndSiteId(String path, Long siteId);

    @Query(value = """
            DELETE FROM page WHERE site_id = ?1
            """, nativeQuery = true)
    void deleteAllBySiteId(Long siteId);

}
