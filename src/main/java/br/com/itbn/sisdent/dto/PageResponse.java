package br.com.itbn.sisdent.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Stable HTTP representation for paged collection responses.
 *
 * @param content records in the requested page
 * @param page zero-based page number
 * @param size requested page size
 * @param totalElements total records matching the query
 * @param totalPages total number of pages
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <S, T> PageResponse<T> from(Page<S> source, Function<S, T> mapper) {
        return new PageResponse<>(
                source.getContent().stream().map(mapper).toList(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages());
    }
}
