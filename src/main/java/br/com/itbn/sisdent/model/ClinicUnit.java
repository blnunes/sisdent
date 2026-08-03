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

import java.util.UUID;

@Entity
@Table(name = "clinic_units")
public class ClinicUnit extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "global_id", nullable = false, unique = true, updatable = false)
    private UUID globalId = UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private boolean active = true;

    protected ClinicUnit() {}
    public ClinicUnit(Organization organization, String name) {
        this.organization = organization;
        this.name = name.trim();
    }
    public Long getId() { return id; }
    public UUID getGlobalId() { return globalId; }
    public Organization getOrganization() { return organization; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}
