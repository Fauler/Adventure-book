package com.adventurebook.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class BookValidationTest {

    private static Section section(String id, SectionType type, Option... options) {
        return new Section(id, "text-" + id, type, options.length == 0 ? List.of() : List.of(options));
    }

    private static Option option(String gotoId) {
        return new Option("go to " + gotoId, gotoId, null);
    }

    private static Book book(Section... sections) {
        return new Book("test-book", "Title", "Author", Difficulty.EASY, List.of(sections));
    }

    @Test
    void validBookHasNoErrors() {
        Book book = book(
                section("1", SectionType.BEGIN, option("2")),
                section("2", SectionType.END));

        assertThat(book.validate()).isEmpty();
        assertThat(book.isValid()).isTrue();
    }

    @Test
    void rule1_noBeginSection_isInvalid() {
        Book book = book(section("1", SectionType.END));

        assertThat(book.validate()).contains("no BEGIN section found");
    }

    @Test
    void rule1_multipleBeginSections_isInvalid() {
        Book book = book(
                section("1", SectionType.BEGIN, option("3")),
                section("2", SectionType.BEGIN, option("3")),
                section("3", SectionType.END));

        assertThat(book.validate()).anyMatch(error -> error.contains("more than one BEGIN section found"));
    }

    @Test
    void rule2_noEndSection_isInvalid() {
        Book book = book(section("1", SectionType.BEGIN, option("1")));

        assertThat(book.validate()).contains("no END section found");
    }

    @Test
    void rule3_gotoIdDoesNotMatchAnySection_isInvalid() {
        Book book = book(
                section("1", SectionType.BEGIN, option("999")),
                section("2", SectionType.END));

        assertThat(book.validate())
                .anyMatch(error -> error.contains("gotoId \"999\" which does not match any existing section id"));
    }

    @Test
    void rule3_gotoIdComparisonIsTypeSafeAcrossNumericAndStringIds() {
        // id "1" (parsed from a JSON number) must match gotoId "1" (parsed from a JSON string).
        Book book = book(
                section("1", SectionType.BEGIN, option("500")),
                section("500", SectionType.END));

        assertThat(book.validate()).isEmpty();
    }

    @Test
    void rule4_nonEndSectionWithNoOptions_isInvalid() {
        Book book = book(
                section("1", SectionType.BEGIN, option("2")),
                section("2", SectionType.END),
                section("666", SectionType.NODE));

        assertThat(book.validate()).anyMatch(error -> error.contains("section 666 is a NODE with no options"));
    }

    @Test
    void rule4_checksAllSectionsNotOnlyReachableOnes() {
        // Section "666" is never referenced by any gotoId (unreachable from BEGIN) but must
        // still be checked, per docs/05-business-architecture.md.
        Book book = book(
                section("1", SectionType.BEGIN, option("2")),
                section("2", SectionType.END),
                section("666", SectionType.NODE));

        assertThat(book.validate()).isNotEmpty();
    }

    @Test
    void rule5_duplicateSectionId_isInvalid() {
        Book book = book(
                section("1", SectionType.BEGIN, option("2")),
                section("2", SectionType.END),
                section("2", SectionType.END));

        assertThat(book.validate()).contains("duplicate section id: 2");
    }

    @Test
    void rule5_duplicateSectionId_isReportedOnlyOncePerIdEvenWithThreeOrMoreDuplicates() {
        Book book = book(
                section("1", SectionType.BEGIN, option("2")),
                section("2", SectionType.END),
                section("2", SectionType.END),
                section("2", SectionType.END));

        assertThat(book.validate()).containsOnlyOnce("duplicate section id: 2");
    }

    @Test
    void collectsAllFailedRulesAtOnce() {
        Book book = book(section("1", SectionType.NODE));

        List<String> errors = book.validate();

        assertThat(errors).contains("no BEGIN section found", "no END section found");
        assertThat(errors).anyMatch(error -> error.contains("section 1 is a NODE with no options"));
    }

    @Test
    void missingOrUnrecognizedSectionType_isInvalid() {
        // "type": "" (or any unrecognized value) is normalized to null by
        // FlexibleSectionTypeDeserializer rather than crashing the whole book's parse —
        // see docs/00-PARKING_LOT.md's entry on malformed/inconsistent input handling.
        Book book = book(
                section("1", SectionType.BEGIN, option("2")),
                section("2", null),
                section("3", SectionType.END));

        assertThat(book.validate()).contains("section 2 has a missing or unrecognized type");
    }

    @Test
    void missingSectionType_doesNotAlsoProduceARedundantNoOptionsError() {
        Book book = book(
                section("1", SectionType.BEGIN, option("2")),
                section("2", null),
                section("3", SectionType.END));

        assertThat(book.validate()).noneMatch(error -> error.contains("with no options"));
    }
}
