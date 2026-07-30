package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.Membership;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    @EntityGraph(attributePaths = {"organization", "clinicUnit", "account"})
    List<Membership> findAllByAccount_IdAndActiveTrue(Long accountId);

    @EntityGraph(attributePaths = {"organization", "clinicUnit", "account"})
    List<Membership> findAllByAccount_IdAndOrganization_GlobalIdAndActiveTrue(Long accountId, UUID organizationId);

    @EntityGraph(attributePaths = {"organization", "clinicUnit", "account"})
    Optional<Membership> findByGlobalId(UUID globalId);

    boolean existsByAccount_IdAndOrganization_IdAndClinicUnit_Id(Long accountId, Long organizationId, Long clinicUnitId);
    boolean existsByAccount_IdAndOrganization_IdAndClinicUnitIsNull(Long accountId, Long organizationId);
}
