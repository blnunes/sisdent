package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.AddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressMutationInput(
        @NotBlank String street,
        String district,
        @NotBlank String city,
        String additionalInfo,
        String block,
        @NotBlank @Size(max = 20) String postalCode,
        @Valid AdministrativeDivisionReferenceInput administrativeDivision,
        @NotBlank @Pattern(regexp = "[A-Z]{2}") String countryCode) {

    AddressRequest toRequest() {
        return new AddressRequest(street, district, city, additionalInfo, block, postalCode,
                administrativeDivision == null ? null : administrativeDivision.toRequest(), countryCode);
    }

    public record AdministrativeDivisionReferenceInput(
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(max = 32) String code,
            @NotBlank @Size(max = 32) String type) {
        AddressRequest.AdministrativeDivisionReference toRequest() {
            return new AddressRequest.AdministrativeDivisionReference(name, code, type);
        }
    }
}
