package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.Speciality;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpecialityRepository extends JpaRepository<Speciality, Long>, JpaSpecificationExecutor<Speciality> {

    @Override
    @EntityGraph(attributePaths = "procedures")
    List<Speciality> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = "procedures")
    Page<Speciality> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "procedures")
    Page<Speciality> findAll(Specification<Speciality> specification, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "procedures")
    Optional<Speciality> findById(Long id);

    Optional<Speciality> findByName(String name);

    @Query("select distinct s.name from Speciality s where lower(s.name) like lower(concat('%', :query, '%')) order by s.name")
    List<String> findNameSuggestions(@Param("query") String query, Pageable pageable);

    @Query("select distinct p.name from DentalProcedure p where lower(p.name) like lower(concat('%', :query, '%')) order by p.name")
    List<String> findProcedureSuggestions(@Param("query") String query, Pageable pageable);
}
