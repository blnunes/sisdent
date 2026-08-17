package br.com.itbn.sisdent.pagination;

import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ValidationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

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
            throw new ValidationException(ErrorCode.PAGINATION_INVALID_VALUES);
        }
        String property = query.sort() == null || query.sort().isBlank()
                ? definition.defaultProperty() : query.sort();
        if (!definition.allowedProperties().contains(property)) {
            throw new ValidationException(ErrorCode.PAGINATION_UNSUPPORTED_SORT);
        }
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(query.direction() == null ? "asc" : query.direction());
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(ErrorCode.PAGINATION_UNSUPPORTED_DIRECTION, java.util.Map.of(), exception);
        }
        return PageRequest.of(page, size, Sort.by(direction, property));
    }
}
