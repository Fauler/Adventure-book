package com.adventurebook.boundary.rest;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.adventurebook.control.BookCatalogService;
import com.adventurebook.control.GameEngineService;
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
}
