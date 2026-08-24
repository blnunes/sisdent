package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.AddressResponse;
import br.com.itbn.sisdent.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
public class AddressMutationController {
    private final AddressService addresses;

    public AddressMutationController(AddressService addresses) {
        this.addresses = addresses;
    }

    @MutationMapping
    public AddressResponse createAddress(@Argument @Valid AddressMutationInput input) {
        return addresses.create(input.toRequest());
    }

    @MutationMapping
    public AddressResponse updateAddress(@Argument Long id, @Argument @Valid AddressMutationInput input) {
        return addresses.update(id, input.toRequest());
    }

    @MutationMapping
    public boolean deleteAddress(@Argument Long id) {
        addresses.delete(id);
        return true;
    }
}
