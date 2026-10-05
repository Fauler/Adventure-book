package com.adventurebook.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.adventurebook.boundary.jpa.SavedProgressRepository;
import com.adventurebook.common.BookNotFoundException;
import com.adventurebook.common.DomainException;
import com.adventurebook.entity.Book;
import com.adventurebook.entity.Difficulty;
import com.adventurebook.entity.Option;
import com.adventurebook.entity.SavedProgress;
import com.adventurebook.entity.Section;
import com.adventurebook.entity.SectionType;

class ProgressServiceTest {

    private SavedProgressRepository savedProgressRepository;
    private BookCatalogService bookCatalogService;
    private ProgressService progressService;

    private static Book sampleBook() {
        return new Book("test-book", "Title", "Author", Difficulty.EASY, List.of(
                new Section("1", "begin", SectionType.BEGIN, List.of(new Option("go", "2", null))),
                new Section("2", "end", SectionType.END, List.of())));
    }

    @BeforeEach
    void setUp() {
        savedProgressRepository = mock(SavedProgressRepository.class);
        bookCatalogService = mock(BookCatalogService.class);
        progressService = new ProgressService(savedProgressRepository, bookCatalogService);
    }

    @Test
    void find_delegatesToRepository() {
        SavedProgress saved = new SavedProgress("test-book", "1", 7, java.time.Instant.now());
        when(savedProgressRepository.findById("test-book")).thenReturn(Optional.of(saved));

        assertThat(progressService.find("test-book")).contains(saved);
    }

    @Test
    void save_newSave_persistsIt() {
        when(bookCatalogService.findById("test-book")).thenReturn(Optional.of(sampleBook()));
        when(savedProgressRepository.findById("test-book")).thenReturn(Optional.empty());
        when(savedProgressRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SavedProgress result = progressService.save("test-book", "1", 7);

        assertThat(result.bookId()).isEqualTo("test-book");
        assertThat(result.currentSectionId()).isEqualTo("1");
        assertThat(result.health()).isEqualTo(7);
    }

    @Test
    void save_existingSave_overwritesIt() {
        SavedProgress existing = new SavedProgress("test-book", "1", 9, java.time.Instant.now().minusSeconds(60));
        when(bookCatalogService.findById("test-book")).thenReturn(Optional.of(sampleBook()));
        when(savedProgressRepository.findById("test-book")).thenReturn(Optional.of(existing));
        when(savedProgressRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SavedProgress result = progressService.save("test-book", "2", 3);

        assertThat(result).isSameAs(existing);
        assertThat(result.currentSectionId()).isEqualTo("2");
        assertThat(result.health()).isEqualTo(3);
    }

    @Test
    void save_unknownBook_throwsBookNotFoundException() {
        when(bookCatalogService.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> progressService.save("missing", "1", 7))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void save_sectionDoesNotExistInBook_throwsDomainException() {
        when(bookCatalogService.findById("test-book")).thenReturn(Optional.of(sampleBook()));

        assertThatThrownBy(() -> progressService.save("test-book", "999", 7))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void save_zeroOrNegativeHealth_throwsDomainException() {
        when(bookCatalogService.findById("test-book")).thenReturn(Optional.of(sampleBook()));

        assertThatThrownBy(() -> progressService.save("test-book", "1", 0))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void delete_existingSave_removesIt() {
        when(savedProgressRepository.existsById("test-book")).thenReturn(true);

        progressService.delete("test-book");

        org.mockito.Mockito.verify(savedProgressRepository).deleteById("test-book");
    }

    @Test
    void delete_noSave_isNoOp() {
        when(savedProgressRepository.existsById("test-book")).thenReturn(false);

        progressService.delete("test-book");

        org.mockito.Mockito.verify(savedProgressRepository, org.mockito.Mockito.never()).deleteById(
                org.mockito.ArgumentMatchers.any());
    }
}
