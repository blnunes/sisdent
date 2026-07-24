package br.com.itbn.sisdent.mapper;

import br.com.itbn.sisdent.dto.AddressResponse;
import br.com.itbn.sisdent.dto.PatientResponse;
import br.com.itbn.sisdent.dto.ProcedureResponse;
import br.com.itbn.sisdent.dto.StateResponse;
import br.com.itbn.sisdent.dto.SpecialityResponse;
import br.com.itbn.sisdent.model.Address;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.model.Procedure;
import br.com.itbn.sisdent.model.State;
import br.com.itbn.sisdent.model.Speciality;

import java.util.Comparator;

public final class ResponseMapper {

    private ResponseMapper() {
    }

    public static StateResponse toResponse(State state) {
        return new StateResponse(state.getId(), state.getName(), state.getAbbreviation());
    }

    public static SpecialityResponse toResponse(Speciality speciality) {
        return new SpecialityResponse(
                speciality.getId(),
                speciality.getName(),
                speciality.getProcedures().stream()
                        .sorted(Comparator.comparing(Procedure::getName))
                        .map(ResponseMapper::toResponse)
                        .toList());
    }

    public static ProcedureResponse toResponse(Procedure procedure) {
        return new ProcedureResponse(procedure.getId(), procedure.getName());
    }

    public static AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getStreet(),
                address.getDistrict(),
                address.getAdditionalInfo(),
                address.getBlock(),
                address.getPostalCode(),
                toResponse(address.getState()));
    }

    public static PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getName(),
                patient.getBirthDate(),
                patient.isActive(),
                patient.getGender(),
                patient.getTaxId(),
                toResponse(patient.getAddress()),
                patient.getSpecialities().stream()
                        .sorted(Comparator.comparing(Speciality::getName))
                        .map(ResponseMapper::toResponse)
                        .toList());
    }
}
