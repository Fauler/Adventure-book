package com.adventurebook.entity;

/**
 * The outcome of a single {@code /play} move. Per
 * {@code docs/05-business-architecture.md} ("Game-end states: only WON or DEAD"), a
 * book that passed validation can never land on a missing section or a dead end during
 * play, so these are the only three possible states — there is no third/"stuck"
 * end-state.
 */
public enum GameStatus {
    PLAYING,
    WON,
    DEAD
}
