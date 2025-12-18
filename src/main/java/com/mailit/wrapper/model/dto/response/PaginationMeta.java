package com.mailit.wrapper.model.dto.response;

/**
 * Pagination metadata for list responses.
 * 
 * @param page current page number (1-based)
 * @param limit items per page
 * @param total total items across all pages
 * @param totalPages total number of pages
 */
public record PaginationMeta(
        int page,
        int limit,
        long total,
        int totalPages
) {
    /**
     * Create pagination metadata from Spring Data Page.
     */
    public static PaginationMeta from(org.springframework.data.domain.Page<?> page) {
        return new PaginationMeta(
                page.getNumber() + 1,  // Convert 0-based to 1-based
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    /**
     * Create pagination metadata for empty results.
     */
    public static PaginationMeta empty(int page, int limit) {
        return new PaginationMeta(page, limit, 0, 0);
    }
}
