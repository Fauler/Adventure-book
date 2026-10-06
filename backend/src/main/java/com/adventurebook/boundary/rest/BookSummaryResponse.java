package com.adventurebook.boundary.rest;

import java.util.List;

import com.adventurebook.entity.Book;
import com.adventurebook.entity.Difficulty;

/**
 * Summarized view of a valid book for {@code GET /api/books}, per the API contract in
 * `docs/03-technical-architecture.md`.
 *
 * <p>{@code type}, {@code estimatedDuration}, {@code chapterCount}, {@code tags}, and
 * {@code description} are all optional, author-provided display metadata — real fields
 * in the book JSON (see `docs/00-PARKING_LOT.md` entry 5), {@code null}/empty when the
 * book's own JSON leaves them blank/absent, letting the frontend hide them rather than
 * show an empty tag/badge.
 */
public record BookSummaryResponse(
        String id,
        String title,
        String author,
        Difficulty difficulty,
        String type,
        String estimatedDuration,
        Integer chapterCount,
        List<String> tags,
        String description) {

    public static BookSummaryResponse from(Book book) {
        return new BookSummaryResponse(
                book.id(),
                book.title(),
                book.author(),
                book.difficulty(),
                book.type(),
                book.estimatedDuration(),
                book.chapterCount(),
                book.tags(),
                book.description());
    }
}
