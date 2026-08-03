package br.com.itbn.sisdent.config;

import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.Membership;
import br.com.itbn.sisdent.model.MembershipRole;
import br.com.itbn.sisdent.model.Organization;
import br.com.itbn.sisdent.model.Person;
import br.com.itbn.sisdent.repository.AccountRepository;
import br.com.itbn.sisdent.repository.MembershipRepository;
import br.com.itbn.sisdent.repository.OrganizationRepository;
import br.com.itbn.sisdent.repository.PersonRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(2)
public class AdminDataLoader implements ApplicationRunner {
    private final AccountRepository accounts;
    private final PersonRepository people;
    private final OrganizationRepository organizations;
    private final MembershipRepository memberships;
    private final PasswordEncoder passwords;
    private final String email;
    private final String password;

    public AdminDataLoader(AccountRepository accounts, PersonRepository people,
            OrganizationRepository organizations, MembershipRepository memberships,
            PasswordEncoder passwords, @Value("${sisdent.bootstrap-admin.email}") String email,
            @Value("${sisdent.bootstrap-admin.password}") String password) {
        this.accounts = accounts;
        this.people = people;
        this.organizations = organizations;
        this.memberships = memberships;
        this.passwords = passwords;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Account account = accounts.findByEmail(Account.normalizeEmail(email)).orElseGet(() ->
                accounts.save(new Account(people.save(new Person("Sisdent Administrator")), email,
                        passwords.encode(password), true)));
        if (organizations.findAll().isEmpty()) organizations.save(new Organization("Sisdent Training Organization"));
        organizations.findAll().forEach(organization -> {
            if (!memberships.existsByAccount_IdAndOrganization_IdAndClinicUnitIsNull(account.getId(), organization.getId())) {
                memberships.save(new Membership(account, organization, null, MembershipRole.ORGANIZATION_ADMIN));
            }
        });
    }
}
