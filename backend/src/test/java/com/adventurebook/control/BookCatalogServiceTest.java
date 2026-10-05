package com.adventurebook.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.adventurebook.boundary.file.BookFileAdapter;
import com.adventurebook.entity.Book;
import com.adventurebook.entity.Difficulty;

class BookCatalogServiceTest {

    private BookFileAdapter bookFileAdapter;
    private BookCatalogService service;

    @BeforeEach
    void setUp() {
        bookFileAdapter = mock(BookFileAdapter.class);
        service = new BookCatalogService(bookFileAdapter);

        when(bookFileAdapter.loadValidBooks()).thenReturn(List.of(
                new Book("crystal-caverns", "The Crystal Caverns", "Evelyn Stormrider", Difficulty.EASY, List.of()),
                new Book("pirates-jade-sea", "Pirates of the Jade Sea", "Some Author", Difficulty.MEDIUM, List.of())));

        service.ingestAndReload();
    }

    @Test
    void listsAllValidBooksWithNoFilter() {
        assertThat(service.list(null, null)).hasSize(2);
    }

    @Test
    void filtersByDifficulty() {
        assertThat(service.list(null, Difficulty.EASY))
                .extracting(Book::id)
                .containsExactly("crystal-caverns");
    }

    @Test
    void searchesByTitleCaseInsensitive() {
        assertThat(service.list("crystal", null))
                .extracting(Book::id)
                .containsExactly("crystal-caverns");
    }

    @Test
    void searchesByAuthorCaseInsensitive() {
        assertThat(service.list("STORMRIDER", null))
                .extracting(Book::id)
                .containsExactly("crystal-caverns");
    }

    @Test
    void noMatchReturnsEmptyList() {
        assertThat(service.list("nonexistent-book-xyz", null)).isEmpty();
    }

    @Test
    void searchAndDifficultyCombineWithAnd() {
        assertThat(service.list("crystal", Difficulty.MEDIUM)).isEmpty();
    }

    @Test
    void addBookRegistersThenReloadsCatalogToIncludeIt() {
        Book newBook = new Book("the_new_tale", "The New Tale", "A. Author", Difficulty.HARD, List.of());
        when(bookFileAdapter.registerBook("raw-json")).thenReturn(newBook);
        when(bookFileAdapter.loadValidBooks()).thenReturn(List.of(
                new Book("crystal-caverns", "The Crystal Caverns", "Evelyn Stormrider", Difficulty.EASY, List.of()),
                new Book("pirates-jade-sea", "Pirates of the Jade Sea", "Some Author", Difficulty.MEDIUM, List.of()),
                newBook));

        Book result = service.addBook("raw-json");

        assertThat(result).isEqualTo(newBook);
        assertThat(service.list(null, null)).extracting(Book::id)
                .containsExactlyInAnyOrder("crystal-caverns", "pirates-jade-sea", "the_new_tale");
    }
}
