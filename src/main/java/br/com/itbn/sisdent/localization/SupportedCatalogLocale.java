package br.com.itbn.sisdent.localization;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SupportedCatalogLocale {
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("en", "nl", "pt");
    private static final List<String> SUPPORTED_LANGUAGE_TAGS = List.of("en", "nl", "pt");

    private SupportedCatalogLocale() {
    }

    public static boolean supports(Locale requested) {
        return requested != null && SUPPORTED_LANGUAGES.contains(requested.getLanguage());
    }

    public static Locale from(Locale requested) {
        return supports(requested) ? requested : Locale.ENGLISH;
    }

    public static String supportedLanguageTags() {
        return String.join(", ", SUPPORTED_LANGUAGE_TAGS);
    }

    /** Converts an accepted language or regional variant to its persisted catalogue key. */
    public static String catalogTag(Locale requested) {
        if (!supports(requested)) throw new IllegalArgumentException("Unsupported catalogue locale");
        return "pt".equals(requested.getLanguage()) ? "pt-PT" : requested.getLanguage();
    }
}
