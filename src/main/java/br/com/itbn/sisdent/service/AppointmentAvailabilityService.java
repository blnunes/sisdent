package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.AppointmentAvailabilityResponse;
import br.com.itbn.sisdent.model.*;
import br.com.itbn.sisdent.repository.*;
import java.time.*; import java.util.*; import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.server.ResponseStatusException;

@Service
public class AppointmentAvailabilityService {
 private final ClinicUnitWorkingHoursRepository hours; private final ClinicUnitBreakRepository breaks; private final AppointmentBlockedPeriodRepository blocked; private final AppointmentRepository appointments; private final PractitionerRepository practitioners; private final ScopeAuthorizationService authorization;
 public AppointmentAvailabilityService(ClinicUnitWorkingHoursRepository h,ClinicUnitBreakRepository b,AppointmentBlockedPeriodRepository x,AppointmentRepository a,PractitionerRepository p,ScopeAuthorizationService z){hours=h;breaks=b;blocked=x;appointments=a;practitioners=p;authorization=z;}
 @Transactional(readOnly=true) public List<AppointmentAvailabilityResponse> list(UUID org,UUID clinicId,Instant from,Instant to,List<UUID> practitionerIds){
  authorization.requireAppointmentRead(org,clinicId); ClinicUnit clinic=authorization.requireClinicInOrganization(org,clinicId); validRange(from,to); List<Practitioner> selected=selected(org,practitionerIds);
  return selected.stream().flatMap(p->intervals(org,clinic,p,from,to).stream()).toList();
 }
 public void requireAvailable(UUID org,ClinicUnit clinic,Practitioner practitioner,Instant from,Instant to,Long ignoredAppointmentId){
  validRange(from,to); if (!ZoneId.of(clinic.getTimezone()).getId().equals(clinic.getTimezone())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A valid IANA timezone is required");
  if (!withinWorkingHours(clinic,from,to) || overlapsBreak(clinic,from,to) || !blocked.findOverlapping(org,clinic.getGlobalId(),practitioner.getGlobalId(),from,to).isEmpty() || appointments.hasOverlap(practitioner.getId(),from,to,ignoredAppointmentId)) throw new SchedulingConflictException();
 }
 private List<Practitioner> selected(UUID org,List<UUID> ids){
  if(ids==null||ids.isEmpty()) return practitioners.findAllByOrganization_GlobalIdOrderByDisplayName(org).stream().filter(Practitioner::isActive).toList();
  return ids.stream().distinct().map(id->practitioners.findByGlobalIdAndOrganization_GlobalId(id,org).filter(Practitioner::isActive).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND))).toList();
 }
 private List<AppointmentAvailabilityResponse> intervals(UUID org,ClinicUnit clinic,Practitioner p,Instant from,Instant to){
  List<AppointmentAvailabilityResponse> result=new ArrayList<>(); ZoneId zone=ZoneId.of(clinic.getTimezone()); LocalDate day=from.atZone(zone).toLocalDate(); LocalDate last=to.minusNanos(1).atZone(zone).toLocalDate();
  while(!day.isAfter(last)){ for(ClinicUnitWorkingHours h:hours.findAllByClinicUnit_Id(clinic.getId())) if(h.getDayOfWeek()==day.getDayOfWeek().getValue()){ Instant s=bound(day,h.getStartMinute(),zone),e=bound(day,h.getEndMinute(),zone); add(result,p,s,e,from,to,AppointmentAvailabilityResponse.Availability.AVAILABLE,AppointmentAvailabilityResponse.Category.WORKING_HOURS); } day=day.plusDays(1); }
  for(ClinicUnitBreak b:breaks.findAllByClinicUnit_Id(clinic.getId())) { day=from.atZone(zone).toLocalDate(); while(!day.isAfter(last)){if(b.getDayOfWeek()==day.getDayOfWeek().getValue())add(result,p,bound(day,b.getStartMinute(),zone),bound(day,b.getEndMinute(),zone),from,to,AppointmentAvailabilityResponse.Availability.UNAVAILABLE,AppointmentAvailabilityResponse.Category.BREAK);day=day.plusDays(1);} }
  blocked.findOverlapping(org,clinic.getGlobalId(),p.getGlobalId(),from,to).forEach(x->add(result,p,x.getStartAt(),x.getEndAt(),from,to,AppointmentAvailabilityResponse.Availability.UNAVAILABLE,AppointmentAvailabilityResponse.Category.BLOCKED));
  appointments.findScheduledOverlapping(org,clinic.getGlobalId(),p.getGlobalId(),from,to).forEach(a->add(result,p,a.getStartAt(),a.getEndAt(),from,to,AppointmentAvailabilityResponse.Availability.UNAVAILABLE,AppointmentAvailabilityResponse.Category.OCCUPIED));
  return result;
 }
 private boolean withinWorkingHours(ClinicUnit c,Instant start,Instant end){ ZoneId z=ZoneId.of(c.getTimezone()); LocalDate day=start.atZone(z).toLocalDate(); LocalDate last=end.minusNanos(1).atZone(z).toLocalDate(); List<ClinicUnitWorkingHours> configured=hours.findAllByClinicUnit_Id(c.getId()); if(configured.isEmpty()) return true; if(!day.equals(last))return false; return configured.stream().filter(h->h.getDayOfWeek()==day.getDayOfWeek().getValue()).anyMatch(h->!start.isBefore(bound(day,h.getStartMinute(),z))&&!end.isAfter(bound(day,h.getEndMinute(),z))); }
 private boolean overlapsBreak(ClinicUnit c,Instant start,Instant end){ZoneId z=ZoneId.of(c.getTimezone()); LocalDate day=start.atZone(z).toLocalDate(); LocalDate last=end.minusNanos(1).atZone(z).toLocalDate(); for(;!day.isAfter(last);day=day.plusDays(1))for(ClinicUnitBreak b:breaks.findAllByClinicUnit_Id(c.getId()))if(b.getDayOfWeek()==day.getDayOfWeek().getValue()&&start.isBefore(bound(day,b.getEndMinute(),z))&&end.isAfter(bound(day,b.getStartMinute(),z)))return true;return false;}
 private Instant bound(LocalDate d,int minute,ZoneId z){return (minute==1440?d.plusDays(1).atStartOfDay(z):d.atStartOfDay(z).plusMinutes(minute)).toInstant();}
 private void add(List<AppointmentAvailabilityResponse> r,Practitioner p,Instant s,Instant e,Instant from,Instant to,AppointmentAvailabilityResponse.Availability a,AppointmentAvailabilityResponse.Category c){s=s.isBefore(from)?from:s;e=e.isAfter(to)?to:e;if(e.isAfter(s))r.add(new AppointmentAvailabilityResponse(p.getGlobalId(),s,e,a,c));}
 private void validRange(Instant from,Instant to){if(from==null||to==null||!to.isAfter(from))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Appointment end must be after start");}
}
