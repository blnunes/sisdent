package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.filter.SpecialityFilter;

/** Typed GraphQL filtering adapter for the existing speciality query service. */
public record SpecialityFilterInput(String name, String procedure) {
    public SpecialityFilter toFilter() {
        return new SpecialityFilter(name, procedure);
    }
}
