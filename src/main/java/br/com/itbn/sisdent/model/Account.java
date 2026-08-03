package br.com.itbn.sisdent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "global_id", nullable = false, unique = true, updatable = false)
    private UUID globalId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legacy_user_id", unique = true)
    private User legacyUser;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "platform_administrator", nullable = false)
    private boolean platformAdministrator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_management_organization_id")
    private Organization accountManagementOrganization;

    @Column(name = "email_migration_required", nullable = false)
    private boolean emailMigrationRequired;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "pending_email", unique = true, length = 320)
    private String pendingEmail;

    protected Account() {
    }

    public Account(Person person, User legacyUser, String email, String password,
            boolean platformAdministrator, boolean emailMigrationRequired) {
        this.person = person;
        this.legacyUser = legacyUser;
        this.email = normalizeEmail(email);
        this.password = password;
        this.platformAdministrator = platformAdministrator;
        this.emailMigrationRequired = emailMigrationRequired;
        this.emailVerified = !emailMigrationRequired;
    }

    public static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    public Long getId() { return id; }
    public UUID getGlobalId() { return globalId; }
    public Person getPerson() { return person; }
    public User getLegacyUser() { return legacyUser; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public boolean isActive() { return active; }
    public boolean isPlatformAdministrator() { return platformAdministrator; }
    public Organization getAccountManagementOrganization() { return accountManagementOrganization; }
    public boolean isEmailMigrationRequired() { return emailMigrationRequired; }
    public boolean isEmailVerified() { return emailVerified; }
    public String getPendingEmail() { return pendingEmail; }

    public void beginEmailEnrollment(String pendingEmail) {
        if (!emailMigrationRequired || emailVerified) {
            throw new IllegalStateException("Verified email replacement is not supported");
        }
        this.pendingEmail = normalizeEmail(pendingEmail);
    }

    public void completeEmailVerification(String verifiedEmail) {
        String normalizedEmail = normalizeEmail(verifiedEmail);
        if (!emailMigrationRequired || emailVerified || !normalizedEmail.equals(pendingEmail)) {
            throw new IllegalStateException("Email verification state is no longer valid");
        }
        this.email = normalizedEmail;
        this.emailVerified = true;
        this.emailMigrationRequired = false;
        this.pendingEmail = null;
    }

    public void changeActive(boolean active) {
        if (this.active == active) {
            throw new IllegalStateException("Account is already in the requested lifecycle state");
        }
        this.active = active;
    }

    public void changePlatformAdministrator(boolean platformAdministrator) {
        if (this.platformAdministrator == platformAdministrator) {
            throw new IllegalStateException("Account already has the requested platform-administrator state");
        }
        this.platformAdministrator = platformAdministrator;
    }

    public void assignAccountManagementOrganizationIfAbsent(Organization organization) {
        if (accountManagementOrganization == null) accountManagementOrganization = organization;
    }
}
