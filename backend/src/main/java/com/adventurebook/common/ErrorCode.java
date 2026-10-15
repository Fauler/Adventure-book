package com.adventurebook.common;

/**
 * Stable, machine-readable error codes returned in {@link ApiError#code()}. Clients
 * must branch on these, never on the human-readable messages.
 */
public enum ErrorCode {
    INVALID_REQUEST,
    VALIDATION_FAILED,
    MALFORMED_REQUEST,
    INVALID_PARAMETER,
    BOOK_NOT_FOUND,
    BOOK_VALIDATION_FAILED,
    INVALID_SECTION,
    INVALID_OPTION,
    GAME_ALREADY_FINISHED,
    ROUTE_NOT_FOUND,
    METHOD_NOT_ALLOWED,
    UNSUPPORTED_MEDIA_TYPE,
    INTERNAL_ERROR
}
