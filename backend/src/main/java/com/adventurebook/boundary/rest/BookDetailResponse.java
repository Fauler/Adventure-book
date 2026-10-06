package com.adventurebook.boundary.rest;

import java.util.List;

import com.adventurebook.entity.Book;
import com.adventurebook.entity.Difficulty;

/**
 * Full detail of a valid book, per {@code GET /api/books/{id}} in the API contract
 * (`docs/03-technical-architecture.md`). Used by the frontend to render the "begin
 * quest" step and, per `docs/05-business-architecture.md`'s "Save / Resume behavior",
 * fetched first on opening the game screen (before the save-exists check, which needs
 * this response's {@code sections} to detect a stale/broken resume) so a **Continue**
 * choice can jump straight to the saved section without a round trip through
 * {@code /play} (which is reserved for *resolving* a move, not re-displaying one).
 *
 * <p>Same optional display metadata as {@link BookSummaryResponse} — {@code type},
 * {@code estimatedDuration}, {@code chapterCount}, {@code tags}, {@code description} —
 * all real, author-provided fields in the book JSON (see `docs/00-PARKING_LOT.md`
 * entry 5), {@code null}/empty when absent.
 */
public record BookDetailResponse(
        String id,
        String title,
        String author,
        Difficulty difficulty,
        List<SectionResponse> sections,
        String type,
        String estimatedDuration,
        Integer chapterCount,
        List<String> tags,
        String description) {

    public static BookDetailResponse from(Book book) {
        List<SectionResponse> sections = book.sections() == null
                ? List.of()
                : book.sections().stream().map(SectionResponse::from).toList();
        return new BookDetailResponse(
                book.id(),
                book.title(),
                book.author(),
                book.difficulty(),
                sections,
                book.type(),
                book.estimatedDuration(),
                book.chapterCount(),
                book.tags(),
                book.description());
    }
}
