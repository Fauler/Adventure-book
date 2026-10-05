package com.adventurebook.entity;

/**
 * The complete next view model for a single {@code /play} move — reused as-is for the
 * API response shape (see the API contract in {@code docs/03-technical-architecture.md}).
 * The frontend renders this directly and never has to derive/compute any of its fields.
 *
 * <p>{@code consequenceText} is the flavor text of the just-resolved option's
 * {@link Consequence} (US-06's "I'm shown the consequence text" AC) — {@code null} when
 * the option carried no consequence, or on a fresh {@link Book#startGame()}.
 */
public record MoveResult(Section section, int health, GameStatus status, String consequenceText) {
}
