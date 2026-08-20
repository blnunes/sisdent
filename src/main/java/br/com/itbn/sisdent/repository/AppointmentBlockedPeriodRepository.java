package br.com.itbn.sisdent.repository;
import br.com.itbn.sisdent.model.AppointmentBlockedPeriod; import org.springframework.data.jpa.repository.*; import java.time.*; import java.util.*;
public interface AppointmentBlockedPeriodRepository extends JpaRepository<AppointmentBlockedPeriod,Long>{
 @Query("select b from AppointmentBlockedPeriod b where b.organization.globalId=:organizationId and b.clinicUnit.globalId=:clinicId and b.startAt < :end and b.endAt > :start and (b.practitioner is null or b.practitioner.globalId=:practitionerId)") List<AppointmentBlockedPeriod> findOverlapping(UUID organizationId,UUID clinicId,UUID practitionerId,Instant start,Instant end);
}
