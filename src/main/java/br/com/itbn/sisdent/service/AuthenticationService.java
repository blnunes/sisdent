package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.LoginRequest;
import br.com.itbn.sisdent.dto.TokenResponse;
import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.AccountEmailClaim;
import br.com.itbn.sisdent.model.EmailClaimType;
import br.com.itbn.sisdent.model.Membership;
import br.com.itbn.sisdent.model.MembershipRole;
import br.com.itbn.sisdent.model.Organization;
import br.com.itbn.sisdent.model.Person;
import br.com.itbn.sisdent.model.Role;
import br.com.itbn.sisdent.model.User;
import br.com.itbn.sisdent.repository.AccountRepository;
import br.com.itbn.sisdent.repository.AccountEmailClaimRepository;
import br.com.itbn.sisdent.repository.MembershipRepository;
import br.com.itbn.sisdent.repository.OrganizationRepository;
import br.com.itbn.sisdent.repository.PersonRepository;
import br.com.itbn.sisdent.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final AccountRepository accountRepository;
    private final AccountEmailClaimRepository emailClaimRepository;
    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthenticationService(
            AccountRepository accountRepository,
            AccountEmailClaimRepository emailClaimRepository,
            UserRepository userRepository,
            PersonRepository personRepository,
            OrganizationRepository organizationRepository,
            MembershipRepository membershipRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.accountRepository = accountRepository;
        this.emailClaimRepository = emailClaimRepository;
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenResponse authenticate(LoginRequest request) {
        boolean emailLogin = request.email() != null && !request.email().isBlank();
        Account account = findAccount(request)
                .filter(Account::isActive)
                .filter(candidate -> emailLogin
                        ? candidate.isEmailVerified()
                        : candidate.isEmailMigrationRequired())
                .filter(candidate -> passwordEncoder.matches(request.password(),
                        emailLogin || candidate.getLegacyUser() == null
                                ? candidate.getPassword()
                                : candidate.getLegacyUser().getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        return jwtService.issue(account);
    }

    private java.util.Optional<Account> findAccount(LoginRequest request) {
        if (request.email() != null && !request.email().isBlank()) {
            return accountRepository.findByEmail(Account.normalizeEmail(request.email()));
        }
        if (request.identificationType() == null
                || request.identificationNumber() == null
                || request.identificationNumber().isBlank()) {
            return java.util.Optional.empty();
        }
        java.util.Optional<Account> account = accountRepository
                .findByLegacyUser_IdentificationTypeAndLegacyUser_IdentificationNumber(
                request.identificationType(),
                IdentificationNumbers.normalize(request.identificationNumber()));
        if (account.isPresent()) {
            return account;
        }
        return userRepository.findByIdentificationTypeAndIdentificationNumber(
                        request.identificationType(),
                        IdentificationNumbers.normalize(request.identificationNumber()))
                .filter(User::isActive)
                .map(this::migrateLegacyUser);
    }

    private Account migrateLegacyUser(User user) {
        Person person = personRepository.save(new Person(
                "Legacy user " + user.getIdentificationType() + " " + user.getIdentificationNumber()));
        String email = (user.getIdentificationType() + "." + user.getIdentificationNumber()
                + "@legacy.sisdent.invalid").toLowerCase(java.util.Locale.ROOT);
        Account account = accountRepository.save(new Account(person, user, email,
                user.getPassword(), user.getRole() == Role.ADMIN, true));
        emailClaimRepository.save(new AccountEmailClaim(account, email, EmailClaimType.VERIFIED));
        Organization organization = organizationRepository.findAll().stream().findFirst()
                .orElseGet(() -> organizationRepository.save(new Organization("Legacy Sisdent Organization")));
        MembershipRole membershipRole = switch (user.getRole()) {
            case ADMIN -> MembershipRole.ORGANIZATION_ADMIN;
            case MANAGER -> MembershipRole.MANAGER;
            case USER -> MembershipRole.READ_ONLY;
        };
        membershipRepository.save(new Membership(account, organization, null, membershipRole));
        return account;
    }
}
