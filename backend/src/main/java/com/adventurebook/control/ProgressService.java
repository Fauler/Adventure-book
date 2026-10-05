package com.adventurebook.control;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adventurebook.boundary.jpa.SavedProgressRepository;
import com.adventurebook.common.BookNotFoundException;
import com.adventurebook.common.DomainException;
import com.adventurebook.entity.Book;
import com.adventurebook.entity.SavedProgress;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates the save/resume use case (US-09/US-10, `docs/05-business-architecture.md`
 * "Save / Resume behavior"): single save slot per book, upsert-on-save, explicit
 * delete (Restart / "discard save"), and auto-clear once a playthrough ends.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final SavedProgressRepository savedProgressRepository;
    private final BookCatalogService bookCatalogService;

    /** Returns the single saved row for a book, or empty if nothing is saved. */
    @Transactional(readOnly = true)
    public Optional<SavedProgress> find(String bookId) {
        return savedProgressRepository.findById(bookId);
    }

    /**
     * Upserts the single save slot for {@code bookId}. Validates that the book exists
     * and that {@code currentSectionId} is one of its real sections — the backend never
     * persists a save that couldn't later be resumed (per "backend is the source of
     * truth").
     */
    @Transactional
    public SavedProgress save(String bookId, String currentSectionId, int health) {
        Book book = bookCatalogService.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("no valid book found with id \"" + bookId + "\""));

        if (currentSectionId == null || currentSectionId.isBlank()) {
            throw new DomainException("currentSectionId is required to save progress");
        }
        book.findSection(currentSectionId)
                .orElseThrow(() -> new DomainException(
                        "section \"" + currentSectionId + "\" does not exist in book \"" + bookId + "\""));
        if (health <= 0) {
            throw new DomainException("cannot save a game that has already ended (health <= 0)");
        }

        SavedProgress existing = savedProgressRepository.findById(bookId).orElse(null);
        Instant now = Instant.now();
        if (existing != null) {
            existing.update(currentSectionId, health, now);
            log.info("Progress overwritten: bookId={}, currentSectionId={}", bookId, currentSectionId);
            return savedProgressRepository.save(existing);
        }

        log.info("Progress saved: bookId={}, currentSectionId={}", bookId, currentSectionId);
        return savedProgressRepository.save(new SavedProgress(bookId, currentSectionId, health, now));
    }

    /**
     * Clears the save slot for a book, if any. Idempotent — a no-op when nothing was
     * saved (used both for explicit "Restart"/"discard" actions and the automatic
     * clear-on-game-end performed by {@link GameEngineService}).
     */
    @Transactional
    public void delete(String bookId) {
        if (savedProgressRepository.existsById(bookId)) {
            savedProgressRepository.deleteById(bookId);
            log.info("Progress cleared: bookId={}", bookId);
        }
    }
}
