package br.com.itbn.sisdent.dto; import br.com.itbn.sisdent.model.AppointmentStatus; import java.time.*; import java.util.*;
public record AppointmentResponse(UUID globalId,UUID clinicUnitId,UUID patientId,String patientName,UUID practitionerId,String practitionerName,Instant startAt,Instant endAt,String schedulingTimezone,AppointmentStatus status) {}
