package com.adventurebook.boundary.rest;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request body for {@code POST /api/books/{id}/play}, per the API contract in
 * {@code docs/03-technical-architecture.md}. All fields are optional at the JSON/shape
 * level: omitting {@code currentSectionId} (and therefore {@code optionIndex}/
 * {@code health}) means "start a fresh game at BEGIN". {@link PositiveOrZero} only
 * checks the *shape* of a present value — whether these fields are legally required
 * together is a domain concern, decided by {@code GameEngineService}/{@code Book}, not
 * here (see the Bean Validation vs. domain rules split in
 * {@code docs/03-technical-architecture.md}).
 */
public record PlayRequest(
        String currentSectionId,
        @PositiveOrZero Integer optionIndex,
        @PositiveOrZero Integer health) {
}
