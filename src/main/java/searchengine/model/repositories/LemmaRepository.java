package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.entities.Lemma;
import searchengine.model.entities.WebSite;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM Lemma")
    void removeAll();
    List<Lemma> findByWebSite(WebSite webSite);
    @Query("SELECT COUNT(l) FROM Lemma l WHERE l.webSite.name= :webSiteName")
    int countAllByWebSiteName(@Param("webSiteName") String webSiteName);
}
