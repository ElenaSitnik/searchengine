package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Lemma;

import java.util.Optional;

public interface LemmaRepository extends JpaRepository<Lemma, Long> {
    Optional<Lemma> findByLemma(String lemma);


}
