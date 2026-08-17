package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.pagination.PageQuery;

/** Typed GraphQL pagination adapter; application services continue to use {@link PageQuery}. */
public record CataloguePageInput(Integer page, Integer size, String sort, SortDirection direction) {
    public PageQuery toPageQuery() {
        return new PageQuery(page, size, sort, direction == null ? null : direction.name());
    }
}
