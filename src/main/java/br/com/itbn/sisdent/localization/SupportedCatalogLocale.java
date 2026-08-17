package br.com.itbn.sisdent.localization;

import java.util.Locale;
import java.util.Set;

public final class SupportedCatalogLocale {
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("en", "nl", "pt");

    private SupportedCatalogLocale() {
    }

    public static boolean supports(Locale requested) {
        return requested != null && SUPPORTED_LANGUAGES.contains(requested.getLanguage());
    }

    public static Locale from(Locale requested) {
        if (supports(requested)) {
            return requested;
        }
        return Locale.ENGLISH;
    }

    public static String supportedLanguageTags() {
        return String.join(", ", SUPPORTED_LANGUAGES.stream().sorted().toList());
    }
}
