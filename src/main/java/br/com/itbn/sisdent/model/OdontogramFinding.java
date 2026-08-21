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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "odontogram_findings")
public class OdontogramFinding extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "global_id", nullable = false, unique = true, updatable = false)
    private UUID globalId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ClinicUnit clinicUnit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PatientOrganizationLink patientLink;

    @ManyToOne(fetch = FetchType.LAZY)
    private Practitioner practitioner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_for_id")
    private OdontogramFinding replacementFor;

    private String toothCode;

    @Enumerated(EnumType.STRING)
    private OdontogramSurface surface;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_code")
    private OdontogramCondition condition;

    private Instant observedAt;

    private String observationTimezone;

    @Column(name = "clinical_note", length = 500)
    private String clinicalNote;

    private Instant voidedAt;

    private String voidedBy;

    @Column(length = 500)
    private String voidReason;

    protected OdontogramFinding() {
    }

    public OdontogramFinding(FindingContext context, Observation observation) {
        organization = context.organization();
        clinicUnit = context.clinicUnit();
        patientLink = context.patientLink();
        practitioner = context.practitioner();
        replacementFor = context.replacementFor();
        toothCode = observation.toothCode();
        surface = observation.surface();
        condition = observation.condition();
        observedAt = observation.observedAt();
        observationTimezone = observation.timezone();
        clinicalNote = observation.clinicalNote();
    }

    public record FindingContext(
            Organization organization,
            ClinicUnit clinicUnit,
            PatientOrganizationLink patientLink,
            Practitioner practitioner,
            OdontogramFinding replacementFor) {
    }

    public record Observation(
            String toothCode,
            OdontogramSurface surface,
            OdontogramCondition condition,
            Instant observedAt,
            String timezone,
            String clinicalNote) {
    }

    public Long getId() {
        return id;
    }

    public UUID getGlobalId() {
        return globalId;
    }

    public Organization getOrganization() {
        return organization;
    }

    public ClinicUnit getClinicUnit() {
        return clinicUnit;
    }

    public PatientOrganizationLink getPatientLink() {
        return patientLink;
    }

    public Practitioner getPractitioner() {
        return practitioner;
    }

    public OdontogramFinding getReplacementFor() {
        return replacementFor;
    }

    public String getToothCode() {
        return toothCode;
    }

    public OdontogramSurface getSurface() {
        return surface;
    }

    public OdontogramCondition getCondition() {
        return condition;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public String getObservationTimezone() {
        return observationTimezone;
    }

    public String getClinicalNote() {
        return clinicalNote;
    }

    public Instant getVoidedAt() {
        return voidedAt;
    }

    public String getVoidedBy() {
        return voidedBy;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public boolean isVoided() {
        return voidedAt != null;
    }

    public void voidRecord(String reason, String actor) {
        if (isVoided()) {
            throw new IllegalStateException();
        }
        voidedAt = Instant.now();
        voidedBy = actor;
        voidReason = reason.strip();
    }
}
