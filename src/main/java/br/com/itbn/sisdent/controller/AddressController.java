package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.AddressResponse;
import br.com.itbn.sisdent.dto.AddressRequest;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.service.AddressService;
import br.com.itbn.sisdent.pagination.PageQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public PageResponse<AddressResponse> findAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {
        return addressService.findPage(new PageQuery(page, size, sort, direction));
    }

    @GetMapping("/postal-code/{postalCode}")
    public ResponseEntity<AddressResponse> findByPostalCode(@PathVariable String postalCode) {
        return addressService.findByPostalCode(postalCode)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse create(@Valid @RequestBody AddressRequest request) { return addressService.create(request); }
    @PutMapping("/{id}")
    public AddressResponse update(@PathVariable Long id, @Valid @RequestBody AddressRequest request) { return addressService.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { addressService.delete(id); }
}
