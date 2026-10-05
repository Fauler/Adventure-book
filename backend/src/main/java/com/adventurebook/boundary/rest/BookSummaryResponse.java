package com.adventurebook.boundary.rest;

import com.adventurebook.entity.Book;
import com.adventurebook.entity.Difficulty;

/**
 * Summarized view of a valid book for {@code GET /api/books}, per the API contract in
 * `docs/03-technical-architecture.md`.
 *
 * <p>Note: the contract table also lists {@code tags}/{@code description} on this
 * response, but the real book JSON format has no such fields today (only
 * {@code title}/{@code author}/{@code difficulty} are real data) — see
 * `docs/00-PARKING_LOT.md` entry 5. Those fields are intentionally omitted here rather
 * than fabricated; revisit once that parking-lot item is resolved.
 */
public record BookSummaryResponse(String id, String title, String author, Difficulty difficulty) {

    public static BookSummaryResponse from(Book book) {
        return new BookSummaryResponse(book.id(), book.title(), book.author(), book.difficulty());
    }
}
