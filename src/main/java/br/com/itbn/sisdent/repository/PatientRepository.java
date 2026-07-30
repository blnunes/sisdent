package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.Patient;
import org.jspecify.annotations.NullMarked;
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

@NullMarked
public interface PatientRepository extends JpaRepository<Patient, Long>, JpaSpecificationExecutor<Patient> {

    @Override
    @EntityGraph(attributePaths = {
            "address",
            "address.state",
            "address.country",
            "nationality",
            "specialities",
            "specialities.procedures"})
    List<Patient> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = {
            "address", "address.state", "address.country", "nationality", "specialities", "specialities.procedures"})
    Page<Patient> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {
            "address", "address.state", "address.country", "nationality", "specialities", "specialities.procedures"})
    Page<Patient> findAll(Specification<Patient> specification, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {
            "address",
            "address.state",
            "address.country",
            "nationality",
            "specialities",
            "specialities.procedures"})
    Optional<Patient> findById(Long id);

    Optional<Patient> findByTaxId(String taxId);

    Optional<Patient> findByIdentificationNumber(String identificationNumber);

    @Query("select distinct p.name from Patient p where lower(p.name) like lower(concat('%', :query, '%')) order by p.name")
    List<String> findNameSuggestions(@Param("query") String query, Pageable pageable);

    @Query("select distinct p.taxId from Patient p where lower(p.taxId) like lower(concat('%', :query, '%')) order by p.taxId")
    List<String> findTaxIdSuggestions(@Param("query") String query, Pageable pageable);

    @Query("select distinct p.identificationNumber from Patient p where lower(p.identificationNumber) like lower(concat('%', :query, '%')) order by p.identificationNumber")
    List<String> findIdentificationNumberSuggestions(@Param("query") String query, Pageable pageable);

    @Query("select distinct p.nationality.code, p.nationality.name from Patient p where lower(p.nationality.code) like lower(concat('%', :query, '%')) or lower(p.nationality.name) like lower(concat('%', :query, '%')) order by p.nationality.name")
    List<Object[]> findNationalitySuggestions(@Param("query") String query, Pageable pageable);

    @Query("select distinct p.address.id, p.address.street, p.address.district, p.address.postalCode from Patient p where lower(p.address.street) like lower(concat('%', :query, '%')) or lower(p.address.district) like lower(concat('%', :query, '%')) or p.address.postalCode like concat('%', :query, '%') order by p.address.street")
    List<Object[]> findAddressSuggestions(@Param("query") String query, Pageable pageable);

    @Query("select distinct s.id, s.name from Patient p join p.specialities s where lower(s.name) like lower(concat('%', :query, '%')) order by s.name")
    List<Object[]> findSpecialitySuggestions(@Param("query") String query, Pageable pageable);
}
