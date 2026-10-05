package com.adventurebook.boundary.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

import com.adventurebook.common.BookNotFoundException;
import com.adventurebook.common.DomainException;
import com.adventurebook.control.BookCatalogService;
import com.adventurebook.control.GameEngineService;
import com.adventurebook.control.ProgressService;
import com.adventurebook.entity.GameStatus;
import com.adventurebook.entity.MoveResult;
import com.adventurebook.entity.Option;
import com.adventurebook.entity.Section;
import com.adventurebook.entity.SectionType;

/**
 * Integration-level test for {@code POST /api/books/{id}/play}, per Objective 2/US-05.
 */
@WebMvcTest(BookController.class)
class BookControllerPlayTest {

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

    private static Section beginSection() {
        return new Section("1", "You wake up in a forest.", SectionType.BEGIN,
                List.of(new Option("Go north", "2", null)));
    }

    @Test
    void startsFreshGame_whenNoCurrentSectionIdGiven() {
        when(gameEngineService.play(eq("test-book"), isNull(), isNull(), isNull()))
                .thenReturn(new MoveResult(beginSection(), 10, GameStatus.PLAYING, null));

        tester().post().uri("/api/books/test-book/play")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .exchange()
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.health").isEqualTo(10);
    }

    @Test
    void returns404_whenBookDoesNotExist() {
        when(gameEngineService.play(eq("missing"), any(), any(), any()))
                .thenThrow(new BookNotFoundException("no valid book found with id \"missing\""));

        tester().post().uri("/api/books/missing/play")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void returns400_whenMoveIsIllegal() {
        when(gameEngineService.play(eq("test-book"), eq("1"), eq(99), eq(10)))
                .thenThrow(new DomainException("option index 99 does not exist on section \"1\""));

        tester().post().uri("/api/books/test-book/play")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentSectionId\":\"1\",\"optionIndex\":99,\"health\":10}")
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.BAD_REQUEST);
    }
}
