package com.adventurebook.entity.json;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.adventurebook.entity.Book;
import com.adventurebook.entity.SectionType;

import tools.jackson.databind.json.JsonMapper;

/**
 * Exercises the custom deserializers directly against raw, deliberately
 * malformed/inconsistent JSON (unquoted numeric ids, mixed numeric/string ids, and a
 * blank section {@code type}) — see docs/00-PARKING_LOT.md's entry on malformed/
 * inconsistent input handling. These are not part of the 4 official validity rules,
 * but the pipeline must still parse such files without crashing and flag the issue
 * clearly rather than silently accepting or rejecting the whole file with a generic
 * "not valid JSON" message.
 */
class BookJsonParsingTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void unquotedNumericGotoIdParsesAndMatchesTheTargetSectionId() {
        String json = """
                {
                  "title": "T", "author": "A", "difficulty": "EASY",
                  "sections": [
                    {"id": 1, "text": "start", "type": "BEGIN",
                     "options": [{"description": "go", "gotoId": 1000}]},
                    {"id": 1000, "text": "end", "type": "END"}
                  ]
                }
                """;

        Book parsed = jsonMapper.readValue(json, Book.class);
        Book book = new Book("test", parsed.title(), parsed.author(), parsed.difficulty(), parsed.sections());

        assertThat(book.validate()).isEmpty();
        assertThat(book.sections().get(0).options().get(0).gotoId()).isEqualTo("1000");
    }

    @Test
    void mixedNumericAndQuotedStringIdsAcrossSectionsAreNormalizedToTheSameType() {
        String json = """
                {
                  "title": "T", "author": "A", "difficulty": "EASY",
                  "sections": [
                    {"id": 1, "text": "start", "type": "BEGIN",
                     "options": [{"description": "go", "gotoId": "500"}]},
                    {"id": "500", "text": "mid", "type": "NODE",
                     "options": [{"description": "go", "gotoId": 1000}]},
                    {"id": 1000, "text": "end", "type": "END"}
                  ]
                }
                """;

        Book parsed = jsonMapper.readValue(json, Book.class);
        Book book = new Book("test", parsed.title(), parsed.author(), parsed.difficulty(), parsed.sections());

        assertThat(book.validate()).isEmpty();
    }

    @Test
    void blankSectionTypeParsesToNullInsteadOfThrowing() {
        String json = """
                {
                  "title": "T", "author": "A", "difficulty": "EASY",
                  "sections": [
                    {"id": 1, "text": "start", "type": "BEGIN",
                     "options": [{"description": "go", "gotoId": 2}]},
                    {"id": 2, "text": "broken", "type": ""},
                    {"id": 3, "text": "end", "type": "END"}
                  ]
                }
                """;

        Book parsed = jsonMapper.readValue(json, Book.class);

        assertThat(parsed.sections().get(1).type()).isNull();

        Book book = new Book("test", parsed.title(), parsed.author(), parsed.difficulty(), parsed.sections());
        assertThat(book.validate()).contains("section 2 has a missing or unrecognized type");
    }

    @Test
    void unrecognizedSectionTypeValueParsesToNullInsteadOfThrowing() {
        String json = """
                {
                  "title": "T", "author": "A", "difficulty": "EASY",
                  "sections": [
                    {"id": 1, "text": "start", "type": "BEGUN"},
                    {"id": 2, "text": "end", "type": "END"}
                  ]
                }
                """;

        Book parsed = jsonMapper.readValue(json, Book.class);

        assertThat(parsed.sections().get(0).type()).isNull();
        assertThat(parsed.sections().stream().noneMatch(s -> s.type() == SectionType.BEGIN)).isTrue();
    }

    @Test
    void blankOrAbsentBookLevelTypeNormalizesToNull() {
        String blankType = """
                {
                  "title": "T", "author": "A", "difficulty": "EASY", "type": "",
                  "sections": [{"id": 1, "text": "start", "type": "BEGIN"}]
                }
                """;
        String absentType = """
                {
                  "title": "T", "author": "A", "difficulty": "EASY",
                  "sections": [{"id": 1, "text": "start", "type": "BEGIN"}]
                }
                """;

        assertThat(jsonMapper.readValue(blankType, Book.class).type()).isNull();
        assertThat(jsonMapper.readValue(absentType, Book.class).type()).isNull();
    }

    @Test
    void nonBlankBookLevelTypeIsPreservedAndTrimmed() {
        String json = """
                {
                  "title": "T", "author": "A", "difficulty": "EASY", "type": "  Fantasy  ",
                  "sections": [{"id": 1, "text": "start", "type": "BEGIN"}]
                }
                """;

        assertThat(jsonMapper.readValue(json, Book.class).type()).isEqualTo("Fantasy");
    }

    @Test
    void absentOptionalDisplayMetadataNormalizesToNullOrEmptyList() {
        String json = """
                {
                  "title": "T", "author": "A", "difficulty": "EASY",
                  "sections": [{"id": 1, "text": "start", "type": "BEGIN"}]
                }
                """;

        Book book = jsonMapper.readValue(json, Book.class);

        assertThat(book.estimatedDuration()).isNull();
        assertThat(book.chapterCount()).isNull();
        assertThat(book.tags()).isEmpty();
        assertThat(book.description()).isNull();
    }

    @Test
    void nonBlankOptionalDisplayMetadataIsPreservedAndTrimmed() {
        String json = """
                {
                  "title": "T", "author": "A", "difficulty": "EASY",
                  "estimatedDuration": " 45-60 min ", "chapterCount": 12,
                  "tags": [" Magic ", "", "Underground", null],
                  "description": " A short blurb ",
                  "sections": [{"id": 1, "text": "start", "type": "BEGIN"}]
                }
                """;

        Book book = jsonMapper.readValue(json, Book.class);

        assertThat(book.estimatedDuration()).isEqualTo("45-60 min");
        assertThat(book.chapterCount()).isEqualTo(12);
        assertThat(book.tags()).containsExactly("Magic", "Underground");
        assertThat(book.description()).isEqualTo("A short blurb");
    }
}
