package br.com.itbn.sisdent.model;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PatientTest {

    @Test
    void createsAndUpdatesPatientFromValueObject() {
        Country portugal = mock(Country.class);
        Country brazil = mock(Country.class);
        Address originalAddress = mock(Address.class);
        Address updatedAddress = mock(Address.class);
        Speciality orthodontics = mock(Speciality.class);
        Speciality endodontics = mock(Speciality.class);
        Patient patient = new Patient(details(
                new Patient.PatientIdentity(
                        "Ana Silva",
                        LocalDate.of(1992, 4, 18),
                        true,
                        Gender.FEMALE,
                        "12345"),
                new Patient.PatientDocument(
                        DocumentType.NATIONAL_ID_CARD,
                        "AB-123",
                        portugal,
                        portugal),
                originalAddress,
                List.of(orthodontics)));

        patient.update(details(
                new Patient.PatientIdentity(
                        "Ana Costa",
                        LocalDate.of(1993, 5, 19),
                        false,
                        Gender.FEMALE,
                        null),
                new Patient.PatientDocument(
                        DocumentType.PASSPORT,
                        "P-987",
                        brazil,
                        brazil),
                updatedAddress,
                List.of(endodontics)));

        assertThat(patient.getName()).isEqualTo("Ana Costa");
        assertThat(patient.getBirthDate()).isEqualTo(LocalDate.of(1993, 5, 19));
        assertThat(patient.isActive()).isFalse();
        assertThat(patient.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(patient.getTaxId()).isNull();
        assertThat(patient.getIdentificationType()).isEqualTo(DocumentType.PASSPORT);
        assertThat(patient.getIdentificationNumber()).isEqualTo("P-987");
        assertThat(patient.getDocumentIssuerCountry()).isSameAs(brazil);
        assertThat(patient.getNationality()).isSameAs(brazil);
        assertThat(patient.getAddress()).isSameAs(updatedAddress);
        assertThat(patient.getSpecialities()).containsExactly(endodontics);
    }

    @Test
    void exposesSpecialitiesAsAnImmutableSnapshot() {
        Speciality speciality = mock(Speciality.class);
        Patient patient = new Patient(details(
                new Patient.PatientIdentity(
                        "Ana Silva",
                        LocalDate.of(1992, 4, 18),
                        true,
                        Gender.FEMALE,
                        null),
                new Patient.PatientDocument(
                        DocumentType.NATIONAL_ID_CARD,
                        "AB-123",
                        mock(Country.class),
                        mock(Country.class)),
                mock(Address.class),
                List.of(speciality)));

        assertThatThrownBy(() -> patient.getSpecialities().add(mock(Speciality.class)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private Patient.PatientDetails details(
            Patient.PatientIdentity identity,
            Patient.PatientDocument document,
            Address address,
            List<Speciality> specialities) {
        return new Patient.PatientDetails(
                identity,
                document,
                address,
                specialities);
    }
}
