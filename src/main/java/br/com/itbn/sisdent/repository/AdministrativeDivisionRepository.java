package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.AdministrativeDivision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdministrativeDivisionRepository extends JpaRepository<AdministrativeDivision, Long> {

    @Override
    @EntityGraph(attributePaths = "country")
    List<AdministrativeDivision> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = "country")
    Page<AdministrativeDivision> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "country")
    Optional<AdministrativeDivision> findByCountry_CodeAndCode(String countryCode, String code);
}
