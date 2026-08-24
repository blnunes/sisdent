package br.com.itbn.sisdent.model;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "appointments")
public class Appointment extends AuditableEntity {
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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_link_id")
    private PatientOrganizationLink patientLink;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "practitioner_id")
    private Practitioner practitioner;
    private Instant startAt;
    private Instant endAt;
    private String schedulingTimezone;
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    protected Appointment() {
    }

    public Appointment(Organization o, ClinicUnit c, PatientOrganizationLink p, Practitioner r, Instant s, Instant e, String z) {
        organization = o;
        clinicUnit = c;
        patientLink = p;
        practitioner = r;
        startAt = s;
        endAt = e;
        schedulingTimezone = z;
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

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public String getSchedulingTimezone() {
        return schedulingTimezone;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void reschedule(Instant s, Instant e, String z) {
        requireScheduled();
        startAt = s;
        endAt = e;
        schedulingTimezone = z;
    }

    public void transition(AppointmentStatus target) {
        requireScheduled();
        status = target;
    }

    private void requireScheduled() {
        if (status != AppointmentStatus.SCHEDULED)
            throw new IllegalStateException("Appointment is in a terminal state");
    }
}
