package br.com.itbn.sisdent.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogModelTest {
    @Test
    void normalizesPersonAndMaintainsCatalogueProcedureState() {
        Person person = new Person(" Ana ");
        Speciality speciality = new Speciality("Dental", List.of("Cleaning"));
        DentalProcedure procedure = speciality.getProcedures().iterator().next();

        assertThat(person.getDisplayName()).isEqualTo("Ana");
        assertThat(person.getGlobalId()).isNotNull();
        assertThat(procedure.getName()).isEqualTo("Cleaning");
        assertThat(procedure.getStatus()).isEqualTo(CatalogStatus.ACTIVE);

        speciality.deactivate();
        assertThat(procedure.getStatus()).isEqualTo(CatalogStatus.INACTIVE);

        assertThat(speciality.addProcedure("cleaning")).isSameAs(procedure);
        assertThat(procedure.getStatus()).isEqualTo(CatalogStatus.ACTIVE);
    }
}
