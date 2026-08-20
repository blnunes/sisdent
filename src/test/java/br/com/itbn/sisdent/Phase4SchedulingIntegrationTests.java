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
class Phase4SchedulingIntegrationTests {
    @Autowired MockMvc mvc; @Autowired PersonRepository persons; @Autowired AccountRepository accounts;
    @Autowired OrganizationRepository organizations; @Autowired ClinicUnitRepository clinics;
    @Autowired MembershipRepository memberships; @Autowired PatientRepository patients;
    @Autowired PatientOrganizationLinkRepository links; @Autowired PasswordEncoder encoder; @Autowired JsonMapper json;

    @Test void scopesSchedulingAndPreservesTerminalLifecycle() throws Exception {
        Organization organization = organizations.save(new Organization("Scheduling organization"));
        ClinicUnit clinic = clinics.save(new ClinicUnit(organization, "Main"));
        Account account = accounts.save(new Account(persons.save(new Person("Scheduler")), "scheduler@example.com", encoder.encode("phase4-password"), false));
        memberships.save(new Membership(account, organization, null, MembershipRole.MANAGER));
        Patient patient = patients.findAll().getFirst();
        links.save(new PatientOrganizationLink(patient, organization, clinic, PatientLinkBasis.ATTENDANCE));
        String token = login();
        String practitioner = mvc.perform(post("/api/organizations/{organizationId}/practitioners", organization.getGlobalId())
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Dr. Scope\",\"specialityIds\":[]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String practitionerId = json.readTree(practitioner).get("globalId").asString();
        String request = """
                {"clinicUnitId":"%s","patientId":"%s","practitionerId":"%s",
                 "startAt":"2030-01-01T09:00:00Z","endAt":"2030-01-01T10:00:00Z","schedulingTimezone":"Europe/Lisbon"}
                """.formatted(clinic.getGlobalId(), patient.getGlobalId(), practitionerId);
        ClinicUnit otherClinic = clinics.save(new ClinicUnit(organization, "Other"));
        String otherClinicRequest = request.replace(clinic.getGlobalId().toString(), otherClinic.getGlobalId().toString());
        mvc.perform(post("/api/organizations/{organizationId}/appointments", organization.getGlobalId())
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(otherClinicRequest))
                .andExpect(status().isNotFound());
        String appointment = mvc.perform(post("/api/organizations/{organizationId}/appointments", organization.getGlobalId())
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String appointmentId = json.readTree(appointment).get("globalId").asString();
        mvc.perform(post("/api/organizations/{organizationId}/appointments", organization.getGlobalId()).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:sisdent:error:scheduling.practitioner_unavailable"))
                .andExpect(jsonPath("$.code").value("SCHEDULING.PRACTITIONER_UNAVAILABLE"));
        mvc.perform(post("/api/organizations/{organizationId}/appointments/{id}/complete", organization.getGlobalId(), appointmentId).param("clinicUnitId", clinic.getGlobalId().toString()).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(post("/api/organizations/{organizationId}/appointments/{id}/cancel", organization.getGlobalId(), appointmentId).param("clinicUnitId", clinic.getGlobalId().toString()).header("Authorization", "Bearer " + token)).andExpect(status().isConflict());
    }
    private String login() throws Exception { String body=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"scheduler@example.com\",\"password\":\"phase4-password\"}")).andReturn().getResponse().getContentAsString(); return json.readTree(body).get("accessToken").asString(); }
}
