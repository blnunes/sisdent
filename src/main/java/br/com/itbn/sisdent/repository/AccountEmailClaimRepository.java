package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.AccountEmailClaim;
import br.com.itbn.sisdent.model.EmailClaimType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountEmailClaimRepository extends JpaRepository<AccountEmailClaim, Long> {
    Optional<AccountEmailClaim> findByAccount_IdAndClaimType(Long accountId, EmailClaimType claimType);
    void deleteByAccount_IdAndClaimType(Long accountId, EmailClaimType claimType);
}
