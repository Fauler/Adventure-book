package com.adventurebook.boundary.rest;

import com.adventurebook.entity.Option;

/**
 * A single selectable choice as rendered to the player. Deliberately omits
 * {@code gotoId}/{@code consequence} — the frontend never needs the raw target section
 * id (it sends back the option's position via {@code optionIndex}, matching the array
 * index in {@link SectionResponse#options()}), and consequence effects/text aren't
 * surfaced yet (deferred to US-06/M3).
 */
public record OptionResponse(String description) {

    public static OptionResponse from(Option option) {
        return new OptionResponse(option.description());
    }
}
