package com.adventurebook.common;

import java.util.List;

/**
 * Raised when a book submitted via {@code POST /api/books} (US-11) fails one or more
 * of {@link com.adventurebook.entity.Book#validate()}'s rules — the same rules
 * enforced by the startup ingestion pipeline, never duplicated or relaxed here.
 * {@link GlobalExceptionHandler} maps this to a {@code 400} carrying every failed
 * rule verbatim, per the API contract in {@code docs/03-technical-architecture.md}.
 */
public class BookValidationException extends RuntimeException {

    private final List<String> reasons;

    public BookValidationException(List<String> reasons) {
        super("Book failed validation: " + reasons);
        this.reasons = List.copyOf(reasons);
    }

    public List<String> reasons() {
        return reasons;
    }
}
