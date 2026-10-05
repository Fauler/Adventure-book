package com.adventurebook.control;

import org.springframework.stereotype.Service;

import com.adventurebook.common.BookNotFoundException;
import com.adventurebook.common.DomainException;
import com.adventurebook.entity.Book;
import com.adventurebook.entity.MoveResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates a single {@code /play} move: loads the requested book from the catalog
 * and delegates move resolution to the domain ({@link Book#startGame()} /
 * {@link Book#resolveMove(String, int, int)}). Stateless — per
 * {@code docs/03-technical-architecture.md}, every call is self-contained; no game
 * session is held in memory between requests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameEngineService {

    private final BookCatalogService bookCatalogService;

    /**
     * Resolves one move. {@code currentSectionId == null} (or blank) means "start a
     * fresh game at BEGIN", per the API contract. Continuing an existing game requires
     * both {@code optionIndex} and {@code health} to be present.
     */
    public MoveResult play(String bookId, String currentSectionId, Integer optionIndex, Integer health) {
        Book book = bookCatalogService.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("no valid book found with id \"" + bookId + "\""));

        if (currentSectionId == null || currentSectionId.isBlank()) {
            log.info("Starting new game: bookId={}", bookId);
            return book.startGame();
        }

        if (optionIndex == null || health == null) {
            throw new DomainException("optionIndex and health are required to continue an existing game");
        }

        log.info("Resolving move: bookId={}, currentSectionId={}, optionIndex={}", bookId, currentSectionId,
                optionIndex);
        return book.resolveMove(currentSectionId, optionIndex, health);
    }
}
