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

import java.util.Set;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
@ActiveProfiles("test")
class Phase4SchedulingIntegrationTests {
    @Autowired MockMvc mvc; @Autowired PersonRepository persons; @Autowired AccountRepository accounts;
    @Autowired OrganizationRepository organizations; @Autowired ClinicUnitRepository clinics;
    @Autowired MembershipRepository memberships; @Autowired PatientRepository patients; @Autowired PractitionerRepository practitioners;
    @Autowired PatientOrganizationLinkRepository links; @Autowired PasswordEncoder encoder; @Autowired JsonMapper json;

    @Test void scopesSchedulingAndPreservesTerminalLifecycle() throws Exception {
        Organization organization = organizations.save(new Organization("Scheduling organization"));
        ClinicUnit clinic = clinics.save(new ClinicUnit(organization, "Main"));
        Account account = accounts.save(new Account(persons.save(new Person("Scheduler")), "scheduler@example.com", encoder.encode("phase4-password"), false));
        memberships.save(new Membership(account, organization, null, MembershipRole.MANAGER));
        Patient patient = patients.findAll().getFirst();
        links.save(new PatientOrganizationLink(patient, organization, clinic, PatientLinkBasis.ATTENDANCE));
        String token = login();
        Practitioner practitioner = practitioners.save(new Practitioner(organization, null, "Dr. Scope", null, Set.of()));
        String request = """
                { clinicUnitId: \\\"%s\\\", patientId: \\\"%s\\\", practitionerId: \\\"%s\\\",
                  startAt: \\\"2030-01-01T09:00:00Z\\\", endAt: \\\"2030-01-01T10:00:00Z\\\", schedulingTimezone: \\\"Europe/Lisbon\\\" }
                """.formatted(clinic.getGlobalId(), patient.getGlobalId(), practitioner.getGlobalId());
        ClinicUnit otherClinic = clinics.save(new ClinicUnit(organization, "Other"));
        String otherClinicRequest = request.replace(clinic.getGlobalId().toString(), otherClinic.getGlobalId().toString());
        graphQl(token, "mutation { createAppointment(organizationId: \\\"%s\\\", input: %s) { globalId } }".formatted(organization.getGlobalId(), otherClinicRequest))
                .andExpect(jsonPath("$.errors[0].extensions.code").value("RESOURCE.NOT_FOUND"));
        String appointment = graphQl(token, "mutation { createAppointment(organizationId: \\\"%s\\\", input: %s) { globalId } }".formatted(organization.getGlobalId(), request))
                .andExpect(jsonPath("$.errors").doesNotExist()).andReturn().getResponse().getContentAsString();
        String appointmentId = json.readTree(appointment).at("/data/createAppointment/globalId").asString();
        graphQl(token, "mutation { createAppointment(organizationId: \\\"%s\\\", input: %s) { globalId } }".formatted(organization.getGlobalId(), request))
                .andExpect(jsonPath("$.errors[0].extensions.code").value("SCHEDULING.PRACTITIONER_UNAVAILABLE"));
        graphQl(token, "mutation { transitionAppointment(organizationId: \\\"%s\\\", clinicUnitId: \\\"%s\\\", appointmentId: \\\"%s\\\", status: COMPLETED) { status } }".formatted(organization.getGlobalId(), clinic.getGlobalId(), appointmentId))
                .andExpect(jsonPath("$.data.transitionAppointment.status").value("COMPLETED"));
        graphQl(token, "mutation { transitionAppointment(organizationId: \\\"%s\\\", clinicUnitId: \\\"%s\\\", appointmentId: \\\"%s\\\", status: CANCELLED) { status } }".formatted(organization.getGlobalId(), clinic.getGlobalId(), appointmentId))
                .andExpect(jsonPath("$.errors[0].extensions.code").value("CONFLICT"));
    }
    private org.springframework.test.web.servlet.ResultActions graphQl(String token, String query) throws Exception {
        String document = query.replace("\\\"", "\"");
        return mvc.perform(post("/graphql")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("query", document))));
    }
    private String login() throws Exception { String body=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"scheduler@example.com\",\"password\":\"phase4-password\"}")).andReturn().getResponse().getContentAsString(); return json.readTree(body).get("accessToken").asString(); }
}
