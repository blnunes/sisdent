package br.com.itbn.sisdent.pagination;

/**
 * Transport-neutral request for a slice of ordered data.
 * It can be used by HTTP controllers, scheduled jobs, queues, or Java callers.
 */
public record PageQuery(Integer page, Integer size, String sort, String direction) {
}
