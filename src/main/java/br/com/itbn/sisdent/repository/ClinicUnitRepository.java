package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.ClinicUnit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClinicUnitRepository extends JpaRepository<ClinicUnit, Long> {
    @EntityGraph(attributePaths = "organization")
    Optional<ClinicUnit> findByGlobalId(UUID globalId);
}
