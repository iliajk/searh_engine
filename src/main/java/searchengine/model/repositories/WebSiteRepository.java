package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.entities.WebSite;

@Repository
public interface WebSiteRepository extends JpaRepository<WebSite, Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM WebSite s WHERE s.url LIKE :url% OR s.name = :name")
    void removeSiteInfo(String url, String name);
}
