package br.com.itbn.sisdent.localization;

import br.com.itbn.sisdent.model.CatalogResourceType;
import br.com.itbn.sisdent.model.Speciality;
import br.com.itbn.sisdent.service.CatalogTranslationService;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpecialityNameLocalizerTest {
    @Test
    void delegatesDynamicAndBuiltInResolutionToTheReusableTranslationService() {
        CatalogTranslationService translations = mock(CatalogTranslationService.class);
        Speciality speciality = new Speciality("Pediatric Dentistry");
        Locale locale = Locale.forLanguageTag("pt-PT");
        when(translations.resolve(CatalogResourceType.SPECIALITY, null, speciality.getName(), locale))
                .thenReturn("Odontopediatria");

        assertThat(new SpecialityNameLocalizer(translations).localize(speciality, locale))
                .isEqualTo("Odontopediatria");
        verify(translations).resolve(CatalogResourceType.SPECIALITY, null, speciality.getName(), locale);
    }
}
