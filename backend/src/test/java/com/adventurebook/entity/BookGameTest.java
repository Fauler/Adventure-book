package com.adventurebook.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.adventurebook.common.DomainException;

/**
 * Objective 2/US-05: "play a game — basic interactions" (no consequence effects yet,
 * see US-06/M3). Covers {@link Book#startGame()} and
 * {@link Book#resolveMove(String, int, int)}.
 */
class BookGameTest {

    private static Section section(String id, SectionType type, Option... options) {
        return new Section(id, "text-" + id, type, options.length == 0 ? List.of() : List.of(options));
    }

    private static Option option(String description, String gotoId) {
        return new Option(description, gotoId, null);
    }

    private static Book book(Section... sections) {
        return new Book("test-book", "Title", "Author", Difficulty.EASY, List.of(sections));
    }

    private static Book sampleBook() {
        return book(
                section("1", SectionType.BEGIN, option("go to node", "2")),
                section("2", SectionType.NODE,
                        option("win the game", "3"),
                        option("die", "4")),
                section("3", SectionType.END),
                section("4", SectionType.END));
    }

    @Test
    void startGame_returnsBeginSectionWithStartingHealthAndPlaying() {
        MoveResult result = sampleBook().startGame();

        assertThat(result.section().id()).isEqualTo("1");
        assertThat(result.health()).isEqualTo(Book.STARTING_HEALTH);
        assertThat(result.status()).isEqualTo(GameStatus.PLAYING);
    }

    @Test
    void resolveMove_toNodeSection_staysPlaying() {
        MoveResult result = sampleBook().resolveMove("1", 0, Book.STARTING_HEALTH);

        assertThat(result.section().id()).isEqualTo("2");
        assertThat(result.health()).isEqualTo(Book.STARTING_HEALTH);
        assertThat(result.status()).isEqualTo(GameStatus.PLAYING);
    }

    @Test
    void resolveMove_toEndSection_returnsWon() {
        MoveResult result = sampleBook().resolveMove("2", 0, Book.STARTING_HEALTH);

        assertThat(result.section().id()).isEqualTo("3");
        assertThat(result.status()).isEqualTo(GameStatus.WON);
    }

    @Test
    void resolveMove_carriesHealthThroughUnchanged_noConsequencesYet() {
        MoveResult result = sampleBook().resolveMove("1", 0, 7);

        assertThat(result.health()).isEqualTo(7);
    }

    @Test
    void resolveMove_unknownCurrentSectionId_throwsDomainException() {
        assertThatThrownBy(() -> sampleBook().resolveMove("does-not-exist", 0, Book.STARTING_HEALTH))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("does-not-exist");
    }

    @Test
    void resolveMove_optionIndexOutOfBounds_throwsDomainException() {
        assertThatThrownBy(() -> sampleBook().resolveMove("1", 5, Book.STARTING_HEALTH))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("option index 5");
    }

    @Test
    void resolveMove_negativeOptionIndex_throwsDomainException() {
        assertThatThrownBy(() -> sampleBook().resolveMove("1", -1, Book.STARTING_HEALTH))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void resolveMove_onAlreadyEndedSection_throwsDomainException() {
        assertThatThrownBy(() -> sampleBook().resolveMove("3", 0, Book.STARTING_HEALTH))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("already ended");
    }

    @Test
    void resolveMove_withZeroHealth_throwsDomainException() {
        assertThatThrownBy(() -> sampleBook().resolveMove("1", 0, 0))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("already ended");
    }

    @Test
    void resolveMove_optionPointingToMissingSection_throwsDomainException() {
        Book broken = book(
                section("1", SectionType.BEGIN, option("go nowhere", "ghost")));

        assertThatThrownBy(() -> broken.resolveMove("1", 0, Book.STARTING_HEALTH))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void startGame_bookWithNoBeginSection_throwsDomainException() {
        Book broken = book(section("1", SectionType.END));

        assertThatThrownBy(broken::startGame).isInstanceOf(DomainException.class);
    }
}
