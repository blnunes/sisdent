package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.*;
import br.com.itbn.sisdent.pagination.PageQuery;
import br.com.itbn.sisdent.service.AccountManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
public class AccountManagementController {
    private final AccountManagementService service;
    public AccountManagementController(AccountManagementService service) { this.service = service; }
    @GetMapping("/api/account") public AccountResponse current() { return service.currentAccount(); }
    @GetMapping("/api/platform/accounts") public PageResponse<AccountResponse> platformPage(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort, @RequestParam(required = false) String direction,
            @RequestParam(required = false) String filter) { return service.platformPage(new PageQuery(page,size,sort,direction), filter); }
    @PostMapping("/api/platform/accounts") public ResponseEntity<AccountResponse> create(@Valid @RequestBody AccountCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request)); }
    @GetMapping("/api/platform/accounts/{accountId}") public AccountResponse platformRead(@PathVariable UUID accountId) { return service.platformRead(accountId); }
    @PatchMapping("/api/platform/accounts/{accountId}/lifecycle") public AccountResponse lifecycle(@PathVariable UUID accountId, @Valid @RequestBody AccountLifecycleRequest request) { return service.changeLifecycle(accountId, request); }
    @GetMapping("/api/organizations/{organizationId}/accounts") public PageResponse<AccountResponse> organizationPage(@PathVariable UUID organizationId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort, @RequestParam(required = false) String direction,
            @RequestParam(required = false) String filter) { return service.organizationPage(organizationId, new PageQuery(page,size,sort,direction), filter); }
    @GetMapping("/api/organizations/{organizationId}/accounts/{accountId}") public AccountResponse organizationRead(@PathVariable UUID organizationId, @PathVariable UUID accountId) { return service.organizationRead(organizationId, accountId); }
}
