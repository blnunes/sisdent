package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.PatientOrganizationLink;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface PatientOrganizationLinkRepository extends JpaRepository<PatientOrganizationLink, Long>, JpaSpecificationExecutor<PatientOrganizationLink> {
    @EntityGraph(attributePaths = {"patient", "patient.address", "patient.address.country",
            "patient.nationality", "patient.documentIssuerCountry", "patient.specialities",
            "organization", "clinicUnit"})
    List<PatientOrganizationLink> findAllByOrganization_GlobalId(UUID organizationId);

    @EntityGraph(attributePaths = {"patient", "patient.address", "patient.address.administrativeDivision",
            "patient.address.country", "patient.nationality", "patient.documentIssuerCountry",
            "patient.specialities", "patient.specialities.procedures"})
    @Query("""
            select distinct link from PatientOrganizationLink link
            where link.organization.globalId = :organizationId
              and lower(link.patient.name) like lower(concat('%', :name, '%'))
            order by link.patient.name
            """)
    List<PatientOrganizationLink> searchLinkedPatientsInOrganization(
            @Param("organizationId") UUID organizationId,
            @Param("name") String name);

    @EntityGraph(attributePaths = {"patient", "patient.address", "patient.address.administrativeDivision",
            "patient.address.country", "patient.nationality", "patient.documentIssuerCountry",
            "patient.specialities", "patient.specialities.procedures"})
    @Query("""
            select distinct link from PatientOrganizationLink link
            where link.organization.globalId = :organizationId
              and link.clinicUnit.globalId = :clinicUnitId
              and lower(link.patient.name) like lower(concat('%', :name, '%'))
            order by link.patient.name
            """)
    List<PatientOrganizationLink> searchLinkedPatientsInClinic(
            @Param("organizationId") UUID organizationId,
            @Param("clinicUnitId") UUID clinicUnitId,
            @Param("name") String name);

    boolean existsByPatient_IdAndOrganization_IdAndClinicUnit_Id(Long patientId, Long organizationId, Long clinicUnitId);
    boolean existsByPatient_IdAndOrganization_IdAndClinicUnitIsNull(Long patientId, Long organizationId);

    @EntityGraph(attributePaths = {"patient", "organization", "clinicUnit"})
    Optional<PatientOrganizationLink> findFirstByPatient_GlobalIdAndOrganization_GlobalId(
            UUID patientId, UUID organizationId);

    @EntityGraph(attributePaths = {"patient", "organization", "clinicUnit"})
    Optional<PatientOrganizationLink> findFirstByPatient_GlobalIdAndOrganization_GlobalIdAndClinicUnit_GlobalIdAndActiveTrue(
            UUID patientId, UUID organizationId, UUID clinicUnitId);

    @Override
    @EntityGraph(attributePaths = {"patient", "patient.address", "patient.address.administrativeDivision",
            "patient.address.country", "patient.nationality", "patient.documentIssuerCountry",
            "patient.specialities", "patient.specialities.procedures", "organization", "clinicUnit"})
    Page<PatientOrganizationLink> findAll(Specification<PatientOrganizationLink> specification, Pageable pageable);

    List<PatientOrganizationLink> findAllByPatient_GlobalIdAndOrganization_GlobalIdAndActiveTrue(
            UUID patientId, UUID organizationId);

    boolean existsByPatient_IdAndOrganization_IdAndActiveTrue(Long patientId, Long organizationId);

    boolean existsByPatient_IdAndOrganization_GlobalIdNotAndActiveTrue(Long patientId, UUID organizationId);
}
