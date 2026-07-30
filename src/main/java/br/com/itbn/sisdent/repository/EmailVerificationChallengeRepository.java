package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.EmailVerificationChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationChallengeRepository
        extends JpaRepository<EmailVerificationChallenge, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"account"})
    Optional<EmailVerificationChallenge> findBySecretHash(String secretHash);

    @Query("select challenge.account.globalId from EmailVerificationChallenge challenge "
            + "where challenge.secretHash = :secretHash")
    Optional<UUID> findAccountGlobalIdBySecretHash(String secretHash);

    List<EmailVerificationChallenge> findAllByAccount_IdAndConsumedAtIsNullAndRevokedAtIsNull(
            Long accountId);

    Optional<EmailVerificationChallenge> findTopByAccount_IdOrderByCreatedAtDesc(Long accountId);

    long countByAccount_IdAndCreatedAtGreaterThanEqual(Long accountId, Instant createdAt);
}
