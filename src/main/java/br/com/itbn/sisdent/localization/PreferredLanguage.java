package br.com.itbn.sisdent.localization;

import java.util.List;

/** Strict UI language contract persisted on an account. Catalogue locales intentionally have broader rules. */
public final class PreferredLanguage {
    public static final String DEFAULT = "en";
    private static final List<String> SUPPORTED = List.of("pt-PT", "en", "nl");

    private PreferredLanguage() {
    }

    public static boolean supports(String value) {
        return value != null && SUPPORTED.contains(value);
    }

    public static String require(String value) {
        if (!supports(value)) throw new IllegalArgumentException("Unsupported preferred language");
        return value;
    }

    public static List<String> supported() {
        return SUPPORTED;
    }
}
