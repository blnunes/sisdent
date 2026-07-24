package br.com.itbn.sisdent.dto;

import java.util.List;

public record SpecialityResponse(
        Long id,
        String name,
        List<ProcedureResponse> procedures) {
}
