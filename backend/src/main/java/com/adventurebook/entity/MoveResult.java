package com.adventurebook.entity;

/**
 * The complete next view model for a single {@code /play} move — reused as-is for the
 * API response shape (see the API contract in {@code docs/03-technical-architecture.md}).
 * The frontend renders this directly and never has to derive/compute any of its fields.
 */
public record MoveResult(Section section, int health, GameStatus status) {
}
