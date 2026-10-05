package com.adventurebook.boundary.rest;

import com.adventurebook.entity.GameStatus;
import com.adventurebook.entity.MoveResult;

/**
 * Response body for {@code POST /api/books/{id}/play}, per the API contract in
 * {@code docs/03-technical-architecture.md}. {@code consequenceText} is the flavor
 * text of the option's {@link com.adventurebook.entity.Consequence} that just applied
 * (US-06's "I'm shown the consequence text" AC) — {@code null} when none applied.
 */
public record PlayResponse(SectionResponse section, int health, GameStatus status, String consequenceText) {

    public static PlayResponse from(MoveResult result) {
        return new PlayResponse(
                SectionResponse.from(result.section()), result.health(), result.status(), result.consequenceText());
    }
}
