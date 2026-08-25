package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.AppointmentBlockedPeriod;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppointmentBlockedPeriodRepository extends JpaRepository<AppointmentBlockedPeriod, Long> {
    @Query("select b from AppointmentBlockedPeriod b where b.organization.globalId = :organizationId "
            + "and b.clinicUnit.globalId = :clinicId and b.startAt < :end and b.endAt > :start "
            + "and (b.practitioner is null or b.practitioner.globalId = :practitionerId)")
    List<AppointmentBlockedPeriod> findOverlapping(
            UUID organizationId,
            UUID clinicId,
            UUID practitionerId,
            Instant start,
            Instant end);

    @Query("select b from AppointmentBlockedPeriod b where b.organization.globalId = :organizationId "
            + "and b.clinicUnit.globalId = :clinicId and b.startAt < :end and b.endAt > :start "
            + "order by b.startAt")
    List<AppointmentBlockedPeriod> findScopedOverlapping(
            UUID organizationId,
            UUID clinicId,
            Instant start,
            Instant end);

    Optional<AppointmentBlockedPeriod> findByGlobalIdAndOrganization_GlobalId(UUID globalId, UUID organizationId);
}
