package br.com.itbn.sisdent.observability;

import java.util.UUID;
import org.slf4j.MDC;

/** Creates and validates correlation IDs without accepting arbitrary header content into logs. */
public final class CorrelationIds {
    public static final String HEADER = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";
    private static final int MAX_LENGTH = 128;

    private CorrelationIds() { }

    public static String from(String value) {
        return value != null && value.length() <= MAX_LENGTH
                && value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}") ? value : generate();
    }

    public static String current() {
        String correlationId = MDC.get(MDC_KEY);
        return correlationId == null ? generate() : correlationId;
    }

    private static String generate() { return UUID.randomUUID().toString(); }
}
