package com.adventurebook.boundary.rest;

import java.util.List;

import com.adventurebook.entity.Book;
import com.adventurebook.entity.Difficulty;

/**
 * Full detail of a valid book, per {@code GET /api/books/{id}} in the API contract
 * (`docs/03-technical-architecture.md`). Used by the frontend to render the "begin
 * quest" step and, per `docs/05-business-architecture.md`'s "Save / Resume behavior",
 * fetched in parallel with the save check on opening the game screen so a **Continue**
 * choice can jump straight to the saved section without a round trip through
 * {@code /play} (which is reserved for *resolving* a move, not re-displaying one).
 *
 * <p>Same intentional omission as {@link BookSummaryResponse}: no {@code tags}/
 * {@code description} fields, since the real book JSON has none (see
 * `docs/00-PARKING_LOT.md` entry 5).
 */
public record BookDetailResponse(
        String id, String title, String author, Difficulty difficulty, List<SectionResponse> sections) {

    public static BookDetailResponse from(Book book) {
        List<SectionResponse> sections = book.sections() == null
                ? List.of()
                : book.sections().stream().map(SectionResponse::from).toList();
        return new BookDetailResponse(book.id(), book.title(), book.author(), book.difficulty(), sections);
    }
}
