package com.adventurebook.common;

/**
 * Raised by {@code entity}/{@code control} code for any legal-but-rejected domain
 * outcome (e.g. an illegal move, an invalid book upload). Always translated to a
 * {@code 400} by {@link GlobalExceptionHandler} — never a {@code 500}. Carries an
 * {@link ErrorCode} so clients can tell the failures apart.
 */
public class DomainException extends RuntimeException {

    private final ErrorCode code;

    public DomainException(String message) {
        this(ErrorCode.INVALID_REQUEST, message);
    }

    public DomainException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
