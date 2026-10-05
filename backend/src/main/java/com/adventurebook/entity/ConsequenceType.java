package com.adventurebook.entity;

/**
 * Kinds of effect a {@link Consequence} can have on the player. The sample books use
 * both {@link #LOSE_HEALTH} and {@link #GAIN_HEALTH}; the enum is deliberately left
 * open so new effect types can be added later without reshaping {@link Consequence}
 * itself.
 */
public enum ConsequenceType {
    LOSE_HEALTH,
    GAIN_HEALTH
}
