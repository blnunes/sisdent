package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.Speciality;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpecialityRepository extends JpaRepository<Speciality, Long> {

    @Override
    @EntityGraph(attributePaths = "procedures")
    List<Speciality> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = "procedures")
    Optional<Speciality> findById(Long id);

    Optional<Speciality> findByName(String name);
}
