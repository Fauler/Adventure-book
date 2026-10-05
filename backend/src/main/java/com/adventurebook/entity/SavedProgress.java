package com.adventurebook.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A single saved playthrough (US-09/US-10, `docs/05-business-architecture.md` "Save /
 * Resume behavior"). {@code bookId} is both the primary key and the natural key: each
 * book has exactly **one** save slot (no auth yet, so no per-player partitioning) —
 * saving again overwrites the existing row rather than creating a new one. There is no
 * {@code status} column: a save only ever exists while the game is still
 * {@link GameStatus#PLAYING} — it is deleted the instant the game ends ({@code WON}/
 * {@code DEAD}), so a persisted row is never in a finished state.
 */
@Entity
@Table(name = "saved_progress")
public class SavedProgress {

    @Id
    @Column(name = "book_id")
    private String bookId;

    @Column(name = "current_section_id", nullable = false)
    private String currentSectionId;

    @Column(nullable = false)
    private int health;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SavedProgress() {
        // JPA
    }

    public SavedProgress(String bookId, String currentSectionId, int health, Instant updatedAt) {
        this.bookId = bookId;
        this.currentSectionId = currentSectionId;
        this.health = health;
        this.updatedAt = updatedAt;
    }

    public String bookId() {
        return bookId;
    }

    public String currentSectionId() {
        return currentSectionId;
    }

    public int health() {
        return health;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void update(String currentSectionId, int health, Instant updatedAt) {
        this.currentSectionId = currentSectionId;
        this.health = health;
        this.updatedAt = updatedAt;
    }
}
