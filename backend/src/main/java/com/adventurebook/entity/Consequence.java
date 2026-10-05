package com.adventurebook.entity;

import com.adventurebook.entity.json.FlexibleIntDeserializer;

import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * An effect applied to the player's health when the option carrying it is chosen.
 * {@link #applyTo(int)} is written so new {@link ConsequenceType} values can be added
 * without touching callers.
 */
public record Consequence(
        ConsequenceType type,
        @JsonDeserialize(using = FlexibleIntDeserializer.class) int value,
        String text) {

    /**
     * Applies this consequence to the given health, clamped at a floor of 0 (there is
     * no hard-coded ceiling — 10 is only the documented starting value).
     */
    public int applyTo(int health) {
        return switch (type) {
            case LOSE_HEALTH -> Math.max(0, health - value);
            case GAIN_HEALTH -> health + value;
        };
    }
}
