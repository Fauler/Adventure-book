package com.adventurebook.common;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error shape returned by {@link GlobalExceptionHandler} for every rejected
 * request (bad move, invalid book upload, malformed request, etc). Never a 500 for
 * anything that is a legal-but-rejected domain outcome. {@code code} is the stable
 * machine-readable identifier ({@link ErrorCode}); {@code messages} are for humans.
 */
public record ApiError(Instant timestamp, int status, ErrorCode code, String error, List<String> messages) {

    public static ApiError of(int status, ErrorCode code, String error, List<String> messages) {
        return new ApiError(Instant.now(), status, code, error, messages);
    }
}
