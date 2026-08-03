package br.com.itbn.sisdent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import br.com.itbn.sisdent.model.IdentificationType;
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
import br.com.itbn.sisdent.service.IdentificationNumbers;

@Component
@Order(2)
public class AdminDataLoader implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final AccountEmailClaimRepository emailClaimRepository;
    private final PersonRepository personRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final IdentificationType identificationType;
    private final String identificationNumber;
    private final String password;
    private final String email;

    public AdminDataLoader(
            UserRepository userRepository,
            AccountRepository accountRepository,
            AccountEmailClaimRepository emailClaimRepository,
            PersonRepository personRepository,
            OrganizationRepository organizationRepository,
            MembershipRepository membershipRepository,
            PasswordEncoder passwordEncoder,
            @Value("${sisdent.bootstrap-admin.identification-type}") IdentificationType identificationType,
            @Value("${sisdent.bootstrap-admin.identification-number}") String identificationNumber,
            @Value("${sisdent.bootstrap-admin.password}") String password,
            @Value("${sisdent.bootstrap-admin.email}") String email) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.emailClaimRepository = emailClaimRepository;
        this.personRepository = personRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.identificationType = identificationType;
        this.identificationNumber = identificationNumber;
        this.password = password;
        this.email = email;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String normalizedNumber = IdentificationNumbers.normalize(identificationNumber);
        User user = userRepository.findByIdentificationTypeAndIdentificationNumber(
                identificationType,
                normalizedNumber)
                .map(existingUser -> {
                    if (existingUser.getRole() == Role.ADMIN) {
                        existingUser.setPermissions(Role.ADMIN.defaultPermissions());
                        return userRepository.save(existingUser);
                    }
                    return existingUser;
                }).orElseGet(() -> userRepository.save(new User(
                        identificationType,
                        normalizedNumber,
                        passwordEncoder.encode(password),
                        Role.ADMIN,
                        Role.ADMIN.defaultPermissions())));
        ensureGlobalAccount(user);
    }

    private void ensureGlobalAccount(User user) {
        Account account = accountRepository.findByLegacyUser_IdentificationTypeAndLegacyUser_IdentificationNumber(
                user.getIdentificationType(), user.getIdentificationNumber()).orElseGet(() -> {
            Person person = personRepository.save(new Person("Sisdent Administrator"));
            Account created = accountRepository.save(new Account(
                    person, user, email, user.getPassword(), true, false));
            emailClaimRepository.save(new AccountEmailClaim(created, email, EmailClaimType.VERIFIED));
            return created;
        });
        if (organizationRepository.findAll().isEmpty()) {
            organizationRepository.save(new Organization("Sisdent Training Organization"));
        }
        organizationRepository.findAll().forEach(organization -> {
            boolean membershipExists = membershipRepository
                    .existsByAccount_IdAndOrganization_IdAndClinicUnitIsNull(account.getId(), organization.getId());
            if (!membershipExists) {
                membershipRepository.save(new Membership(
                        account, organization, null, MembershipRole.ORGANIZATION_ADMIN));
            }
        });
    }
}
