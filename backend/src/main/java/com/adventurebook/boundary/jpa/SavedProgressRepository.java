package com.adventurebook.boundary.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.adventurebook.entity.SavedProgress;

/**
 * Spring Data repository for {@link SavedProgress} (US-09/US-10). {@code bookId} is the
 * primary key, so {@code findById}/{@code deleteById} already give the "single save slot
 * per book" semantics for free — no custom queries needed.
 */
public interface SavedProgressRepository extends JpaRepository<SavedProgress, String> {
}
