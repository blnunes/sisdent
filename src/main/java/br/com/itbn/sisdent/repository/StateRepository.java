package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.State;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StateRepository extends JpaRepository<State, Long> {

    Optional<State> findByAbbreviation(String abbreviation);
}
