package com.adventurebook.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.adventurebook.common.DomainException;
import com.adventurebook.common.ErrorCode;

/**
 * Objective 2/US-05 ("play a game — basic interactions") + Objective 3/US-06
 * ("suffer consequences and manage health"). Covers {@link Book#startGame()} and
 * {@link Book#resolveMove(String, int, int)}.
 */
class BookGameTest {

    private static Section section(String id, SectionType type, Option... options) {
        return new Section(id, "text-" + id, type, options.length == 0 ? List.of() : List.of(options));
    }

    private static Option option(String description, String gotoId) {
        return new Option(description, gotoId, null);
    }

    private static Option option(String description, String gotoId, Consequence consequence) {
        return new Option(description, gotoId, consequence);
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
        assertThat(result.consequenceText()).isNull();
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
    void resolveMove_withNoConsequence_carriesHealthThroughUnchanged() {
        MoveResult result = sampleBook().resolveMove("1", 0, 7);

        assertThat(result.health()).isEqualTo(7);
        assertThat(result.consequenceText()).isNull();
    }

    @Test
    void resolveMove_withLoseHealthConsequence_reducesHealthAndReturnsText() {
        Book book = book(
                section("1", SectionType.BEGIN,
                        option("jump across the gap", "2",
                                new Consequence(ConsequenceType.LOSE_HEALTH, 4, "You scrape your shoulder."))),
                section("2", SectionType.NODE, option("go on", "3")),
                section("3", SectionType.END));

        MoveResult result = book.resolveMove("1", 0, 10);

        assertThat(result.health()).isEqualTo(6);
        assertThat(result.healthChange()).isEqualTo(-4);
        assertThat(result.status()).isEqualTo(GameStatus.PLAYING);
        assertThat(result.consequenceText()).isEqualTo("You scrape your shoulder.");
    }

    @Test
    void resolveMove_withGainHealthConsequence_increasesHealth() {
        Book book = book(
                section("1", SectionType.BEGIN,
                        option("rest a while", "2",
                                new Consequence(ConsequenceType.GAIN_HEALTH, 3, "You feel better."))),
                section("2", SectionType.END));

        MoveResult result = book.resolveMove("1", 0, 5);

        assertThat(result.health()).isEqualTo(8);
        assertThat(result.healthChange()).isEqualTo(3);
    }

    @Test
    void resolveMove_healthChangeIsNominalValue_evenWhenHpIsClamped() {
        Book book = book(
                section("1", SectionType.BEGIN,
                        option("gather your thoughts", "2",
                                new Consequence(ConsequenceType.LOSE_HEALTH, 3, "You're getting crazier..."))),
                section("2", SectionType.NODE, option("go on", "1")));

        assertThat(book.resolveMove("1", 0, 10).healthChange()).isEqualTo(-3);

        MoveResult atOneHp = book.resolveMove("1", 0, 1);
        assertThat(atOneHp.health()).isEqualTo(0);
        assertThat(atOneHp.healthChange()).isEqualTo(-3);
    }

    @Test
    void resolveMove_withoutConsequence_hasZeroHealthChange() {
        Book book = book(
                section("1", SectionType.BEGIN, option("go on", "2")),
                section("2", SectionType.END));

        assertThat(book.resolveMove("1", 0, 10).healthChange()).isZero();
    }

    @Test
    void resolveMove_withLoseHealthConsequence_clampsAtZeroAndReturnsDead() {
        Book book = book(
                section("1", SectionType.BEGIN,
                        option("fall into the chasm", "2",
                                new Consequence(ConsequenceType.LOSE_HEALTH, 20, "You fall to your doom."))),
                section("2", SectionType.NODE, option("go on", "1")));

        MoveResult result = book.resolveMove("1", 0, 10);

        assertThat(result.health()).isEqualTo(0);
        assertThat(result.healthChange()).isEqualTo(-20);
        assertThat(result.status()).isEqualTo(GameStatus.DEAD);
        assertThat(result.consequenceText()).isEqualTo("You fall to your doom.");
    }

    @Test
    void resolveMove_deathTakesPriorityOverSimultaneousEnd() {
        Book book = book(
                section("1", SectionType.BEGIN,
                        option("the fatal ending", "2",
                                new Consequence(ConsequenceType.LOSE_HEALTH, 10, "It was too much."))),
                section("2", SectionType.END));

        MoveResult result = book.resolveMove("1", 0, 10);

        assertThat(result.status()).isEqualTo(GameStatus.DEAD);
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

    @Test
    void resolveMove_failures_carryStableErrorCodes() {
        Book book = sampleBook();

        assertThat(codeOf(() -> book.resolveMove("nope", 0, 10))).isEqualTo(ErrorCode.INVALID_SECTION);
        assertThat(codeOf(() -> book.resolveMove("1", 99, 10))).isEqualTo(ErrorCode.INVALID_OPTION);
        assertThat(codeOf(() -> book.resolveMove("3", 0, 10))).isEqualTo(ErrorCode.GAME_ALREADY_FINISHED);
        assertThat(codeOf(() -> book.resolveMove("1", 0, 0))).isEqualTo(ErrorCode.GAME_ALREADY_FINISHED);
    }

    private static ErrorCode codeOf(Runnable action) {
        try {
            action.run();
        } catch (DomainException ex) {
            return ex.code();
        }
        throw new AssertionError("expected a DomainException");
    }
}
