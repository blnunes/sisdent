package br.com.itbn.sisdent.repository;
import br.com.itbn.sisdent.model.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.time.*; import java.util.*;
public interface AppointmentRepository extends JpaRepository<Appointment,Long> {
 boolean existsByOrganization_Id(Long organizationId);
 @EntityGraph(attributePaths={"organization","clinicUnit","patientLink","patientLink.patient","practitioner"}) @Query("select a from Appointment a where a.organization.globalId=:organizationId and (:clinicUnitId is null or a.clinicUnit.globalId=:clinicUnitId) and a.startAt < :to and a.endAt > :from") Page<Appointment> findScoped(UUID organizationId,UUID clinicUnitId,Instant from,Instant to,Pageable page);
 @EntityGraph(attributePaths={"organization","clinicUnit","patientLink","patientLink.patient","practitioner"}) Optional<Appointment> findByGlobalIdAndOrganization_GlobalId(UUID id,UUID organizationId);
 @Query("select count(a)>0 from Appointment a where a.practitioner.id=:practitionerId and a.status='SCHEDULED' and a.startAt < :end and a.endAt > :start and (:excludeId is null or a.id<>:excludeId)") boolean hasOverlap(@Param("practitionerId")Long practitionerId,@Param("start")Instant start,@Param("end")Instant end,@Param("excludeId")Long excludeId);
}
