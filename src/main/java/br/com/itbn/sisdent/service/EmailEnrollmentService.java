package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.EmailEnrollmentResponse;
import br.com.itbn.sisdent.dto.EmailVerificationResponse;
import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.AccountEmailClaim;
import br.com.itbn.sisdent.model.EmailClaimType;
import br.com.itbn.sisdent.model.EmailVerificationChallenge;
import br.com.itbn.sisdent.repository.AccountEmailClaimRepository;
import br.com.itbn.sisdent.repository.AccountRepository;
import br.com.itbn.sisdent.repository.EmailVerificationChallengeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class EmailEnrollmentService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CurrentAccountService currentAccountService;
    private final AccountRepository accountRepository;
    private final AccountEmailClaimRepository emailClaimRepository;
    private final EmailVerificationChallengeRepository challengeRepository;
    private final EmailVerificationDelivery delivery;
    private final Duration challengeLifetime;
    private final Duration resendCooldown;
    private final Duration recentWindow;
    private final int maxRecentChallenges;

    public EmailEnrollmentService(
            CurrentAccountService currentAccountService,
            AccountRepository accountRepository,
            AccountEmailClaimRepository emailClaimRepository,
            EmailVerificationChallengeRepository challengeRepository,
            EmailVerificationDelivery delivery,
            @Value("${sisdent.email-verification.challenge-lifetime-seconds:900}")
                    long challengeLifetimeSeconds,
            @Value("${sisdent.email-verification.resend-cooldown-seconds:60}")
                    long resendCooldownSeconds,
            @Value("${sisdent.email-verification.recent-window-seconds:86400}")
                    long recentWindowSeconds,
            @Value("${sisdent.email-verification.max-recent-challenges:5}")
                    int maxRecentChallenges) {
        this.currentAccountService = currentAccountService;
        this.accountRepository = accountRepository;
        this.emailClaimRepository = emailClaimRepository;
        this.challengeRepository = challengeRepository;
        this.delivery = delivery;
        this.challengeLifetime = Duration.ofSeconds(challengeLifetimeSeconds);
        this.resendCooldown = Duration.ofSeconds(resendCooldownSeconds);
        this.recentWindow = Duration.ofSeconds(recentWindowSeconds);
        this.maxRecentChallenges = maxRecentChallenges;
    }

    @Transactional
    public EmailEnrollmentResponse start(String candidateEmail) {
        Account account = lockCurrentAccount();
        requireMigrating(account);
        String normalizedEmail = Account.normalizeEmail(candidateEmail);
        enforceRateLimits(account, Instant.now());

        AccountEmailClaim existingPending = emailClaimRepository
                .findByAccount_IdAndClaimType(account.getId(), EmailClaimType.PENDING)
                .orElse(null);
        try {
            if (existingPending != null
                    && !existingPending.getNormalizedEmail().equals(normalizedEmail)) {
                emailClaimRepository.delete(existingPending);
                emailClaimRepository.flush();
            }
            if (existingPending == null
                    || !existingPending.getNormalizedEmail().equals(normalizedEmail)) {
                emailClaimRepository.saveAndFlush(new AccountEmailClaim(
                        account, normalizedEmail, EmailClaimType.PENDING));
            }
            account.beginEmailEnrollment(normalizedEmail);
            accountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailEnrollmentUnavailableException();
        }
        createAndDeliver(account, normalizedEmail, Instant.now());
        return EmailEnrollmentResponse.challengeSent();
    }

    @Transactional
    public EmailEnrollmentResponse resend() {
        Account account = lockCurrentAccount();
        requireMigrating(account);
        if (account.getPendingEmail() == null) {
            throw new EmailEnrollmentUnavailableException();
        }
        Instant now = Instant.now();
        enforceRateLimits(account, now);
        createAndDeliver(account, account.getPendingEmail(), now);
        return EmailEnrollmentResponse.challengeSent();
    }

    @Transactional
    public EmailVerificationResponse verify(String rawSecret) {
        if (rawSecret == null || rawSecret.length() < 32 || rawSecret.length() > 256) {
            return EmailVerificationResponse.invalidOrExpired();
        }
        String secretHash = hash(rawSecret);
        java.util.UUID accountGlobalId = challengeRepository
                .findAccountGlobalIdBySecretHash(secretHash)
                .orElse(null);
        Instant now = Instant.now();
        if (accountGlobalId == null) {
            return EmailVerificationResponse.invalidOrExpired();
        }
        Account account = accountRepository.findLockedByGlobalId(accountGlobalId).orElse(null);
        EmailVerificationChallenge challenge = challengeRepository
                .findBySecretHash(secretHash)
                .orElse(null);
        if (account == null
                || challenge == null
                || !challenge.isUsableAt(now)
                || !account.isEmailMigrationRequired()
                || account.isEmailVerified()
                || !challenge.getTargetEmail().equals(account.getPendingEmail())) {
            return EmailVerificationResponse.invalidOrExpired();
        }
        AccountEmailClaim pendingClaim = emailClaimRepository
                .findByAccount_IdAndClaimType(account.getId(), EmailClaimType.PENDING)
                .filter(claim -> claim.getNormalizedEmail().equals(challenge.getTargetEmail()))
                .orElse(null);
        if (pendingClaim == null) {
            return EmailVerificationResponse.invalidOrExpired();
        }

        emailClaimRepository.deleteByAccount_IdAndClaimType(account.getId(), EmailClaimType.VERIFIED);
        emailClaimRepository.flush();
        pendingClaim.markVerified();
        account.completeEmailVerification(challenge.getTargetEmail());
        challenge.consume(now);
        revokeActiveChallenges(account.getId(), now, challenge);
        accountRepository.save(account);
        challengeRepository.save(challenge);
        return EmailVerificationResponse.verified();
    }

    private Account lockCurrentAccount() {
        Account current = currentAccountService.require();
        return accountRepository.findLockedByGlobalId(current.getGlobalId())
                .orElseThrow(EmailEnrollmentUnavailableException::new);
    }

    private void requireMigrating(Account account) {
        if (!account.isEmailMigrationRequired() || account.isEmailVerified()) {
            throw new EmailEnrollmentUnavailableException();
        }
    }

    private void enforceRateLimits(Account account, Instant now) {
        challengeRepository.findTopByAccount_IdOrderByCreatedAtDesc(account.getId())
                .ifPresent(latest -> {
                    Instant nextAllowed = latest.getCreatedAt().plus(resendCooldown);
                    if (nextAllowed.isAfter(now)) {
                        throw new EmailEnrollmentThrottledException(
                                Duration.between(now, nextAllowed).toSeconds() + 1);
                    }
                });
        if (challengeRepository.countByAccount_IdAndCreatedAtGreaterThanEqual(
                account.getId(), now.minus(recentWindow)) >= maxRecentChallenges) {
            throw new EmailEnrollmentThrottledException(recentWindow.toSeconds());
        }
    }

    private void createAndDeliver(Account account, String targetEmail, Instant now) {
        revokeActiveChallenges(account.getId(), now, null);
        String secret = newSecret();
        EmailVerificationChallenge challenge = challengeRepository.saveAndFlush(
                new EmailVerificationChallenge(account, targetEmail, hash(secret),
                        now.plus(challengeLifetime), delivery.providerName()));
        String messageId = delivery.deliver(account.getId(), targetEmail, secret);
        challenge.recordDelivery(messageId);
    }

    private void revokeActiveChallenges(
            Long accountId, Instant now, EmailVerificationChallenge except) {
        challengeRepository
                .findAllByAccount_IdAndConsumedAtIsNullAndRevokedAtIsNull(accountId)
                .stream()
                .filter(challenge -> except == null || !challenge.getId().equals(except.getId()))
                .forEach(challenge -> challenge.revoke(now));
    }

    private static String newSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String secret) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
