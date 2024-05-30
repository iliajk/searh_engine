package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.entities.Index;
@Repository
public interface IndexRepository extends JpaRepository<Index,Long> {
}
