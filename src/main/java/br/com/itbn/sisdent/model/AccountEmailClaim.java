package br.com.itbn.sisdent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "account_email_claims", uniqueConstraints = {
        @UniqueConstraint(name = "uk_account_email_claims_email", columnNames = "normalized_email"),
        @UniqueConstraint(name = "uk_account_email_claims_account_type", columnNames = {"account_id", "claim_type"})
})
public class AccountEmailClaim extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "normalized_email", nullable = false, length = 320)
    private String normalizedEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_type", nullable = false, length = 16)
    private EmailClaimType claimType;

    protected AccountEmailClaim() {
    }

    public AccountEmailClaim(Account account, String normalizedEmail, EmailClaimType claimType) {
        this.account = account;
        this.normalizedEmail = Account.normalizeEmail(normalizedEmail);
        this.claimType = claimType;
    }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public String getNormalizedEmail() { return normalizedEmail; }
    public EmailClaimType getClaimType() { return claimType; }

    public void markVerified() {
        this.claimType = EmailClaimType.VERIFIED;
    }
}
