package br.com.itbn.sisdent.model;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class OdontogramFindingTest {

    @Test
    void createsFindingFromCohesiveContextAndObservation() {
        Organization organization = mock(Organization.class);
        ClinicUnit clinicUnit = mock(ClinicUnit.class);
        PatientOrganizationLink patientLink = mock(PatientOrganizationLink.class);
        Practitioner practitioner = mock(Practitioner.class);
        OdontogramFinding replacement = mock(OdontogramFinding.class);
        Instant observedAt = Instant.parse("2030-01-01T09:00:00Z");

        OdontogramFinding finding = new OdontogramFinding(
                new OdontogramFinding.FindingContext(
                        organization,
                        clinicUnit,
                        patientLink,
                        practitioner,
                        replacement),
                new OdontogramFinding.Observation(
                        "11",
                        OdontogramSurface.WHOLE_TOOTH,
                        OdontogramCondition.CARIES,
                        observedAt,
                        "Europe/Lisbon",
                        "Initial observation"));

        assertThat(finding.getOrganization()).isSameAs(organization);
        assertThat(finding.getClinicUnit()).isSameAs(clinicUnit);
        assertThat(finding.getPatientLink()).isSameAs(patientLink);
        assertThat(finding.getPractitioner()).isSameAs(practitioner);
        assertThat(finding.getReplacementFor()).isSameAs(replacement);
        assertThat(finding.getToothCode()).isEqualTo("11");
        assertThat(finding.getSurface()).isEqualTo(OdontogramSurface.WHOLE_TOOTH);
        assertThat(finding.getCondition()).isEqualTo(OdontogramCondition.CARIES);
        assertThat(finding.getObservedAt()).isEqualTo(observedAt);
        assertThat(finding.getObservationTimezone()).isEqualTo("Europe/Lisbon");
        assertThat(finding.getClinicalNote()).isEqualTo("Initial observation");
        assertThat(finding.isVoided()).isFalse();
    }

    @Test
    void voidsAnActiveFindingAndRejectsASecondVoid() {
        OdontogramFinding finding = new OdontogramFinding(
                new OdontogramFinding.FindingContext(
                        mock(Organization.class),
                        mock(ClinicUnit.class),
                        mock(PatientOrganizationLink.class),
                        null,
                        null),
                new OdontogramFinding.Observation(
                        "21",
                        OdontogramSurface.WHOLE_TOOTH,
                        OdontogramCondition.CARIES,
                        Instant.parse("2030-01-01T09:00:00Z"),
                        "Europe/Lisbon",
                        null));

        finding.voidRecord(" Correction ", "account-id");

        assertThat(finding.isVoided()).isTrue();
        assertThat(finding.getVoidedAt()).isNotNull();
        assertThat(finding.getVoidedBy()).isEqualTo("account-id");
        assertThat(finding.getVoidReason()).isEqualTo("Correction");
        assertThatThrownBy(() -> finding.voidRecord("Duplicate", "account-id"))
                .isInstanceOf(IllegalStateException.class);
    }
}
