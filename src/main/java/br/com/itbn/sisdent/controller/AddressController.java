package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.AddressResponse;
import br.com.itbn.sisdent.service.AddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public List<AddressResponse> findAll() {
        return addressService.findAll();
    }

    @GetMapping("/postal-code/{postalCode}")
    public ResponseEntity<AddressResponse> findByPostalCode(@PathVariable String postalCode) {
        return addressService.findByPostalCode(postalCode)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
