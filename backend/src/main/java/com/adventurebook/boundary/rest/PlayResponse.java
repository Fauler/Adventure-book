package com.adventurebook.boundary.rest;

import com.adventurebook.entity.GameStatus;
import com.adventurebook.entity.MoveResult;

/**
 * Response body for {@code POST /api/books/{id}/play}, per the API contract in
 * {@code docs/03-technical-architecture.md}.
 */
public record PlayResponse(SectionResponse section, int health, GameStatus status) {

    public static PlayResponse from(MoveResult result) {
        return new PlayResponse(SectionResponse.from(result.section()), result.health(), result.status());
    }
}
