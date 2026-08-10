package br.com.itbn.sisdent.localization;

import br.com.itbn.sisdent.model.Continent;
import br.com.itbn.sisdent.model.Country;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class CountryNameLocalizerTest {
    private final CountryNameLocalizer localizer = new CountryNameLocalizer();

    @Test
    void localizesIsoCountryNamesInEverySupportedLanguage() {
        Country netherlands = new Country("Netherlands", "NL", Continent.EUROPE);

        assertThat(localizer.localize(netherlands, Locale.ENGLISH)).isEqualTo("Netherlands");
        assertThat(localizer.localize(netherlands, Locale.forLanguageTag("nl"))).isEqualTo("Nederland");
        assertThat(localizer.localize(netherlands, Locale.forLanguageTag("pt-PT"))).isEqualTo("Países Baixos");
    }

    @Test
    void fallsBackToEnglishForUnsupportedLocalesAndToCanonicalNameForUnknownCodes() {
        Country netherlands = new Country("Netherlands", "NL", Continent.EUROPE);
        Country custom = new Country("Custom country", "ZZ", Continent.EUROPE);

        assertThat(localizer.localize(netherlands, Locale.FRENCH)).isEqualTo("Netherlands");
        assertThat(localizer.localize(custom, Locale.forLanguageTag("nl"))).isEqualTo("Custom country");
    }
}
