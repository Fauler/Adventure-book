package com.adventurebook.entity;

import java.util.List;

import com.adventurebook.entity.json.FlexibleSectionTypeDeserializer;
import com.adventurebook.entity.json.FlexibleStringDeserializer;

import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * One numbered passage of a book. {@code options} is empty/absent only when
 * {@code type == END} — see {@link #hasOptions()}. {@code type} is {@code null} when
 * the JSON value was blank/missing/unrecognized (e.g. {@code "type": ""}) — see
 * {@link FlexibleSectionTypeDeserializer}; {@link Book#validate()} turns that into its
 * own specific validation error rather than letting the whole book fail to parse.
 */
public record Section(
        @JsonDeserialize(using = FlexibleStringDeserializer.class) String id,
        String text,
        @JsonDeserialize(using = FlexibleSectionTypeDeserializer.class) SectionType type,
        List<Option> options) {

    public boolean isEnding() {
        return type == SectionType.END;
    }

    public boolean hasOptions() {
        return options != null && !options.isEmpty();
    }
}
