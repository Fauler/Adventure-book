package com.adventurebook.common;

/**
 * Raised when a request references a book {@code id} that doesn't exist in the valid
 * catalog. Distinct from {@link DomainException} (an illegal move on a book that
 * *does* exist): this is a missing-resource case, so {@link GlobalExceptionHandler}
 * maps it to {@code 404} rather than {@code 400}.
 */
public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(String message) {
        super(message);
    }
}
