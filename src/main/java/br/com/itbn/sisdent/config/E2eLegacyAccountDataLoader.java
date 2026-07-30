package br.com.itbn.sisdent.config;

import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.AccountEmailClaim;
import br.com.itbn.sisdent.model.EmailClaimType;
import br.com.itbn.sisdent.model.IdentificationType;
import br.com.itbn.sisdent.model.Membership;
import br.com.itbn.sisdent.model.MembershipRole;
import br.com.itbn.sisdent.model.Organization;
import br.com.itbn.sisdent.model.Person;
import br.com.itbn.sisdent.model.Role;
import br.com.itbn.sisdent.model.User;
import br.com.itbn.sisdent.repository.AccountEmailClaimRepository;
import br.com.itbn.sisdent.repository.AccountRepository;
import br.com.itbn.sisdent.repository.MembershipRepository;
import br.com.itbn.sisdent.repository.OrganizationRepository;
import br.com.itbn.sisdent.repository.PersonRepository;
import br.com.itbn.sisdent.repository.UserRepository;
import br.com.itbn.sisdent.service.IdentificationNumbers;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("e2e")
public class E2eLegacyAccountDataLoader implements ApplicationRunner {
    public static final String IDENTIFICATION_NUMBER = "E2ELEGACY";
    public static final String PASSWORD = "e2e-password";

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final AccountRepository accountRepository;
    private final AccountEmailClaimRepository emailClaimRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    public E2eLegacyAccountDataLoader(
            UserRepository userRepository,
            PersonRepository personRepository,
            AccountRepository accountRepository,
            AccountEmailClaimRepository emailClaimRepository,
            OrganizationRepository organizationRepository,
            MembershipRepository membershipRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.accountRepository = accountRepository;
        this.emailClaimRepository = emailClaimRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (accountRepository
                .findByLegacyUser_IdentificationTypeAndLegacyUser_IdentificationNumber(
                        IdentificationType.PASSPORT, IDENTIFICATION_NUMBER)
                .isPresent()) {
            return;
        }
        User user = userRepository.save(new User(
                IdentificationType.PASSPORT,
                IdentificationNumbers.normalize(IDENTIFICATION_NUMBER),
                passwordEncoder.encode(PASSWORD),
                Role.USER,
                Role.USER.defaultPermissions()));
        Person person = personRepository.save(new Person("E2E legacy user"));
        String syntheticEmail = "passport.e2elegacy@legacy.sisdent.invalid";
        Account account = accountRepository.save(new Account(
                person, user, syntheticEmail, user.getPassword(), false, true));
        emailClaimRepository.save(new AccountEmailClaim(
                account, syntheticEmail, EmailClaimType.VERIFIED));
        Organization organization = organizationRepository.findAll().stream().findFirst()
                .orElseGet(() -> organizationRepository.save(
                        new Organization("E2E Organization")));
        membershipRepository.save(new Membership(
                account, organization, null, MembershipRole.READ_ONLY));
    }
}
