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

import java.util.UUID;

@Entity
@Table(name = "memberships")
public class Membership extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "global_id", nullable = false, unique = true, updatable = false)
    private UUID globalId = UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_unit_id")
    private ClinicUnit clinicUnit;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MembershipRole role;
    @Column(nullable = false)
    private boolean active = true;

    protected Membership() {}
    public Membership(Account account, Organization organization, ClinicUnit clinicUnit, MembershipRole role) {
        this.account = account; this.organization = organization; this.clinicUnit = clinicUnit; this.role = role;
    }
    public Long getId() { return id; }
    public UUID getGlobalId() { return globalId; }
    public Account getAccount() { return account; }
    public Organization getOrganization() { return organization; }
    public ClinicUnit getClinicUnit() { return clinicUnit; }
    public MembershipRole getRole() { return role; }
    public boolean isActive() { return active; }
    public void revoke() { active = false; }
    public void changeRole(MembershipRole role) { this.role = role; }
}
