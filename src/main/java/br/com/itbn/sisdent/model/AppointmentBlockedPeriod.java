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
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "appointment_blocked_periods")
public class AppointmentBlockedPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "global_id", nullable = false, unique = true, updatable = false)
    private UUID globalId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_unit_id")
    private ClinicUnit clinicUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id")
    private Practitioner practitioner;

    @Column(nullable = false)
    private Instant startAt;

    @Column(nullable = false)
    private Instant endAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected AppointmentBlockedPeriod() {
    }

    public AppointmentBlockedPeriod(
            Organization organization,
            ClinicUnit clinicUnit,
            Practitioner practitioner,
            Instant startAt,
            Instant endAt) {
        this.organization = organization;
        this.clinicUnit = clinicUnit;
        this.practitioner = practitioner;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public UUID getGlobalId() {
        return globalId;
    }

    public ClinicUnit getClinicUnit() {
        return clinicUnit;
    }

    public Practitioner getPractitioner() {
        return practitioner;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public long getVersion() {
        return version;
    }

    public void update(Practitioner practitioner, Instant startAt, Instant endAt) {
        this.practitioner = practitioner;
        this.startAt = startAt;
        this.endAt = endAt;
    }
}
