package br.com.itbn.sisdent.dto; import java.util.*;
public record PractitionerResponse(UUID globalId,String displayName,String registrationNumber,UUID accountId,boolean active,Set<Long> specialityIds) {}
