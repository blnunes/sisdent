package br.com.itbn.sisdent.filter;

/** Criteria used to restrict specialities independently from pagination. */
public record SpecialityFilter(String name, String procedure) {

    public String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase();
    }
}
