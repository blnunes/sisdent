package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.*;
import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.Membership;
import br.com.itbn.sisdent.model.Person;
import br.com.itbn.sisdent.pagination.PageQuery;
import br.com.itbn.sisdent.pagination.PageableFactory;
import br.com.itbn.sisdent.pagination.SortDefinition;
import br.com.itbn.sisdent.repository.AccountRepository;
import br.com.itbn.sisdent.repository.MembershipRepository;
import br.com.itbn.sisdent.repository.PersonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AccountManagementService {
    private static final SortDefinition SORTS = new SortDefinition("person.displayName",
            Set.of("person.displayName", "email", "globalId"));
    private final AccountRepository accounts;
    private final PersonRepository persons;
    private final MembershipRepository memberships;
    private final PasswordEncoder passwords;
    private final PageableFactory pages;
    private final ScopeAuthorizationService authorization;
    private final CurrentAccountService current;

    public AccountManagementService(AccountRepository accounts, PersonRepository persons,
            MembershipRepository memberships, PasswordEncoder passwords, PageableFactory pages,
            ScopeAuthorizationService authorization, CurrentAccountService current) {
        this.accounts = accounts; this.persons = persons; this.memberships = memberships;
        this.passwords = passwords; this.pages = pages; this.authorization = authorization; this.current = current;
    }

    @Transactional(readOnly = true)
    public PageResponse<AccountResponse> platformPage(PageQuery query, String filter) {
        authorization.requirePlatformAdministrator();
        return PageResponse.from(accounts.findManagementPage(normalizeFilter(filter), deterministicPage(query)),
                account -> response(account, null));
    }

    @Transactional(readOnly = true)
    public AccountResponse platformRead(UUID accountId) {
        authorization.requirePlatformAdministrator();
        return response(requireAccount(accountId), null);
    }

    @Transactional
    public AccountResponse create(AccountCreateRequest request) {
        authorization.requirePlatformAdministrator();
        String email = Account.normalizeEmail(request.email());
        if (accounts.existsByEmail(email)) throw conflict("The email address is unavailable");
        Person person = persons.save(new Person(request.displayName()));
        Account account = accounts.saveAndFlush(new Account(person, email, passwords.encode(request.password()), false));
        return response(account, null);
    }

    @Transactional
    public AccountResponse changeLifecycle(UUID accountId, AccountLifecycleRequest request) {
        authorization.requirePlatformAdministrator();
        Account account = requireAccount(accountId);
        requireVersion(account.getVersion(), request.version());
        try { account.changeActive(request.active()); }
        catch (IllegalStateException exception) { throw conflict("The requested account lifecycle transition is unavailable"); }
        return response(accounts.saveAndFlush(account), null);
    }

    @Transactional
    public AccountResponse changePlatformAdministrator(UUID accountId, AccountPlatformAdministratorRequest request) {
        authorization.requirePlatformAdministrator();
        Account account = accounts.findLockedByGlobalId(accountId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        requireVersion(account.getVersion(), request.version());
        if (!request.platformAdministrator() && account.isPlatformAdministrator() && account.isActive()
                && accounts.countByPlatformAdministratorTrueAndActiveTrue() <= 1) {
            throw conflict("At least one active platform administrator is required");
        }
        try { account.changePlatformAdministrator(request.platformAdministrator()); }
        catch (IllegalStateException exception) { throw conflict("The requested platform-administrator transition is unavailable"); }
        return response(accounts.saveAndFlush(account), null);
    }

    @Transactional(readOnly = true)
    public AccountResponse currentAccount() { return response(current.require(), null); }

    @Transactional(readOnly = true)
    public PageResponse<AccountResponse> organizationPage(UUID organizationId, PageQuery query, String filter) {
        authorization.requireAccountAdministration(organizationId);
        return PageResponse.from(accounts.findManagementPageInOrganization(organizationId, normalizeFilter(filter), deterministicPage(query)),
                account -> response(account, organizationId));
    }

    @Transactional(readOnly = true)
    public AccountResponse organizationRead(UUID organizationId, UUID accountId) {
        authorization.requireAccountAdministration(organizationId);
        Account account = accounts.findVisibleInOrganization(organizationId, accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return response(account, organizationId);
    }

    private Account requireAccount(UUID accountId) {
        return accounts.findByGlobalId(accountId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private AccountResponse response(Account account, UUID organizationId) {
        List<Membership> visible = organizationId == null
                ? memberships.findAllByAccount_IdOrderByOrganization_NameAscClinicUnit_NameAsc(account.getId())
                : memberships.findAllByAccount_IdAndOrganization_GlobalIdOrderByClinicUnit_NameAsc(account.getId(), organizationId);
        return new AccountResponse(account.getGlobalId(), account.getPerson().getDisplayName(), account.getEmail(),
                account.isActive(), account.isPlatformAdministrator(), account.getVersion(),
                visible.stream().map(OrganizationService::toResponse).toList());
    }

    private static String normalizeFilter(String filter) {
        if (filter == null || filter.isBlank()) return null;
        String value = filter.strip();
        if (value.length() > 120) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filter is too long");
        return value;
    }
    private Pageable deterministicPage(PageQuery query) {
        Pageable requested = pages.create(query, SORTS);
        Sort tieBreaker = requested.getSort().getOrderFor("globalId") == null
                ? Sort.by(Sort.Direction.ASC, "globalId") : Sort.unsorted();
        return PageRequest.of(requested.getPageNumber(), requested.getPageSize(), requested.getSort().and(tieBreaker));
    }
    private static void requireVersion(long current, long supplied) {
        if (current != supplied) throw conflict("The account was changed by another request");
    }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
