package br.com.itbn.sisdent;

import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.ClinicUnit;
import br.com.itbn.sisdent.model.Membership;
import br.com.itbn.sisdent.model.MembershipRole;
import br.com.itbn.sisdent.model.Organization;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.model.PatientLinkBasis;
import br.com.itbn.sisdent.model.PatientOrganizationLink;
import br.com.itbn.sisdent.model.Person;
import br.com.itbn.sisdent.repository.AccountRepository;
import br.com.itbn.sisdent.repository.ClinicUnitRepository;
import br.com.itbn.sisdent.repository.MembershipRepository;
import br.com.itbn.sisdent.repository.OrganizationRepository;
import br.com.itbn.sisdent.repository.PatientOrganizationLinkRepository;
import br.com.itbn.sisdent.repository.PatientRepository;
import br.com.itbn.sisdent.repository.PersonRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ClinicalRecordsGraphQlIntegrationTests {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private PersonRepository persons;
    @Autowired
    private AccountRepository accounts;
    @Autowired
    private OrganizationRepository organizations;
    @Autowired
    private ClinicUnitRepository clinics;
    @Autowired
    private MembershipRepository memberships;
    @Autowired
    private PatientRepository patients;
    @Autowired
    private PatientOrganizationLinkRepository links;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private JsonMapper json;

    @Test
    void preservesFinalEncounterAndVersionedOdontogramCorrectionThroughGraphQl() throws Exception {
        Organization organization = organizations.save(new Organization("Clinical organization"));
        ClinicUnit clinic = clinics.save(new ClinicUnit(organization, "Clinical unit"));
        Account manager = accounts.save(new Account(persons.save(new Person("Clinical manager")),
                "clinical-manager@example.com", encoder.encode("phase6-password"), false));
        memberships.save(new Membership(manager, organization, clinic, MembershipRole.CLINICAL_MANAGER));
        Patient patient = patients.findAll().getFirst();
        links.save(new PatientOrganizationLink(patient, organization, clinic, PatientLinkBasis.ATTENDANCE));
        String token = login();
        String encounterId = createEncounter(token, organization, clinic, patient);

        graphQl(token, "mutation { updateClinicalEncounter(organizationId: \"%s\", encounterId: \"%s\", input: { clinicUnitId: \"%s\", patientId: \"%s\", careAt: \"2030-01-01T09:30:00Z\", careTimezone: \"Europe/Lisbon\", narrative: \"Updated clinical note\", version: 0 }) { narrative status version } }"
                .formatted(organization.getGlobalId(), encounterId, clinic.getGlobalId(), patient.getGlobalId()))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.updateClinicalEncounter.narrative").value("Updated clinical note"))
                .andExpect(jsonPath("$.data.updateClinicalEncounter.status").value("DRAFT"));

        graphQl(token, "mutation { finalizeClinicalEncounter(organizationId: \"%s\", clinicUnitId: \"%s\", encounterId: \"%s\") { status } }"
                .formatted(organization.getGlobalId(), clinic.getGlobalId(), encounterId))
                .andExpect(jsonPath("$.data.finalizeClinicalEncounter.status").value("FINAL"));
        graphQl(token, "mutation { updateClinicalEncounter(organizationId: \"%s\", encounterId: \"%s\", input: { clinicUnitId: \"%s\", patientId: \"%s\", careAt: \"2030-01-01T09:00:00Z\", careTimezone: \"Europe/Lisbon\", narrative: \"Changed\", version: 0 }) { globalId } }"
                .formatted(organization.getGlobalId(), encounterId, clinic.getGlobalId(), patient.getGlobalId()))
                .andExpect(jsonPath("$.errors[0].extensions.code").value("CONFLICT"));

        String findingId = createFinding(token, organization, clinic, patient);
        graphQl(token, "mutation { voidOdontogramFinding(organizationId: \"%s\", clinicUnitId: \"%s\", findingId: \"%s\", input: { reason: \"Correction\", version: 0 }) { voidReason } }"
                .formatted(organization.getGlobalId(), clinic.getGlobalId(), findingId))
                .andExpect(jsonPath("$.data.voidOdontogramFinding.voidReason").value("Correction"));
        graphQl(token, "query { currentOdontogram(organizationId: \"%s\", clinicUnitId: \"%s\", patientId: \"%s\") { globalId } }"
                .formatted(organization.getGlobalId(), clinic.getGlobalId(), patient.getGlobalId()))
                .andExpect(jsonPath("$.data.currentOdontogram").isEmpty());
    }

    @Test
    void rejectsClinicalReadsOutsideTheAuthorizedClinicScope() throws Exception {
        Organization organization = organizations.save(new Organization("Isolated organization"));
        ClinicUnit assigned = clinics.save(new ClinicUnit(organization, "Assigned clinic"));
        ClinicUnit other = clinics.save(new ClinicUnit(organization, "Other clinic"));
        Account reader = accounts.save(new Account(persons.save(new Person("Clinical reader")),
                "clinical-reader@example.com", encoder.encode("phase6-password"), false));
        memberships.save(new Membership(reader, organization, assigned, MembershipRole.CLINICAL_READER));
        Patient patient = patients.findAll().getFirst();

        graphQl(login("clinical-reader@example.com"), "query { clinicalEncounters(organizationId: \"%s\", clinicUnitId: \"%s\", patientId: \"%s\") { content { globalId } } }"
                .formatted(organization.getGlobalId(), other.getGlobalId(), patient.getGlobalId()))
                .andExpect(jsonPath("$.errors[0].extensions.code").value("AUTHORIZATION.DENIED"));
    }

    private String createEncounter(String token, Organization organization, ClinicUnit clinic, Patient patient)
            throws Exception {
        String document = "mutation { createClinicalEncounter(organizationId: \"%s\", input: { clinicUnitId: \"%s\", patientId: \"%s\", careAt: \"2030-01-01T09:00:00Z\", careTimezone: \"Europe/Lisbon\", narrative: \"Scoped clinical note\" }) { globalId } }"
                .formatted(organization.getGlobalId(), clinic.getGlobalId(), patient.getGlobalId());
        String response = graphQl(token, document).andExpect(jsonPath("$.errors").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).at("/data/createClinicalEncounter/globalId").asString();
    }

    private String createFinding(String token, Organization organization, ClinicUnit clinic, Patient patient)
            throws Exception {
        String document = "mutation { createOdontogramFinding(organizationId: \"%s\", input: { clinicUnitId: \"%s\", patientId: \"%s\", toothCode: \"11\", surface: WHOLE_TOOTH, condition: CARIES, observedAt: \"2030-01-01T09:00:00Z\", observationTimezone: \"Europe/Lisbon\" }) { globalId } }"
                .formatted(organization.getGlobalId(), clinic.getGlobalId(), patient.getGlobalId());
        String response = graphQl(token, document).andExpect(jsonPath("$.errors").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).at("/data/createOdontogramFinding/globalId").asString();
    }

    private ResultActions graphQl(String token, String document) throws Exception {
        return mvc.perform(post("/graphql").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("query", document))));
    }

    private String login() throws Exception {
        return login("clinical-manager@example.com");
    }

    private String login(String email) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"phase6-password\"}".formatted(email)))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("accessToken").asString();
    }
}
