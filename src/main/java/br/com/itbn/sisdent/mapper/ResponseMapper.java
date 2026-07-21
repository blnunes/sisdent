package br.com.itbn.sisdent.mapper;

import br.com.itbn.sisdent.dto.AddressResponse;
import br.com.itbn.sisdent.dto.PatientResponse;
import br.com.itbn.sisdent.dto.StateResponse;
import br.com.itbn.sisdent.model.Address;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.model.State;

public final class ResponseMapper {

    private ResponseMapper() {
    }

    public static StateResponse toResponse(State state) {
        return new StateResponse(state.getId(), state.getName(), state.getAbbreviation());
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
                toResponse(patient.getAddress()));
    }
}
