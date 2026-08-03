package br.com.itbn.sisdent.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Creates database-backed pagination consistently for every application consumer. */
@Component
public class PageableFactory {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    public Pageable create(PageQuery query, SortDefinition definition) {
        int page = query.page() == null ? DEFAULT_PAGE : query.page();
        int size = query.size() == null ? DEFAULT_SIZE : query.size();
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination values");
        }
        String property = query.sort() == null || query.sort().isBlank()
                ? definition.defaultProperty() : query.sort();
        if (!definition.allowedProperties().contains(property)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field");
        }
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(query.direction() == null ? "asc" : query.direction());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort direction");
        }
        return PageRequest.of(page, size, Sort.by(direction, property));
    }
}
