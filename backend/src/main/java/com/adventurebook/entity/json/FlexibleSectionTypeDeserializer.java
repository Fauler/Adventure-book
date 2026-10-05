package com.adventurebook.entity.json;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

import com.adventurebook.entity.SectionType;

/**
 * Deserializes a section's {@code type} leniently: blank/missing/unrecognized values
 * (e.g. {@code ""} or a typo) become {@code null} instead of aborting the whole book's
 * parse with a generic Jackson exception. {@link com.adventurebook.entity.Book#validate()}
 * turns a {@code null} type into its own specific, diagnosable validation error — see
 * `docs/00-PARKING_LOT.md` entry on malformed/inconsistent input handling.
 */
public final class FlexibleSectionTypeDeserializer extends ValueDeserializer<SectionType> {

    @Override
    public SectionType deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode node = p.readValueAsTree();
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return SectionType.valueOf(text.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
