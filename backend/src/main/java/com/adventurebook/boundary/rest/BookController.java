package com.adventurebook.boundary.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.adventurebook.common.BookNotFoundException;
import com.adventurebook.control.BookCatalogService;
import com.adventurebook.control.GameEngineService;
import com.adventurebook.control.ProgressService;
import com.adventurebook.entity.Book;
import com.adventurebook.entity.Difficulty;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Book catalog endpoints, per the API contract in `docs/03-technical-architecture.md`.
 */
@Slf4j
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookCatalogService bookCatalogService;
    private final GameEngineService gameEngineService;
    private final ProgressService progressService;

    /**
     * Lists valid books, optionally narrowed by free-text {@code search} (matched
     * against title/author) and/or an exact {@code difficulty}.
     *
     * <p>{@code tags} is accepted for forward-compatibility with the API contract
     * table but currently has no effect: the real book JSON format has no tag data
     * yet (see `docs/00-PARKING_LOT.md` entry 5).
     */
    @GetMapping
    public List<BookSummaryResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) List<String> tags) {
        log.info("Listing books: search={}, difficulty={}", search, difficulty);
        return bookCatalogService.list(search, difficulty).stream()
                .map(BookSummaryResponse::from)
                .toList();
    }

    /**
     * Resolves a single move (Objective 2/US-05). Omit {@code currentSectionId} (and
     * therefore {@code optionIndex}/{@code health}) in the request body to start a
     * fresh game at the book's {@code BEGIN} section.
     */
    @PostMapping("/{id}/play")
    public PlayResponse play(@PathVariable String id, @Valid @RequestBody PlayRequest request) {
        log.info("Play request: bookId={}, currentSectionId={}", id, request.currentSectionId());
        var result = gameEngineService.play(id, request.currentSectionId(), request.optionIndex(), request.health());
        return PlayResponse.from(result);
    }

    /**
     * Full detail of a single valid book (all sections). Fetched by the frontend both
     * to render the "begin quest" step and, per `docs/05-business-architecture.md`, in
     * parallel with {@link #getProgress} on opening the game screen so a resumed game
     * can jump straight to the saved section's content.
     */
    @GetMapping("/{id}")
    public BookDetailResponse getDetail(@PathVariable String id) {
        Book book = bookCatalogService.findById(id)
                .orElseThrow(() -> new BookNotFoundException("no valid book found with id \"" + id + "\""));
        return BookDetailResponse.from(book);
    }

    /**
     * Returns the saved progress for this book, or {@code 204 No Content} if nothing is
     * saved (US-10 — "no save exists" means straight into a fresh playthrough, no
     * modal).
     */
    @GetMapping("/{id}/progress")
    public ResponseEntity<ProgressResponse> getProgress(@PathVariable String id) {
        return progressService.find(id)
                .map(ProgressResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Saves (or overwrites) the single save slot for this book (US-09).
     */
    @PutMapping("/{id}/progress")
    public ProgressResponse putProgress(@PathVariable String id, @Valid @RequestBody ProgressRequest request) {
        log.info("Save request: bookId={}, currentSectionId={}", id, request.currentSectionId());
        var saved = progressService.save(id, request.currentSectionId(), request.health());
        return ProgressResponse.from(saved);
    }

    /**
     * Clears the save slot for this book (explicit "Restart"/"discard" action, US-10).
     * Idempotent — also a {@code 204} if nothing was saved.
     */
    @DeleteMapping("/{id}/progress")
    public ResponseEntity<Void> deleteProgress(@PathVariable String id) {
        log.info("Discard-save request: bookId={}", id);
        progressService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
