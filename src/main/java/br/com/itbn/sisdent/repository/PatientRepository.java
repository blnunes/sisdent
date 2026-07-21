package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.Patient;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    @Override
    @EntityGraph(attributePaths = {"address", "address.state"})
    List<Patient> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = {"address", "address.state"})
    Optional<Patient> findById(Long id);
}
