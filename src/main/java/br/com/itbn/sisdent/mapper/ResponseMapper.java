package br.com.itbn.sisdent.mapper;

import br.com.itbn.sisdent.dto.AddressResponse;
import br.com.itbn.sisdent.dto.AdministrativeDivisionResponse;
import br.com.itbn.sisdent.dto.CountryResponse;
import br.com.itbn.sisdent.dto.PatientResponse;
import br.com.itbn.sisdent.dto.DentalProcedureResponse;
import br.com.itbn.sisdent.dto.SpecialityResponse;
import br.com.itbn.sisdent.model.AdministrativeDivision;
import br.com.itbn.sisdent.model.Address;
import br.com.itbn.sisdent.model.CatalogStatus;
import br.com.itbn.sisdent.model.Country;
import br.com.itbn.sisdent.model.DentalProcedure;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.model.Speciality;

import java.util.Comparator;

public final class ResponseMapper {

    private ResponseMapper() {
    }

    public static AdministrativeDivisionResponse toResponse(AdministrativeDivision division) {
        return new AdministrativeDivisionResponse(
                division.getId(),
                division.getName(),
                division.getCode(),
                division.getType(),
                toResponse(division.getCountry()));
    }

    public static CountryResponse toResponse(Country country) {
        return new CountryResponse(
                country.getId(),
                country.getName(),
                country.getCode(),
                country.getContinent());
    }

    public static SpecialityResponse toResponse(Speciality speciality) {
        return new SpecialityResponse(
                speciality.getId(),
                speciality.getName(),
                speciality.getStatus(),
                speciality.getProcedures().stream()
                        .filter(procedure -> procedure.getStatus() == CatalogStatus.ACTIVE)
                        .sorted(Comparator.comparing(DentalProcedure::getName))
                        .map(ResponseMapper::toResponse)
                        .toList());
    }

    public static DentalProcedureResponse toResponse(DentalProcedure procedure) {
        return new DentalProcedureResponse(procedure.getId(), procedure.getName(), procedure.getStatus());
    }

    public static AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getStreet(),
                address.getDistrict(),
                address.getCity(),
                address.getAdditionalInfo(),
                address.getBlock(),
                address.getPostalCode(),
                address.getAdministrativeDivision() == null
                        ? null
                        : toResponse(address.getAdministrativeDivision()),
                toResponse(address.getCountry()));
    }

    public static PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getGlobalId(),
                patient.getName(),
                patient.getBirthDate(),
                patient.isActive(),
                patient.getGender(),
                patient.getTaxId(),
                patient.getIdentificationType(),
                patient.getIdentificationNumber(),
                toResponse(patient.getDocumentIssuerCountry()),
                toResponse(patient.getNationality()),
                toResponse(patient.getAddress()),
                patient.getSpecialities().stream()
                        .sorted(Comparator.comparing(Speciality::getName))
                        .map(ResponseMapper::toResponse)
                        .toList());
    }
}
