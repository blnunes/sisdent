package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.AddressResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.service.AddressService;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class AddressQueryController {
    private final AddressService addresses;

    public AddressQueryController(AddressService addresses) {
        this.addresses = addresses;
    }

    @QueryMapping
    public PageResponse<AddressResponse> addresses(@Argument CataloguePageInput page) {
        return addresses.findPage(page == null
                ? new CataloguePageInput(null, null, null, null).toPageQuery()
                : page.toPageQuery());
    }

    @QueryMapping
    public List<AddressResponse> addressesByPostalCode(@Argument String countryCode, @Argument String postalCode) {
        return addresses.findByPostalCode(countryCode, postalCode);
    }

    @QueryMapping
    public List<AddressResponse> addressPostalCodeSuggestions(@Argument String countryCode, @Argument String query) {
        return addresses.suggestByPostalCode(countryCode, query);
    }
}
