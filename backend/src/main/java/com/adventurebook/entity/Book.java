package com.adventurebook.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.adventurebook.common.DomainException;
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

    /** The player's HP at the start of a fresh game (Objective 2/US-05) — the brief
     * only defines this as the *starting* value, not a hard ceiling. */
    public static final int STARTING_HEALTH = 10;

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

    /**
     * Looks up a section by id. Only meaningful on a book that already passed
     * {@link #validate()} — an invalid book has no gameplay guarantees.
     */
    public Optional<Section> findSection(String sectionId) {
        return sections.stream().filter(s -> s.id().equals(sectionId)).findFirst();
    }

    /**
     * Starts a brand-new game at the book's {@code BEGIN} section with
     * {@link #STARTING_HEALTH}, per Objective 2/US-05 (omitting {@code currentSectionId}
     * on {@code /play} means "start fresh"). A validated book is guaranteed to have
     * exactly one {@code BEGIN} section (rule 1); the exception here is only a defensive
     * fallback and should be structurally unreachable for a book that passed validation.
     */
    public MoveResult startGame() {
        Section begin = sections.stream()
                .filter(s -> s.type() == SectionType.BEGIN)
                .findFirst()
                .orElseThrow(() -> new DomainException(
                        "book \"" + id + "\" has no BEGIN section and cannot be played"));
        return new MoveResult(begin, STARTING_HEALTH, GameStatus.PLAYING, null);
    }

    /**
     * Resolves a single move: the player was last shown {@code currentSectionId} with
     * {@code health}, and picked the option at {@code optionIndex}. Applies that
     * option's {@link Consequence} (if any), clamped per
     * {@link Consequence#applyTo(int)}, per Objective 3/US-06. Every failure here is a
     * {@link DomainException} (never a 500) per the boundary/entity validation split in
     * {@code docs/03-technical-architecture.md}.
     */
    public MoveResult resolveMove(String currentSectionId, int optionIndex, int health) {
        Section current = findSection(currentSectionId)
                .orElseThrow(() -> new DomainException(
                        "section \"" + currentSectionId + "\" does not exist in book \"" + id + "\""));

        if (current.isEnding() || health <= 0) {
            throw new DomainException("the game has already ended; no further moves are possible");
        }

        List<Option> options = current.options() != null ? current.options() : List.of();
        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new DomainException(
                    "option index " + optionIndex + " does not exist on section \"" + currentSectionId + "\"");
        }

        Option chosen = options.get(optionIndex);
        Section next = findSection(chosen.gotoId())
                .orElseThrow(() -> new DomainException(
                        "option \"" + chosen.description() + "\" points to a section that does not exist"));

        Consequence consequence = chosen.consequence();
        int newHealth = consequence != null ? consequence.applyTo(health) : health;
        String consequenceText = consequence != null ? consequence.text() : null;

        // Death takes priority over a simultaneous END (rules 3+4 guarantee every
        // reachable section has valid options, but nothing forbids a consequence from
        // killing the player on the very move that would otherwise have won the game).
        GameStatus status;
        if (newHealth <= 0) {
            status = GameStatus.DEAD;
        } else if (next.isEnding()) {
            status = GameStatus.WON;
        } else {
            status = GameStatus.PLAYING;
        }

        return new MoveResult(next, newHealth, status, consequenceText);
    }
}
