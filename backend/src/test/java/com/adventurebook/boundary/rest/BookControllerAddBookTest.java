package com.adventurebook.boundary.rest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.assertj.MockMvcTester.create;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.adventurebook.common.BookValidationException;
import com.adventurebook.control.BookCatalogService;
import com.adventurebook.control.GameEngineService;
import com.adventurebook.control.ProgressService;
import com.adventurebook.entity.Book;
import com.adventurebook.entity.Difficulty;

/**
 * Tests for {@code POST /api/books} (US-11 — add a new book at runtime).
 */
@WebMvcTest(BookController.class)
class BookControllerAddBookTest {

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

    @Test
    void addBook_valid_returns201WithSummary() {
        Book registered = new Book("a_tiny_tale", "A Tiny Tale", "Tester", Difficulty.EASY, List.of());
        when(bookCatalogService.addBook(anyString())).thenReturn(registered);

        tester().post().uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"A Tiny Tale\"}")
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.id").isEqualTo("a_tiny_tale");
    }

    @Test
    void addBook_invalid_returns400WithEveryFailedRule() {
        when(bookCatalogService.addBook(anyString()))
                .thenThrow(new BookValidationException(List.of("no BEGIN section found", "no END section found")));

        tester().post().uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Broken\"}")
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.messages.length()").isEqualTo(2);
    }
}
