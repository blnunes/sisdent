package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.error.ValidationException;
import br.com.itbn.sisdent.model.DocumentType;
import br.com.itbn.sisdent.model.Gender;
import br.com.itbn.sisdent.model.OdontogramCondition;
import br.com.itbn.sisdent.model.OdontogramSurface;
import br.com.itbn.sisdent.model.PatientLinkBasis;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GraphQlMutationInputTest {
    private static final UUID CLINIC_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID PRACTITIONER_ID = UUID.randomUUID();
    private static final String INSTANT = "2026-08-24T10:15:30Z";

    @Test
    void convertsPatientSearchAndIdentityInputsAndRejectsInvalidBirthDates() {
        PatientFilterInput filter = new PatientFilterInput(1L, "Ana", "1990-01-02", true, Gender.FEMALE,
                null, null, null, null, null, null);
        ExactPatientMatchInput match = new ExactPatientMatchInput(DocumentType.PASSPORT, "PT", "AB123", "1990-01-02");
        PatientLinkMutationInput link = new PatientLinkMutationInput(DocumentType.PASSPORT, "PT", "AB123",
                "1990-01-02", CLINIC_ID, PatientLinkBasis.INTAKE);

        assertThat(filter.toFilter().birthDate()).isEqualTo(LocalDate.of(1990, 1, 2));
        assertThat(match.toRequest().birthDate()).isEqualTo(LocalDate.of(1990, 1, 2));
        assertThat(link.toRequest().clinicUnitId()).isEqualTo(CLINIC_ID);
        assertThat(new PatientFilterInput(null, null, null, null, null, null, null, null, null, null, null)
                .toFilter().birthDate()).isNull();
        ExactPatientMatchInput invalidMatch = new ExactPatientMatchInput(DocumentType.PASSPORT, "PT", "AB123", "bad");
        PatientLinkMutationInput invalidLink = new PatientLinkMutationInput(DocumentType.PASSPORT, "PT", "AB123", "bad",
                CLINIC_ID, PatientLinkBasis.INTAKE);

        assertThatThrownBy(invalidMatch::toRequest)
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(invalidLink::toRequest).isInstanceOf(ValidationException.class);
    }

    @Test
    void convertsClinicalAndSchedulingInputsAndRejectsInvalidInstants() {
        ClinicalEncounterMutationInput clinical = new ClinicalEncounterMutationInput(CLINIC_ID, PATIENT_ID, null,
                PRACTITIONER_ID, INSTANT, "Europe/Lisbon", "note", null, 3L);
        AppointmentMutationInput appointment = new AppointmentMutationInput(CLINIC_ID, PATIENT_ID, PRACTITIONER_ID,
                INSTANT, "2026-08-24T11:15:30Z", "Europe/Lisbon");
        AmendEncounterMutationInput amendment = new AmendEncounterMutationInput(CLINIC_ID, null, PRACTITIONER_ID,
                INSTANT, "Europe/Lisbon", "note", null, "correction");

        assertThat(clinical.toCreateRequest().careAt()).isEqualTo(Instant.parse(INSTANT));
        assertThat(clinical.toUpdateRequest().version()).isEqualTo(3L);
        assertThat(appointment.toRequest().startAt()).isEqualTo(Instant.parse(INSTANT));
        assertThat(amendment.toRequest().careAt()).isEqualTo(Instant.parse(INSTANT));
        ClinicalEncounterMutationInput invalidClinical = new ClinicalEncounterMutationInput(CLINIC_ID, PATIENT_ID, null,
                null, INSTANT, "Europe/Lisbon", "note", null, null);
        AppointmentMutationInput invalidAppointment = new AppointmentMutationInput(CLINIC_ID, PATIENT_ID,
                PRACTITIONER_ID, "bad", INSTANT, "Europe/Lisbon");
        AmendEncounterMutationInput invalidAmendment = new AmendEncounterMutationInput(CLINIC_ID, null, null, "bad",
                "Europe/Lisbon", "note", null, "correction");

        assertThatThrownBy(invalidClinical::toUpdateRequest).isInstanceOf(ValidationException.class);
        assertThatThrownBy(invalidAppointment::toRequest).isInstanceOf(ValidationException.class);
        assertThatThrownBy(invalidAmendment::toRequest).isInstanceOf(ValidationException.class);
    }

    @Test
    void convertsPatientOdontogramAndPerformedProcedureInputsAndRejectsInvalidValues() {
        PatientMutationInput patient = new PatientMutationInput("Ana", "1990-01-02", true, Gender.FEMALE, null,
                DocumentType.PASSPORT, "AB123", "PT", "PT", null, null, Set.of(1L));
        OdontogramFindingMutationInput finding = new OdontogramFindingMutationInput(CLINIC_ID, PATIENT_ID,
                PRACTITIONER_ID, null, "11", OdontogramSurface.WHOLE_TOOTH, OdontogramCondition.SOUND, INSTANT,
                "Europe/Lisbon", null);
        PerformedProcedureMutationInput procedure = new PerformedProcedureMutationInput(1L, INSTANT, "done");

        assertThat(patient.toRequest().birthDate()).isEqualTo(LocalDate.of(1990, 1, 2));
        assertThat(finding.toRequest().observedAt()).isEqualTo(Instant.parse(INSTANT));
        assertThat(procedure.toRequest().performedAt()).isEqualTo(Instant.parse(INSTANT));
        PatientMutationInput invalidPatient = new PatientMutationInput("Ana", "bad", true, Gender.FEMALE, null,
                DocumentType.PASSPORT, "AB123", "PT", "PT", null, null, Set.of());
        OdontogramFindingMutationInput invalidFinding = new OdontogramFindingMutationInput(CLINIC_ID, PATIENT_ID, null,
                null, "11", OdontogramSurface.WHOLE_TOOTH, OdontogramCondition.SOUND, "bad", "Europe/Lisbon", null);
        PerformedProcedureMutationInput invalidProcedure = new PerformedProcedureMutationInput(1L, "bad", null);

        assertThatThrownBy(invalidPatient::toRequest)
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(invalidFinding::toRequest).isInstanceOf(ValidationException.class);
        assertThatThrownBy(invalidProcedure::toRequest).isInstanceOf(ValidationException.class);
    }
}
