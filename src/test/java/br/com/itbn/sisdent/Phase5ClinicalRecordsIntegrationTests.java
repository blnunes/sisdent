package br.com.itbn.sisdent;

import br.com.itbn.sisdent.model.*;
import br.com.itbn.sisdent.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
@ActiveProfiles("test")
class Phase5ClinicalRecordsIntegrationTests {
 @Autowired MockMvc mvc; @Autowired PersonRepository persons; @Autowired AccountRepository accounts; @Autowired OrganizationRepository organizations; @Autowired ClinicUnitRepository clinics; @Autowired MembershipRepository memberships; @Autowired PatientRepository patients; @Autowired PatientOrganizationLinkRepository links; @Autowired PasswordEncoder encoder; @Autowired JsonMapper json;
 @Test void preservesFinalEncounterAndVoidReplaceOdontogramHistory() throws Exception {
  Organization organization=organizations.save(new Organization("Clinical organization")); ClinicUnit clinic=clinics.save(new ClinicUnit(organization,"Clinical unit"));
  Account manager=accounts.save(new Account(persons.save(new Person("Clinical manager")),"clinical-manager@example.com",encoder.encode("phase5-password"),false)); memberships.save(new Membership(manager,organization,clinic,MembershipRole.CLINICAL_MANAGER));
  Patient patient=patients.findAll().getFirst(); links.save(new PatientOrganizationLink(patient,organization,clinic,PatientLinkBasis.DOCUMENT_ACCEPTANCE)); String token=login();
  String draft=encounter(clinic,patient); String encounterId=json.readTree(draft).get("globalId").asText();
  mvc.perform(post("/api/organizations/{org}/clinical/encounters/{id}/finalize",organization.getGlobalId(),encounterId).param("clinicUnitId",clinic.getGlobalId().toString()).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FINAL"));
  mvc.perform(put("/api/organizations/{org}/clinical/encounters/{id}",organization.getGlobalId(),encounterId).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{} ")).andExpect(status().isBadRequest());
  String finding=mvc.perform(post("/api/organizations/{org}/clinical/odontogram/findings",organization.getGlobalId()).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{\"clinicUnitId\":\"%s\",\"patientId\":\"%s\",\"toothCode\":\"11\",\"surface\":\"WHOLE_TOOTH\",\"condition\":\"CARIES\",\"observedAt\":\"2030-01-01T09:00:00Z\",\"observationTimezone\":\"Europe/Lisbon\"}".formatted(clinic.getGlobalId(),patient.getGlobalId()))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  String findingId=json.readTree(finding).get("globalId").asText(); long version=json.readTree(finding).get("version").longValue();
  mvc.perform(post("/api/organizations/{org}/clinical/odontogram/findings/{id}/void",organization.getGlobalId(),findingId).param("clinicUnitId",clinic.getGlobalId().toString()).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Correction\",\"version\":%d}".formatted(version))).andExpect(status().isOk()).andExpect(jsonPath("$.voidReason").value("Correction"));
  mvc.perform(get("/api/organizations/{org}/clinical/odontogram/current",organization.getGlobalId()).param("clinicUnitId",clinic.getGlobalId().toString()).param("patientId",patient.getGlobalId().toString()).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
 }
 private String encounter(ClinicUnit clinic,Patient patient) throws Exception {return mvc.perform(post("/api/organizations/{org}/clinical/encounters",clinic.getOrganization().getGlobalId()).header("Authorization","Bearer "+login()).contentType(MediaType.APPLICATION_JSON).content("{\"clinicUnitId\":\"%s\",\"patientId\":\"%s\",\"careAt\":\"2030-01-01T09:00:00Z\",\"careTimezone\":\"Europe/Lisbon\",\"narrative\":\"Scoped clinical note\"}".formatted(clinic.getGlobalId(),patient.getGlobalId()))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();}
 private String login() throws Exception{return json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"clinical-manager@example.com\",\"password\":\"phase5-password\"}")).andReturn().getResponse().getContentAsString()).get("accessToken").asText();}
}
