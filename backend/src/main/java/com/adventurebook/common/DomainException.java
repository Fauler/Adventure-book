package com.adventurebook.common;

/**
 * Raised by {@code entity}/{@code control} code for any legal-but-rejected domain
 * outcome (e.g. an illegal move, an invalid book upload). Always translated to a
 * {@code 400} by {@link GlobalExceptionHandler} — never a {@code 500}.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
