package com.adventurebook.boundary.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.adventurebook.entity.Book;

/**
 * Exercises the ingestion pipeline against the 4 real sample books (copied into
 * {@code src/test/resources/sample-books}) — the same fixtures named in
 * `docs/06-implementation-plan.md`'s M1 plan.
 *
 * <p>A full, literal read of these fixtures (not just the summary in
 * {@code docs/01-challenge-understanding.md}) shows all 4 are actually invalid: in
 * addition to the documented the-prisoner/dragon-quest issues, both crystal-caverns and
 * pirates-jade-sea contain a {@code NODE} section with id 666 that has no options — the
 * same "orphan node" rule-4 violation as the-prisoner — and pirates-jade-sea also has a
 * dangling {@code gotoId} (999, no such section exists). The validator must enforce the
 * 4 documented rules exactly as written, regardless of how the fixtures were originally
 * assessed, so these assertions reflect the actual (corrected) outcome.
 */
class BookFileAdapterTest {

    @TempDir
    Path tempDir;

    private Path incomingDir;
    private Path validDir;
    private Path invalidDir;
    private BookFileAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        incomingDir = tempDir.resolve("incoming");
        validDir = tempDir.resolve("valid");
        invalidDir = tempDir.resolve("invalid");
        Files.createDirectories(incomingDir);

        for (String filename : List.of(
                "crystal-caverns.json", "pirates-jade-sea.json", "the-prisoner.json", "dragon-quest.json")) {
            copySampleBook(filename, incomingDir.resolve(filename));
        }

        adapter = new BookFileAdapter(incomingDir.toString(), validDir.toString(), invalidDir.toString());
    }

    private static void copySampleBook(String filename, Path target) throws IOException {
        try (InputStream in = Objects.requireNonNull(
                BookFileAdapterTest.class.getResourceAsStream("/sample-books/" + filename))) {
            Files.copy(in, target);
        }
    }

    @Test
    void crystalCavernsIsInvalid_orphanNodeWithNoOptions() throws IOException {
        adapter.ingestIncoming();

        Path rejected = invalidDir.resolve("crystal-caverns.json");
        Path errorsFile = invalidDir.resolve("crystal-caverns.json.errors.txt");
        assertThat(rejected).exists();
        assertThat(errorsFile).exists();

        String errors = Files.readString(errorsFile);
        assertThat(errors).contains("FAILED:").contains("666").contains("no options");
    }

    @Test
    void piratesJadeSeaIsInvalid_orphanNodeAndDanglingGotoId() throws IOException {
        adapter.ingestIncoming();

        Path rejected = invalidDir.resolve("pirates-jade-sea.json");
        Path errorsFile = invalidDir.resolve("pirates-jade-sea.json.errors.txt");
        assertThat(rejected).exists();
        assertThat(errorsFile).exists();

        String errors = Files.readString(errorsFile);
        assertThat(errors).contains("FAILED:").contains("666").contains("no options").contains("999");
    }

    @Test
    void thePrisonerIsInvalid_orphanNodeWithNoOptions() throws IOException {
        adapter.ingestIncoming();

        Path rejected = invalidDir.resolve("the-prisoner.json");
        Path errorsFile = invalidDir.resolve("the-prisoner.json.errors.txt");
        assertThat(rejected).exists();
        assertThat(errorsFile).exists();

        String errors = Files.readString(errorsFile);
        assertThat(errors).contains("FAILED:").contains("666").contains("no options");
    }

    @Test
    void dragonQuestIsInvalid_emptyCorruptFile() throws IOException {
        adapter.ingestIncoming();

        Path rejected = invalidDir.resolve("dragon-quest.json");
        Path errorsFile = invalidDir.resolve("dragon-quest.json.errors.txt");
        assertThat(rejected).exists();
        assertThat(errorsFile).exists();
        assertThat(Files.readString(errorsFile)).contains("FAILED:");
    }

    @Test
    void incomingFilesAreRemovedAfterProcessing() {
        adapter.ingestIncoming();

        assertThat(incomingDir).isEmptyDirectory();
    }

    @Test
    void loadValidBooksRebuildsCatalogFromValidDirOnly() {
        adapter.ingestIncoming();

        List<Book> books = adapter.loadValidBooks();

        // All 4 bundled sample fixtures are, in fact, invalid once every section is
        // checked (see class-level note) — the catalog built from valid/ is empty.
        assertThat(books).isEmpty();
    }
}
