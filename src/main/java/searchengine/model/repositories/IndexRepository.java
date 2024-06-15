package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.entities.Index;
@Repository
public interface IndexRepository extends JpaRepository<Index,Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM Index")
    void removeAll();
}
