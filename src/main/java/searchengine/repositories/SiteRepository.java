package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteModel;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<SiteModel, Long> {

    @Query(value = """
            SELECT * FROM site WHERE url = ?1
            """, nativeQuery = true)
    Optional<SiteModel> findByURL(String URL);

}
