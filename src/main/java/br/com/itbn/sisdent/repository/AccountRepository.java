package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.IdentificationType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, Long> {
    @EntityGraph(attributePaths = {"person", "legacyUser"})
    Optional<Account> findByEmail(String email);

    @EntityGraph(attributePaths = {"person", "legacyUser"})
    Optional<Account> findByLegacyUser_IdentificationTypeAndLegacyUser_IdentificationNumber(
            IdentificationType identificationType, String identificationNumber);

    @EntityGraph(attributePaths = {"person", "legacyUser"})
    Optional<Account> findByGlobalId(UUID globalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"person", "legacyUser"})
    Optional<Account> findLockedByGlobalId(UUID globalId);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "person")
    @Query("select a from Account a where (:filter is null or lower(a.person.displayName) like lower(concat('%', :filter, '%')) or lower(a.email) like lower(concat('%', :filter, '%')))")
    Page<Account> findManagementPage(String filter, Pageable pageable);

    @EntityGraph(attributePaths = "person")
    @Query("select distinct a from Account a join Membership m on m.account = a where m.organization.globalId = :organizationId and (:filter is null or lower(a.person.displayName) like lower(concat('%', :filter, '%')) or lower(a.email) like lower(concat('%', :filter, '%')))")
    Page<Account> findManagementPageInOrganization(UUID organizationId, String filter, Pageable pageable);

    @EntityGraph(attributePaths = "person")
    @Query("select a from Account a where a.globalId = :accountId and exists (select m.id from Membership m where m.account = a and m.organization.globalId = :organizationId)")
    Optional<Account> findVisibleInOrganization(UUID organizationId, UUID accountId);
}
