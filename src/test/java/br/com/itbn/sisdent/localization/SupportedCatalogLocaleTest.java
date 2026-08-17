package br.com.itbn.sisdent.localization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SupportedCatalogLocaleTest {

    @ParameterizedTest
    @CsvSource({"en,en", "pt,pt-PT", "pt-PT,pt-PT", "pt-BR,pt-PT", "nl,nl", "nl-NL,nl"})
    void acceptsSupportedLanguagesAndRegionalVariants(String requested, String catalogTag) {
        Locale locale = Locale.forLanguageTag(requested);
        assertThat(SupportedCatalogLocale.supports(locale)).isTrue();
        assertThat(SupportedCatalogLocale.catalogTag(locale)).isEqualTo(catalogTag);
    }

    @ParameterizedTest
    @ValueSource(strings = {"zh-CN", "", "und"})
    void rejectsUnsupportedOrMalformedLanguageTags(String requested) {
        Locale locale = Locale.forLanguageTag(requested);
        assertThat(SupportedCatalogLocale.supports(locale)).isFalse();
        assertThatThrownBy(() -> SupportedCatalogLocale.catalogTag(locale))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported catalogue locale");
    }
}
