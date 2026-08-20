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
import br.com.itbn.sisdent.localization.PreferredLanguage;

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

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "platform_administrator", nullable = false)
    private boolean platformAdministrator;

    @Column(name = "preferred_language", nullable = false, length = 5)
    private String preferredLanguage = PreferredLanguage.DEFAULT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_management_organization_id")
    private Organization accountManagementOrganization;

    protected Account() {
    }

    public Account(Person person, String email, String password, boolean platformAdministrator) {
        this.person = person;
        this.email = normalizeEmail(email);
        this.password = password;
        this.platformAdministrator = platformAdministrator;
    }

    public static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    public Long getId() { return id; }
    public UUID getGlobalId() { return globalId; }
    public Person getPerson() { return person; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public boolean isActive() { return active; }
    public boolean isPlatformAdministrator() { return platformAdministrator; }
    public String getPreferredLanguage() { return preferredLanguage == null ? PreferredLanguage.DEFAULT : preferredLanguage; }
    public Organization getAccountManagementOrganization() { return accountManagementOrganization; }
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

    public void changePassword(String password) {
        this.password = password;
    }

    public void changePreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = PreferredLanguage.require(preferredLanguage);
    }

    public void assignAccountManagementOrganizationIfAbsent(Organization organization) {
        if (accountManagementOrganization == null) accountManagementOrganization = organization;
    }
}
