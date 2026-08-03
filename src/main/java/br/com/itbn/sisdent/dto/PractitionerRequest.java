package br.com.itbn.sisdent.dto; import jakarta.validation.constraints.*; import java.util.*;
public record PractitionerRequest(@NotBlank @Size(max=255) String displayName,@Size(max=128) String registrationNumber,UUID accountId,@NotNull Set<Long> specialityIds) {}
