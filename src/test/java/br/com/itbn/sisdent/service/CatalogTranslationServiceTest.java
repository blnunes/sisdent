package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.model.CatalogResourceType;
import br.com.itbn.sisdent.model.CatalogTranslation;
import br.com.itbn.sisdent.model.Speciality;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ResourceNotFoundException;
import br.com.itbn.sisdent.error.ValidationException;
import br.com.itbn.sisdent.repository.CatalogTranslationRepository;
import br.com.itbn.sisdent.repository.DentalProcedureRepository;
import br.com.itbn.sisdent.repository.SpecialityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogTranslationServiceTest {
    private final CatalogTranslationRepository repository = mock(CatalogTranslationRepository.class);
    private final SpecialityRepository specialities = mock(SpecialityRepository.class);
    private final DentalProcedureRepository procedures = mock(DentalProcedureRepository.class);
    private final MessageSource messages = mock(MessageSource.class);
    private CatalogTranslationService service;

    @BeforeEach
    void setUp() { service = new CatalogTranslationService(repository, specialities, procedures, messages); }

    @Test
    void customTranslationTakesPrecedenceOverBuiltInAndCanonicalFallbacks() {
        CatalogTranslation custom = new CatalogTranslation(CatalogResourceType.SPECIALITY, 7L, "pt-PT", "Implantologia digital");
        when(repository.findByResourceTypeAndResourceIdAndLocale(CatalogResourceType.SPECIALITY, 7L, "pt-PT"))
                .thenReturn(Optional.of(custom));

        assertThat(service.resolve(CatalogResourceType.SPECIALITY, 7L, "Digital Implantology", Locale.forLanguageTag("pt-PT")))
                .isEqualTo("Implantologia digital");
        verify(messages, never()).getMessage(any(), any(), any(), any(Locale.class));

        assertThatThrownBy(() -> service.resolve(CatalogResourceType.SPECIALITY, 8L, "Custom name", Locale.FRENCH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported catalogue locale");
    }

    @Test
    void resolvesBuiltInTranslationUsingNormalizedSlugWithoutEdgeHyphens() {
        when(repository.findByResourceTypeAndResourceIdAndLocale(CatalogResourceType.SPECIALITY, 9L, "en"))
                .thenReturn(Optional.empty());
        when(messages.getMessage("catalog.speciality.implantologia-digital", null, null, Locale.ENGLISH))
                .thenReturn("Digital implantology");

        assertThat(service.resolve(CatalogResourceType.SPECIALITY, 9L, "--Implantologia digital--", Locale.ENGLISH))
                .isEqualTo("Digital implantology");
    }

    @Test
    void replacesTranslationsAndReportsMissingLocales() {
        Speciality speciality = mock(Speciality.class);
        when(speciality.getId()).thenReturn(4L);
        when(speciality.getName()).thenReturn("Digital Implantology");
        when(specialities.findById(4L)).thenReturn(Optional.of(speciality));
        when(repository.findByResourceTypeAndResourceId(CatalogResourceType.SPECIALITY, 4L)).thenReturn(List.of());
        when(repository.findByResourceTypeAndResourceIdAndLocale(any(), any(), any())).thenReturn(Optional.empty());
        when(messages.getMessage(any(), any(), any(), any(Locale.class))).thenAnswer(invocation -> {
            Locale locale = invocation.getArgument(3);
            return locale.getLanguage().equals("nl") ? null : "Translated";
        });

        var response = service.replace(CatalogResourceType.SPECIALITY, 4L,
                Map.of("en", "Digital Implantology", "pt-PT", "Implantologia digital", "nl", ""));

        verify(repository, times(2)).save(any(CatalogTranslation.class));
        assertThat(response.missingLocales()).containsExactly("nl");
    }

    @Test
    void rejectsUnsupportedLocalesAndUnknownResources() {
        when(specialities.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.replace(CatalogResourceType.SPECIALITY, 99L, Map.of("en", "Name")))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting(exception -> ((ResourceNotFoundException) exception).errorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

        Speciality speciality = mock(Speciality.class);
        when(speciality.getId()).thenReturn(4L);
        when(speciality.getName()).thenReturn("Name");
        when(specialities.findById(4L)).thenReturn(Optional.of(speciality));
        when(repository.findByResourceTypeAndResourceId(CatalogResourceType.SPECIALITY, 4L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.replace(CatalogResourceType.SPECIALITY, 4L, Map.of("fr", "Nom")))
                .isInstanceOf(ValidationException.class)
                .extracting(exception -> ((ValidationException) exception).errorCode())
                .isEqualTo(ErrorCode.CATALOG_UNSUPPORTED_LOCALE);
    }
}
