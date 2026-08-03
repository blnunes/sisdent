package br.com.itbn.sisdent;

import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.Membership;
import br.com.itbn.sisdent.model.MembershipRole;
import br.com.itbn.sisdent.model.Organization;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.model.Person;
import br.com.itbn.sisdent.repository.AccountRepository;
import br.com.itbn.sisdent.repository.MembershipRepository;
import br.com.itbn.sisdent.repository.OrganizationRepository;
import br.com.itbn.sisdent.repository.PatientOrganizationLinkRepository;
import br.com.itbn.sisdent.repository.PatientRepository;
import br.com.itbn.sisdent.repository.PersonRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Tag("integration")
class Phase2IdentityIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired PersonRepository personRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired MembershipRepository membershipRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired PatientOrganizationLinkRepository linkRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JsonMapper jsonMapper;

    @Test
    void normalizesAndEnforcesGlobalEmailUniqueness() {
        saveAccount(" First.User@Example.COM ", false);
        assertThat(accountRepository.findByEmail("first.user@example.com")).isPresent();
        assertThatThrownBy(() -> saveAccount("FIRST.USER@example.com", false))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void oneAccountCanHaveDifferentRolesAndRevokingOneScopePreservesTheOthers() throws Exception {
        Organization first = organizationRepository.save(new Organization("First organization"));
        Organization second = organizationRepository.save(new Organization("Second organization"));
        Account account = saveAccount("multi@example.com", false);
        account.assignAccountManagementOrganizationIfAbsent(first);
        accountRepository.save(account);
        Membership firstMembership = membershipRepository.save(
                new Membership(account, first, null, MembershipRole.ORGANIZATION_ADMIN));
        Membership secondMembership = membershipRepository.save(
                new Membership(account, second, null, MembershipRole.READ_ONLY));
        String token = login("multi@example.com", "phase2-password");

        mockMvc.perform(delete("/api/organizations/{organizationId}/memberships/{membershipId}",
                        first.getGlobalId(), firstMembership.getGlobalId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        assertThat(accountRepository.findById(account.getId())).isPresent();
        assertThat(membershipRepository.findById(firstMembership.getId()).orElseThrow().isActive()).isFalse();
        assertThat(membershipRepository.findById(secondMembership.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void delegatedAccountAdministrationIsLimitedToThePersistedManagementOrganization() throws Exception {
        Organization base = organizationRepository.save(new Organization("Management base"));
        Organization other = organizationRepository.save(new Organization("Operational-only organization"));
        Account account = saveAccount("scoped-admin@example.com", false);
        account.assignAccountManagementOrganizationIfAbsent(base);
        accountRepository.save(account);
        membershipRepository.save(new Membership(account, base, null, MembershipRole.ORGANIZATION_ADMIN));
        membershipRepository.save(new Membership(account, other, null, MembershipRole.ORGANIZATION_ADMIN));
        String token = login("scoped-admin@example.com", "phase2-password");

        mockMvc.perform(get("/api/platform/accounts").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/organizations/{organizationId}/accounts", base.getGlobalId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/organizations/{organizationId}/accounts", other.getGlobalId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void membershipChangesRequireTheCurrentVersionAndCannotCrossOrganizationScope() throws Exception {
        Organization management = organizationRepository.save(new Organization("Membership management"));
        Organization other = organizationRepository.save(new Organization("Other membership scope"));
        Account administrator = saveAccount("membership-admin@example.com", false);
        administrator.assignAccountManagementOrganizationIfAbsent(management);
        accountRepository.saveAndFlush(administrator);
        membershipRepository.saveAndFlush(new Membership(administrator, management, null, MembershipRole.ORGANIZATION_ADMIN));
        Account colleague = saveAccount("membership-colleague@example.com", false);
        Membership managed = membershipRepository.saveAndFlush(new Membership(colleague, management, null, MembershipRole.READ_ONLY));
        Membership outsideScope = membershipRepository.saveAndFlush(new Membership(colleague, other, null, MembershipRole.READ_ONLY));
        String token = login("membership-admin@example.com", "phase2-password");

        mockMvc.perform(post("/api/organizations/{organizationId}/memberships/{membershipId}/revoke",
                        management.getGlobalId(), managed.getGlobalId()).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":99}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/organizations/{organizationId}/memberships/{membershipId}/revoke",
                        management.getGlobalId(), outsideScope.getGlobalId()).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/organizations/{organizationId}/memberships/{membershipId}/revoke",
                        management.getGlobalId(), managed.getGlobalId()).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void organizationAdministratorCannotChangePlatformAdministratorMemberships() throws Exception {
        Organization organization = organizationRepository.save(new Organization("Protected platform account organization"));
        Account organizationAdministrator = saveAccount("organization-administrator@example.com", false);
        organizationAdministrator.assignAccountManagementOrganizationIfAbsent(organization);
        accountRepository.saveAndFlush(organizationAdministrator);
        membershipRepository.saveAndFlush(new Membership(organizationAdministrator, organization, null,
                MembershipRole.ORGANIZATION_ADMIN));
        Account platformAdministrator = saveAccount("protected-platform@example.com", true);
        Membership protectedMembership = membershipRepository.saveAndFlush(new Membership(platformAdministrator,
                organization, null, MembershipRole.READ_ONLY));
        String token = login("organization-administrator@example.com", "phase2-password");

        mockMvc.perform(patch("/api/organizations/{organizationId}/memberships/{membershipId}",
                        organization.getGlobalId(), protectedMembership.getGlobalId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MANAGER\",\"version\":0}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/organizations/{organizationId}/memberships/{membershipId}/revoke",
                        organization.getGlobalId(), protectedMembership.getGlobalId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isForbidden());

        assertThat(membershipRepository.findById(protectedMembership.getId()).orElseThrow().getRole())
                .isEqualTo(MembershipRole.READ_ONLY);
        assertThat(membershipRepository.findById(protectedMembership.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void patientSearchAndExactIntakeAreTenantScoped() throws Exception {
        Patient patient = patientRepository.findAll().getFirst();
        Organization linkedOrganization = organizationRepository.save(new Organization("Linked clinic"));
        Organization unrelatedOrganization = organizationRepository.save(new Organization("Unrelated clinic"));
        Account account = saveAccount("manager@example.com", false);
        membershipRepository.save(new Membership(account, linkedOrganization, null, MembershipRole.MANAGER));
        membershipRepository.save(new Membership(account, unrelatedOrganization, null, MembershipRole.MANAGER));
        linkRepository.save(new br.com.itbn.sisdent.model.PatientOrganizationLink(
                patient, linkedOrganization, null, br.com.itbn.sisdent.model.PatientLinkBasis.INTAKE));
        String token = login("manager@example.com", "phase2-password");

        mockMvc.perform(get("/api/organizations/{organizationId}/patients", unrelatedOrganization.getGlobalId())
                        .param("name", patient.getName())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        mockMvc.perform(post("/api/organizations/{organizationId}/patient-intake/exact-match",
                        unrelatedOrganization.getGlobalId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exactMatchJson(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.possibleMatchExists").value(false))
                .andExpect(jsonPath("$.patientId").doesNotExist())
                .andExpect(jsonPath("$.organizationId").doesNotExist());
    }

    @Test
    void patientLinkIsExplicitAuditedAndCannotBeCreatedOutsideMembershipScope() throws Exception {
        Patient patient = patientRepository.findAll().getFirst();
        Organization allowed = organizationRepository.save(new Organization("Allowed clinic"));
        Organization blocked = organizationRepository.save(new Organization("Blocked clinic"));
        Account account = saveAccount("linker@example.com", false);
        membershipRepository.save(new Membership(account, allowed, null, MembershipRole.MANAGER));
        String token = login("linker@example.com", "phase2-password");
        String request = linkJson(patient);

        mockMvc.perform(post("/api/organizations/{organizationId}/patient-links", blocked.getGlobalId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access denied"));

        mockMvc.perform(post("/api/organizations/{organizationId}/patient-links", allowed.getGlobalId())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(patient.getGlobalId().toString()))
                .andExpect(jsonPath("$.operationalBasis").value("ATTENDANCE"))
                .andExpect(jsonPath("$.createdBy").value(account.getGlobalId().toString()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void deactivatingOneOrganizationLinkDoesNotDeactivateAnother() throws Exception {
        Patient patient = patientRepository.findAll().getFirst();
        Organization first = organizationRepository.save(new Organization("First patient tenant"));
        Organization second = organizationRepository.save(new Organization("Second patient tenant"));
        Account account = saveAccount("patient-lifecycle@example.com", false);
        membershipRepository.save(new Membership(account, first, null, MembershipRole.MANAGER));
        membershipRepository.save(new Membership(account, second, null, MembershipRole.MANAGER));
        linkRepository.save(new br.com.itbn.sisdent.model.PatientOrganizationLink(
                patient, first, null, br.com.itbn.sisdent.model.PatientLinkBasis.INTAKE));
        linkRepository.saveAndFlush(new br.com.itbn.sisdent.model.PatientOrganizationLink(
                patient, second, null, br.com.itbn.sisdent.model.PatientLinkBasis.INTAKE));
        String token = login("patient-lifecycle@example.com", "phase2-password");

        mockMvc.perform(delete("/api/organizations/{organizationId}/patients/{patientId}",
                        first.getGlobalId(), patient.getGlobalId()).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/organizations/{organizationId}/patients", first.getGlobalId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
        mockMvc.perform(get("/api/organizations/{organizationId}/patients", second.getGlobalId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
        assertThat(patientRepository.findById(patient.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void platformAdministratorAloneHasNoPatientAccess() throws Exception {
        Organization organization = organizationRepository.save(new Organization("Clinical tenant"));
        saveAccount("platform@example.com", true);
        String token = login("platform@example.com", "phase2-password");

        mockMvc.perform(get("/api/organizations/{organizationId}/patients", organization.getGlobalId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access denied"));
    }

    private Account saveAccount(String email, boolean platformAdministrator) {
        Person person = personRepository.save(new Person(email.strip()));
        return accountRepository.saveAndFlush(new Account(person, email,
                passwordEncoder.encode("phase2-password"), platformAdministrator));
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(response).get("accessToken").asText();
    }

    private String exactMatchJson(Patient patient) {
        return """
                {
                  "documentType":"%s",
                  "issuerCountryCode":"%s",
                  "documentNumber":"%s",
                  "birthDate":"%s"
                }
                """.formatted(patient.getIdentificationType(), patient.getDocumentIssuerCountry().getCode(),
                patient.getIdentificationNumber(), patient.getBirthDate());
    }

    private String linkJson(Patient patient) {
        return """
                {
                  "documentType":"%s",
                  "issuerCountryCode":"%s",
                  "documentNumber":"%s",
                  "birthDate":"%s",
                  "operationalBasis":"ATTENDANCE"
                }
                """.formatted(patient.getIdentificationType(), patient.getDocumentIssuerCountry().getCode(),
                patient.getIdentificationNumber(), patient.getBirthDate());
    }

    private String bearer(String token) { return "Bearer " + token; }
}
