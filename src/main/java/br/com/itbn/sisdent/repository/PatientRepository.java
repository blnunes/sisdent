package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.Patient;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

@NullMarked
public interface PatientRepository extends JpaRepository<Patient, Long> {

    @Override
    @EntityGraph(attributePaths = {"address", "address.state", "specialities"})
    List<Patient> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = {"address", "address.state", "specialities"})
    Optional<Patient> findById(Long id);
}
