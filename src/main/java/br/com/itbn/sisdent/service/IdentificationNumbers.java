package br.com.itbn.sisdent.service;

import java.util.Locale;

public final class IdentificationNumbers {

    private IdentificationNumbers() {
    }

    public static String normalize(String value) {
        return value.replaceAll("[\\s-]", "")
                .toUpperCase(Locale.ROOT);
    }
}
