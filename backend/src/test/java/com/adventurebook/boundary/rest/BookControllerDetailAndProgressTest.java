package com.adventurebook.boundary.rest;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.assertj.MockMvcTester.create;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.adventurebook.common.BookNotFoundException;
import com.adventurebook.control.BookCatalogService;
import com.adventurebook.control.GameEngineService;
import com.adventurebook.control.ProgressService;
import com.adventurebook.entity.Book;
import com.adventurebook.entity.Difficulty;
import com.adventurebook.entity.Option;
import com.adventurebook.entity.SavedProgress;
import com.adventurebook.entity.Section;
import com.adventurebook.entity.SectionType;

/**
 * Integration-level tests for {@code GET /api/books/{id}} (full detail) and the
 * {@code /progress} endpoints (US-09/US-10).
 */
@WebMvcTest(BookController.class)
class BookControllerDetailAndProgressTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BookCatalogService bookCatalogService;

    @MockitoBean
    private GameEngineService gameEngineService;

    @MockitoBean
    private ProgressService progressService;

    private MockMvcTester tester() {
        return create(mvc);
    }

    private static Book sampleBook() {
        return new Book("test-book", "Title", "Author", Difficulty.EASY, List.of(
                new Section("1", "begin", SectionType.BEGIN, List.of(new Option("go", "2", null))),
                new Section("2", "end", SectionType.END, List.of())));
    }

    @Test
    void getDetail_existingBook_returnsFullDetail() {
        when(bookCatalogService.findById("test-book")).thenReturn(Optional.of(sampleBook()));

        tester().get().uri("/api/books/test-book")
                .exchange()
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.sections.length()").isEqualTo(2);

        tester().get().uri("/api/books/test-book")
                .exchange()
                .assertThat()
                .bodyJson()
                .extractingPath("$.type").isEqualTo(null);
    }

    @Test
    void getDetail_bookWithGenreType_includesItInResponse() {
        Book bookWithType = new Book("test-book", "Title", "Author", Difficulty.EASY, List.of(
                new Section("1", "begin", SectionType.BEGIN, List.of(new Option("go", "2", null))),
                new Section("2", "end", SectionType.END, List.of())), "Fantasy", "45-60 min", 12,
                List.of("Magic", "Underground"), "A short blurb");
        when(bookCatalogService.findById("test-book")).thenReturn(Optional.of(bookWithType));

        tester().get().uri("/api/books/test-book")
                .exchange()
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.type").isEqualTo("Fantasy");

        tester().get().uri("/api/books/test-book")
                .exchange()
                .assertThat()
                .bodyJson()
                .extractingPath("$.estimatedDuration").isEqualTo("45-60 min");

        tester().get().uri("/api/books/test-book")
                .exchange()
                .assertThat()
                .bodyJson()
                .extractingPath("$.chapterCount").isEqualTo(12);

        tester().get().uri("/api/books/test-book")
                .exchange()
                .assertThat()
                .bodyJson()
                .extractingPath("$.tags").isEqualTo(List.of("Magic", "Underground"));

        tester().get().uri("/api/books/test-book")
                .exchange()
                .assertThat()
                .bodyJson()
                .extractingPath("$.description").isEqualTo("A short blurb");
    }

    @Test
    void getDetail_unknownBook_returns404() {
        when(bookCatalogService.findById("missing")).thenReturn(Optional.empty());

        tester().get().uri("/api/books/missing")
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void getProgress_whenSaveExists_returnsIt() {
        when(progressService.find("test-book"))
                .thenReturn(Optional.of(new SavedProgress("test-book", "1", 7, Instant.now())));

        tester().get().uri("/api/books/test-book/progress")
                .exchange()
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.currentSectionId").isEqualTo("1");
    }

    @Test
    void getProgress_whenNoSave_returns204() {
        when(progressService.find("test-book")).thenReturn(Optional.empty());

        tester().get().uri("/api/books/test-book/progress")
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void putProgress_savesAndReturnsConfirmation() {
        when(progressService.save(eq("test-book"), eq("1"), eq(7)))
                .thenReturn(new SavedProgress("test-book", "1", 7, Instant.now()));

        tester().put().uri("/api/books/test-book/progress")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentSectionId\":\"1\",\"health\":7}")
                .exchange()
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.health").isEqualTo(7);
    }

    @Test
    void putProgress_unknownBook_returns404() {
        when(progressService.save(eq("missing"), anyString(), anyInt()))
                .thenThrow(new BookNotFoundException("no valid book found with id \"missing\""));

        tester().put().uri("/api/books/missing/progress")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentSectionId\":\"1\",\"health\":7}")
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteProgress_clearsSaveAndReturns204() {
        tester().delete().uri("/api/books/test-book/progress")
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.NO_CONTENT);

        verify(progressService).delete("test-book");
    }
}
