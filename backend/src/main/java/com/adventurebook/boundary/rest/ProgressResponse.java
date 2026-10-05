package com.adventurebook.boundary.rest;

import java.time.Instant;

import com.adventurebook.entity.SavedProgress;

/**
 * Response body for {@code GET}/{@code PUT /api/books/{id}/progress} (US-09/US-10), per
 * the API contract in `docs/03-technical-architecture.md`. {@code GET} returns this
 * wrapped in a {@code 200}, or a bare {@code 204 No Content} when nothing is saved (see
 * {@code BookController#getProgress}).
 */
public record ProgressResponse(String currentSectionId, int health, Instant updatedAt) {

    public static ProgressResponse from(SavedProgress savedProgress) {
        return new ProgressResponse(
                savedProgress.currentSectionId(), savedProgress.health(), savedProgress.updatedAt());
    }
}
