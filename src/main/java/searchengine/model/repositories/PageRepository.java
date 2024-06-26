package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.entities.Page;


@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM Page")
    void removeAll();

    @Query("SELECT COUNT(p) FROM Page p WHERE p.webSite.name= :webSiteName")
    int countAllByWebSiteName(@Param("webSiteName") String webSiteName);
}
