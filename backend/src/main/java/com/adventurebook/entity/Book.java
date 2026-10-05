package com.adventurebook.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The domain model for a single adventure book. {@code id} is not part of the book's
 * own JSON — it is assigned by the ingestion pipeline (the book file's slug, per
 * {@code docs/05-business-architecture.md}) once the file has been parsed.
 *
 * <p>{@link #validate()} is the single place the 4 book-validity rules (see
 * {@code docs/05-business-architecture.md}) are implemented — the ingestion pipeline
 * and the add-a-book endpoint both call this method and never duplicate/relax the
 * rules themselves.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Book(String id, String title, String author, Difficulty difficulty, List<Section> sections) {

    /**
     * Returns every failed rule, in plain English, ready to be written 1:1 into an
     * {@code .errors.txt} file. An empty list means the book is valid.
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        List<Section> allSections = sections != null ? sections : List.of();

        long beginCount = allSections.stream().filter(s -> s.type() == SectionType.BEGIN).count();
        if (beginCount == 0) {
            errors.add("no BEGIN section found");
        } else if (beginCount > 1) {
            errors.add("more than one BEGIN section found (found " + beginCount + ")");
        }

        boolean hasEnd = allSections.stream().anyMatch(Section::isEnding);
        if (!hasEnd) {
            errors.add("no END section found");
        }

        Set<String> sectionIds = new HashSet<>();
        for (Section section : allSections) {
            sectionIds.add(section.id());
        }
        for (Section section : allSections) {
            List<Option> options = section.options() != null ? section.options() : List.of();
            for (Option option : options) {
                if (option.gotoId() == null || !sectionIds.contains(option.gotoId())) {
                    errors.add("option \"" + option.description() + "\" at section " + section.id()
                            + " has gotoId \"" + option.gotoId() + "\" which does not match any existing section id");
                }
            }
        }

        for (Section section : allSections) {
            if (section.type() == null) {
                errors.add("section " + section.id() + " has a missing or unrecognized type");
            }
        }

        for (Section section : allSections) {
            if (section.type() != null && !section.isEnding() && !section.hasOptions()) {
                errors.add("section " + section.id() + " is a " + section.type() + " with no options");
            }
        }

        return errors;
    }

    public boolean isValid() {
        return validate().isEmpty();
    }
}
