package com.adventurebook.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.adventurebook.common.BookNotFoundException;
import com.adventurebook.entity.Book;
import com.adventurebook.entity.Consequence;
import com.adventurebook.entity.ConsequenceType;
import com.adventurebook.entity.Difficulty;
import com.adventurebook.entity.GameStatus;
import com.adventurebook.entity.MoveResult;
import com.adventurebook.entity.Option;
import com.adventurebook.entity.Section;
import com.adventurebook.entity.SectionType;

class GameEngineServiceTest {

    private BookCatalogService bookCatalogService;
    private ProgressService progressService;
    private GameEngineService gameEngineService;

    private static Book sampleBook() {
        return new Book("test-book", "Title", "Author", Difficulty.EASY, List.of(
                new Section("1", "begin", SectionType.BEGIN, List.of(new Option("go", "2", null))),
                new Section("2", "end", SectionType.END, List.of())));
    }

    private static Book lethalBook() {
        Consequence lethal = new Consequence(ConsequenceType.LOSE_HEALTH, 999, "It kills you.");
        return new Book("test-book", "Title", "Author", Difficulty.EASY, List.of(
                new Section("1", "begin", SectionType.BEGIN, List.of(new Option("go", "2", lethal))),
                new Section("2", "node", SectionType.NODE, List.of(new Option("go", "1", null)))));
    }

    @BeforeEach
    void setUp() {
        bookCatalogService = mock(BookCatalogService.class);
        progressService = mock(ProgressService.class);
        gameEngineService = new GameEngineService(bookCatalogService, progressService);
    }

    @Test
    void play_withNoCurrentSectionId_startsFreshGame() {
        when(bookCatalogService.findById("test-book")).thenReturn(Optional.of(sampleBook()));

        MoveResult result = gameEngineService.play("test-book", null, null, null);

        assertThat(result.section().id()).isEqualTo("1");
        assertThat(result.health()).isEqualTo(Book.STARTING_HEALTH);
        assertThat(result.status()).isEqualTo(GameStatus.PLAYING);
    }

    @Test
    void play_withCurrentSectionId_resolvesMove() {
        when(bookCatalogService.findById("test-book")).thenReturn(Optional.of(sampleBook()));

        MoveResult result = gameEngineService.play("test-book", "1", 0, Book.STARTING_HEALTH);

        assertThat(result.section().id()).isEqualTo("2");
        assertThat(result.status()).isEqualTo(GameStatus.WON);
    }

    @Test
    void play_unknownBookId_throwsBookNotFoundException() {
        when(bookCatalogService.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameEngineService.play("missing", null, null, null))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void play_resultingInWon_autoClearsSavedProgress() {
        when(bookCatalogService.findById("test-book")).thenReturn(Optional.of(sampleBook()));

        gameEngineService.play("test-book", "1", 0, Book.STARTING_HEALTH);

        verify(progressService).delete("test-book");
    }

    @Test
    void play_resultingInDead_autoClearsSavedProgress() {
        when(bookCatalogService.findById("test-book")).thenReturn(Optional.of(lethalBook()));

        MoveResult result = gameEngineService.play("test-book", "1", 0, Book.STARTING_HEALTH);

        assertThat(result.status()).isEqualTo(GameStatus.DEAD);
        verify(progressService).delete("test-book");
    }

    @Test
    void play_stillPlaying_doesNotClearSavedProgress() {
        when(bookCatalogService.findById("test-book")).thenReturn(Optional.of(lethalBook()));

        gameEngineService.play("test-book", "2", 0, Book.STARTING_HEALTH);

        verify(progressService, never()).delete("test-book");
    }
}
