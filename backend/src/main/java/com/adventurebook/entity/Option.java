package com.adventurebook.entity;

import com.adventurebook.entity.json.FlexibleStringDeserializer;

import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * A single choice offered to the player at the end of a {@link Section}.
 */
public record Option(
        String description,
        @JsonDeserialize(using = FlexibleStringDeserializer.class) String gotoId,
        Consequence consequence) {
}
