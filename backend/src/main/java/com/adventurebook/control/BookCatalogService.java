package com.adventurebook.control;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;

import com.adventurebook.boundary.file.BookFileAdapter;
import com.adventurebook.entity.Book;
import com.adventurebook.entity.Difficulty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates the book catalog use case: runs the ingestion pipeline, holds the
 * resulting in-memory list of valid books, and serves list/search/filter queries.
 * No business rules of its own — validity rules live on {@link Book#validate()}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookCatalogService {

    private final BookFileAdapter bookFileAdapter;
    private final List<Book> catalog = new CopyOnWriteArrayList<>();

    /**
     * Runs the ingestion pipeline ({@code incoming/} &rarr; {@code valid/}/{@code
     * invalid/}) and rebuilds the in-memory catalog from {@code valid/}. Safe to call
     * repeatedly (e.g. at startup, and later on-demand once US-11 adds an endpoint).
     */
    public synchronized void ingestAndReload() {
        bookFileAdapter.ingestIncoming();
        List<Book> reloaded = bookFileAdapter.loadValidBooks();
        catalog.clear();
        catalog.addAll(reloaded);
        log.info("Book catalog reloaded: {} valid book(s)", catalog.size());
    }

    /**
     * Returns valid books matching the given criteria. {@code search} matches
     * case-insensitively against title or author; {@code difficulty} is an exact
     * match. Both are optional — {@code null}/blank means "no filter".
     */
    public List<Book> list(String search, Difficulty difficulty) {
        String query = (search == null || search.isBlank()) ? null : search.trim().toLowerCase();
        return catalog.stream()
                .filter(book -> difficulty == null || book.difficulty() == difficulty)
                .filter(book -> query == null
                        || book.title().toLowerCase().contains(query)
                        || book.author().toLowerCase().contains(query))
                .toList();
    }

    /**
     * Looks up a single valid book by id (e.g. for {@code /play}). Empty if {@code id}
     * doesn't exist or belongs to an invalid book — invalid books are never part of the
     * catalog in the first place.
     */
    public Optional<Book> findById(String id) {
        return catalog.stream().filter(book -> book.id().equals(id)).findFirst();
    }
}
