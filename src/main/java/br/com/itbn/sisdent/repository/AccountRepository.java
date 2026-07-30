package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.IdentificationType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
}
