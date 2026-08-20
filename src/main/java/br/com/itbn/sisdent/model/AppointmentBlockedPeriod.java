package br.com.itbn.sisdent.model;
import jakarta.persistence.*; import java.time.*; import java.util.*;
@Entity @Table(name="appointment_blocked_periods") public class AppointmentBlockedPeriod {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="global_id",nullable=false,unique=true,updatable=false) private UUID globalId=UUID.randomUUID();
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="organization_id") private Organization organization;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="clinic_unit_id") private ClinicUnit clinicUnit;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="practitioner_id") private Practitioner practitioner;
 private Instant startAt; private Instant endAt;
 protected AppointmentBlockedPeriod(){} public AppointmentBlockedPeriod(Organization o,ClinicUnit c,Practitioner p,Instant s,Instant e){organization=o;clinicUnit=c;practitioner=p;startAt=s;endAt=e;}
 public Practitioner getPractitioner(){return practitioner;} public Instant getStartAt(){return startAt;} public Instant getEndAt(){return endAt;}
}
