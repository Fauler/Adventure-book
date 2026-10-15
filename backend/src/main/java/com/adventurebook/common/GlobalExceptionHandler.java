package com.adventurebook.common;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

/**
 * Centralized exception handling (NFR-01). Anything that is a legal-but-rejected
 * outcome — malformed request shape (Bean Validation) or a domain-rule violation
 * ({@link DomainException}) — becomes a {@code 4xx} with a uniform {@link ApiError}
 * body carrying a stable {@link ErrorCode}. Anything unexpected becomes a {@code 500}
 * with a generic message (never a stack trace) and is logged at {@code ERROR}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBeanValidation(MethodArgumentNotValidException ex) {
        List<String> messages = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        log.warn("Rejected request: shape validation failed: {}", messages);
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Validation failed", messages);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.warn("Rejected request: body could not be parsed: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST, "Malformed request body",
                List.of("Request body is missing or not valid JSON"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex) {
        log.warn("Rejected request: missing parameter: {}", ex.getParameterName());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PARAMETER, "Invalid parameter",
                List.of("Required parameter '" + ex.getParameterName() + "' is missing"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Rejected request: parameter '{}' has an invalid value", ex.getName());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PARAMETER, "Invalid parameter",
                List.of("Parameter '" + ex.getName() + "' has an invalid value"));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomainException(DomainException ex) {
        log.warn("Rejected request: domain rule violation [{}]: {}", ex.code(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.code(), "Invalid request", List.of(ex.getMessage()));
    }

    @ExceptionHandler(BookValidationException.class)
    public ResponseEntity<ApiError> handleBookValidation(BookValidationException ex) {
        log.warn("Rejected book submission: {}", ex.reasons());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.BOOK_VALIDATION_FAILED, "Book validation failed",
                ex.reasons());
    }

    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ApiError> handleBookNotFound(BookNotFoundException ex) {
        log.warn("Rejected request: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ErrorCode.BOOK_NOT_FOUND, "Book not found", List.of(ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleRouteNotFound(NoResourceFoundException ex) {
        log.warn("Rejected request: no route for {}", ex.getResourcePath());
        return build(HttpStatus.NOT_FOUND, ErrorCode.ROUTE_NOT_FOUND, "Not found",
                List.of("No endpoint exists at this path"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        log.warn("Rejected request: method {} not supported", ex.getMethod());
        return build(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED, "Method not allowed",
                List.of("HTTP method " + ex.getMethod() + " is not supported for this endpoint"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        log.warn("Rejected request: unsupported media type {}", ex.getContentType());
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported media type", List.of("Content type is not supported for this endpoint"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unexpected failure", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "Internal server error",
                List.of("Something went wrong"));
    }

    private static ResponseEntity<ApiError> build(HttpStatus status, ErrorCode code, String error,
            List<String> messages) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), code, error, messages));
    }
}
