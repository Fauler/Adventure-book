package com.adventurebook.boundary.rest;

import static org.mockito.ArgumentMatchers.any;
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
import com.adventurebook.common.BookValidationException;
import com.adventurebook.common.DomainException;
import com.adventurebook.common.ErrorCode;
import com.adventurebook.control.BookCatalogService;
import com.adventurebook.control.GameEngineService;
import com.adventurebook.control.ProgressService;

/**
 * Contract test for the uniform error body: every failure must come back with the right
 * HTTP status and a stable {@code code}, and never leak internals (NFR-01, M6.2).
 */
@WebMvcTest(BookController.class)
class ApiErrorContractTest {

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

    private void assertError(MockMvcTester.MockMvcRequestBuilder request, HttpStatus status, ErrorCode code) {
        var result = request.exchange();
        result.assertThat().hasStatus(status).bodyJson()
                .extractingPath("$.code").isEqualTo(code.name());
        result.assertThat().bodyJson().extractingPath("$.status").isEqualTo(status.value());
    }

    @Test
    void unknownBook_is404BookNotFound() {
        when(bookCatalogService.findById("x")).thenReturn(java.util.Optional.empty());
        assertError(tester().get().uri("/api/books/x"), HttpStatus.NOT_FOUND, ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    void progressOfRemovedBook_is404BookNotFound() {
        when(progressService.find("x")).thenThrow(new BookNotFoundException("gone"));
        assertError(tester().get().uri("/api/books/x/progress"), HttpStatus.NOT_FOUND, ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    void invalidSection_is400WithItsOwnCode() {
        when(gameEngineService.play(any(), any(), any(), any()))
                .thenThrow(new DomainException(ErrorCode.INVALID_SECTION, "no such section"));
        assertError(play("{\"currentSectionId\":\"9\",\"optionIndex\":0,\"health\":5}"),
                HttpStatus.BAD_REQUEST, ErrorCode.INVALID_SECTION);
    }

    @Test
    void invalidOption_is400WithItsOwnCode() {
        when(gameEngineService.play(any(), any(), any(), any()))
                .thenThrow(new DomainException(ErrorCode.INVALID_OPTION, "no such option"));
        assertError(play("{\"currentSectionId\":\"1\",\"optionIndex\":9,\"health\":5}"),
                HttpStatus.BAD_REQUEST, ErrorCode.INVALID_OPTION);
    }

    @Test
    void finishedGame_is400WithItsOwnCode() {
        when(gameEngineService.play(any(), any(), any(), any()))
                .thenThrow(new DomainException(ErrorCode.GAME_ALREADY_FINISHED, "over"));
        assertError(play("{\"currentSectionId\":\"3\",\"optionIndex\":0,\"health\":5}"),
                HttpStatus.BAD_REQUEST, ErrorCode.GAME_ALREADY_FINISHED);
    }

    @Test
    void invalidBookUpload_is400BookValidationFailed() {
        when(bookCatalogService.addBook(any())).thenThrow(new BookValidationException(List.of("no BEGIN section")));
        assertError(tester().post().uri("/api/books").contentType(MediaType.APPLICATION_JSON).content("{}"),
                HttpStatus.BAD_REQUEST, ErrorCode.BOOK_VALIDATION_FAILED);
    }

    @Test
    void malformedBody_is400MalformedRequest() {
        assertError(play("{not json"), HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST);
    }

    @Test
    void invalidQueryParameter_is400InvalidParameter() {
        assertError(tester().get().uri("/api/books?difficulty=IMPOSSIBLE"),
                HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void unsupportedMethod_is405() {
        assertError(tester().patch().uri("/api/books/x"),
                HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED);
    }

    @Test
    void unsupportedMediaType_is415() {
        assertError(tester().post().uri("/api/books/x/play").contentType(MediaType.TEXT_PLAIN).content("hi"),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void unknownRoute_is404RouteNotFound() {
        assertError(tester().get().uri("/api/nope"), HttpStatus.NOT_FOUND, ErrorCode.ROUTE_NOT_FOUND);
    }

    @Test
    void unexpectedFailure_is500WithoutLeakingDetails() throws Exception {
        when(bookCatalogService.list(any(), any())).thenThrow(new IllegalStateException("secret internal detail"));
        var result = tester().get().uri("/api/books").exchange();
        result.assertThat().hasStatus(HttpStatus.INTERNAL_SERVER_ERROR).bodyJson()
                .extractingPath("$.code").isEqualTo(ErrorCode.INTERNAL_ERROR.name());
        org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                .doesNotContain("secret internal detail")
                .doesNotContain("IllegalStateException");
    }

    private MockMvcTester.MockMvcRequestBuilder play(String body) {
        return tester().post().uri("/api/books/test-book/play")
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
