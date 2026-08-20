package br.com.itbn.sisdent.filter;

import br.com.itbn.sisdent.model.DocumentType;
import br.com.itbn.sisdent.model.Gender;
import br.com.itbn.sisdent.model.Patient;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PatientSpecificationsTest {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void buildsPredicatesForEveryPatientFilter() {
        Root root = mock(Root.class);
        Path path = mock(Path.class);
        Join join = mock(Join.class);
        when(root.get(anyString())).thenReturn(path);
        when(root.join(anyString())).thenReturn(join);
        when(join.get(anyString())).thenReturn(path);

        assertThat(PatientSpecifications.matching(new PatientFilter(1L, " Ana ", LocalDate.of(1990, 1, 2), true,
                Gender.FEMALE, " 123 ", DocumentType.PASSPORT, " AB 12 ", " pt ", 4L, 5L))
                .toPredicate(root, mock(CriteriaQuery.class), mock(CriteriaBuilder.class))).isNull();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void acceptsAnEmptyPatientFilter() {
        Root root = mock(Root.class);
        Path path = mock(Path.class);
        when(root.get(anyString())).thenReturn(path);

        assertThat(PatientSpecifications.matching(new PatientFilter(null, null, null, null, null, null, null,
                null, null, null, null)).toPredicate(root, mock(CriteriaQuery.class), mock(CriteriaBuilder.class)))
                .isNull();
    }
}
