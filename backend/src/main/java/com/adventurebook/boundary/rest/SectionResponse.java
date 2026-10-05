package com.adventurebook.boundary.rest;

import java.util.List;

import com.adventurebook.entity.Section;

/**
 * The section currently shown to the player. {@code options} is empty for an
 * {@code END} section — the frontend uses that (together with {@link PlayResponse#status()})
 * to render the ending state instead of a choice list.
 */
public record SectionResponse(String id, String text, List<OptionResponse> options) {

    public static SectionResponse from(Section section) {
        List<OptionResponse> options = section.options() == null
                ? List.of()
                : section.options().stream().map(OptionResponse::from).toList();
        return new SectionResponse(section.id(), section.text(), options);
    }
}
