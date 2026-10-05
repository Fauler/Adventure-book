package com.adventurebook.boundary.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request body for {@code PUT /api/books/{id}/progress} (US-09), per the API contract
 * in `docs/03-technical-architecture.md`.
 */
public record ProgressRequest(@NotBlank String currentSectionId, @Positive Integer health) {
}
