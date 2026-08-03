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
@Table(name = "patient_organization_links")
public class PatientOrganizationLink extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "global_id", nullable = false, unique = true, updatable = false)
    private UUID globalId = UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_unit_id")
    private ClinicUnit clinicUnit;
    @Enumerated(EnumType.STRING)
    @Column(name = "operational_basis", nullable = false, length = 32)
    private PatientLinkBasis operationalBasis;
    @Column(nullable = false)
    private boolean active = true;

    protected PatientOrganizationLink() {}
    public PatientOrganizationLink(Patient patient, Organization organization, ClinicUnit clinicUnit,
            PatientLinkBasis operationalBasis) {
        this.patient = patient; this.organization = organization; this.clinicUnit = clinicUnit;
        this.operationalBasis = operationalBasis;
    }
    public Long getId() { return id; }
    public UUID getGlobalId() { return globalId; }
    public Patient getPatient() { return patient; }
    public Organization getOrganization() { return organization; }
    public ClinicUnit getClinicUnit() { return clinicUnit; }
    public PatientLinkBasis getOperationalBasis() { return operationalBasis; }
    public boolean isActive() { return active; }
}
