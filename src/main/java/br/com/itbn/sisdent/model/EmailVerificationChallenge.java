package br.com.itbn.sisdent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verification_challenges")
public class EmailVerificationChallenge extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "global_id", nullable = false, unique = true, updatable = false)
    private UUID globalId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "target_email", nullable = false, length = 320)
    private String targetEmail;

    @Column(name = "secret_hash", nullable = false, unique = true, length = 64)
    private String secretHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "delivery_provider", nullable = false, length = 64)
    private String deliveryProvider;

    @Column(name = "delivery_message_id")
    private String deliveryMessageId;

    protected EmailVerificationChallenge() {
    }

    public EmailVerificationChallenge(
            Account account,
            String targetEmail,
            String secretHash,
            Instant expiresAt,
            String deliveryProvider) {
        this.account = account;
        this.targetEmail = Account.normalizeEmail(targetEmail);
        this.secretHash = secretHash;
        this.expiresAt = expiresAt;
        this.deliveryProvider = deliveryProvider;
    }

    public Long getId() { return id; }
    public UUID getGlobalId() { return globalId; }
    public Account getAccount() { return account; }
    public String getTargetEmail() { return targetEmail; }
    public String getSecretHash() { return secretHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public String getDeliveryProvider() { return deliveryProvider; }
    public String getDeliveryMessageId() { return deliveryMessageId; }

    public boolean isUsableAt(Instant now) {
        return consumedAt == null && revokedAt == null && expiresAt.isAfter(now);
    }

    public void consume(Instant now) {
        this.consumedAt = now;
    }

    public void revoke(Instant now) {
        if (consumedAt == null && revokedAt == null) {
            this.revokedAt = now;
        }
    }

    public void recordDelivery(String messageId) {
        this.deliveryMessageId = messageId;
    }
}
