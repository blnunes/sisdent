package br.com.itbn.sisdent.dto; import java.time.*; import java.util.*;
public record PerformedProcedureResponse(UUID globalId,Long dentalProcedureId,String procedureNameSnapshot,Instant performedAt,String administrativeNote,Instant voidedAt,String voidedBy,String voidReason) {}
