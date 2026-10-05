package com.adventurebook.entity.json;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

/**
 * Parses a {@code consequence.value} regardless of whether it was authored as a JSON
 * number or a JSON string (the sample data quotes it, e.g. {@code "value": "6"}).
 */
public final class FlexibleIntDeserializer extends ValueDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode node = p.readValueAsTree();
        return (node == null || node.isNull()) ? null : node.asInt();
    }
}
