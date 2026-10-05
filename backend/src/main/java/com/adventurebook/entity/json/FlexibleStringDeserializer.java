package com.adventurebook.entity.json;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

/**
 * Normalizes a JSON section/option id to a {@link String} regardless of whether it was
 * authored as a JSON number or a JSON string in the book file — the sample data mixes
 * both (e.g. {@code 1} vs. {@code "500"}), and the 4 book-validity rules require
 * comparing ids as the same type.
 */
public final class FlexibleStringDeserializer extends ValueDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode node = p.readValueAsTree();
        return (node == null || node.isNull()) ? null : node.asText();
    }
}
