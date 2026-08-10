package br.com.itbn.sisdent.localization;

import java.util.Locale;
import java.util.Set;

final class SupportedCatalogLocale {
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("en", "nl", "pt");

    private SupportedCatalogLocale() {
    }

    static Locale from(Locale requested) {
        if (requested != null && SUPPORTED_LANGUAGES.contains(requested.getLanguage())) {
            return requested;
        }
        return Locale.ENGLISH;
    }
}
