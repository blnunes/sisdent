package br.com.itbn.sisdent.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ValidationException;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Pageable;

class PageableFactoryTest {

    private static final SortDefinition SORT_DEFINITION = new SortDefinition("name", Set.of("id", "name"));
    private final PageableFactory factory = new PageableFactory();

    @Test
    void usesDefaultPaginationAndSortValues() {
        Pageable result = factory.create(new PageQuery(null, null, null, null), SORT_DEFINITION);

        assertThat(result.getPageNumber()).isZero();
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getSort().getOrderFor("name").getDirection().isAscending()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("invalidQueries")
    void rejectsEveryInvalidPaginationFlowWithItsStableCodeAndMessage(
            PageQuery query,
            ErrorCode expectedCode,
            String expectedMessage) {
        assertThatThrownBy(() -> factory.create(query, SORT_DEFINITION))
                .isInstanceOf(ValidationException.class)
                .satisfies(throwable -> {
                    ValidationException exception = (ValidationException) throwable;
                    assertThat(exception.errorCode()).isEqualTo(expectedCode);
                    assertThat(exception.getMessage()).isEqualTo(expectedMessage);
                });
    }

    private static Stream<Arguments> invalidQueries() {
        return Stream.of(
                Arguments.of(new PageQuery(-1, 10, "name", "asc"), ErrorCode.PAGINATION_INVALID_VALUES, "PAGINATION.INVALID_VALUES"),
                Arguments.of(new PageQuery(0, 0, "name", "asc"), ErrorCode.PAGINATION_INVALID_VALUES, "PAGINATION.INVALID_VALUES"),
                Arguments.of(new PageQuery(0, 101, "name", "asc"), ErrorCode.PAGINATION_INVALID_VALUES, "PAGINATION.INVALID_VALUES"),
                Arguments.of(new PageQuery(0, 10, "unsupported", "asc"), ErrorCode.PAGINATION_UNSUPPORTED_SORT, "PAGINATION.UNSUPPORTED_SORT"),
                Arguments.of(new PageQuery(0, 10, "name", "sideways"), ErrorCode.PAGINATION_UNSUPPORTED_DIRECTION, "PAGINATION.UNSUPPORTED_DIRECTION"));
    }
}
