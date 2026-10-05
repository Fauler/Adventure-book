package com.adventurebook.boundary.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.adventurebook.common.BookValidationException;
import com.adventurebook.entity.Book;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

/**
 * The only class allowed to touch the book flat-file pipeline
 * (`data/books/{incoming,valid,invalid}`, per `docs/03-technical-architecture.md`).
 * Parses book JSON into the {@link Book} entity, moves files between the 3 folders
 * based on {@link Book#validate()}, and writes `<name>.errors.txt` next to rejected
 * files. Never re-implements or relaxes the validity rules itself.
 */
@Slf4j
@Component
public class BookFileAdapter {

    private final Path incomingDir;
    private final Path validDir;
    private final Path invalidDir;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public BookFileAdapter(
            @Value("${adventurebook.books.incoming-dir}") String incomingDir,
            @Value("${adventurebook.books.valid-dir}") String validDir,
            @Value("${adventurebook.books.invalid-dir}") String invalidDir) {
        this.incomingDir = Path.of(incomingDir);
        this.validDir = Path.of(validDir);
        this.invalidDir = Path.of(invalidDir);
    }

    /**
     * Processes every {@code *.json} file currently in {@code incoming/}: parses it,
     * validates it, and moves it into {@code valid/} or {@code invalid/} (+ a sibling
     * {@code .errors.txt} listing every failed rule). Files are removed from
     * {@code incoming/} once processed — idempotent in the sense that files already
     * sitting in {@code valid/}/{@code invalid/} are left untouched unless a new file
     * with the same name lands in {@code incoming/} again.
     */
    public void ingestIncoming() {
        ensureDirectoriesExist();
        List<Path> files = listJsonFiles(incomingDir);
        for (Path file : files) {
            processIncomingFile(file);
        }
        log.info("Book ingestion pipeline processed {} file(s) from {}", files.size(), incomingDir);
    }

    /**
     * Rebuilds the in-memory catalog by re-reading every file currently in
     * {@code valid/} — the single source of truth for what the API serves.
     */
    public List<Book> loadValidBooks() {
        ensureDirectoriesExist();
        List<Book> books = new ArrayList<>();
        for (Path file : listJsonFiles(validDir)) {
            try {
                books.add(parse(file));
            } catch (RuntimeException e) {
                // Defensive only: files in valid/ were already validated when they were
                // written there, so this should not normally happen.
                log.error("Previously-valid book file {} could no longer be parsed; excluding from catalog",
                        file.getFileName(), e);
            }
        }
        return books;
    }

    /**
     * Registers a book submitted at runtime via {@code POST /api/books} (US-11):
     * parses and validates it with the exact same rules as the startup ingestion
     * pipeline, then writes it straight to {@code valid/} or {@code invalid/} (+
     * {@code .errors.txt}) — no round-trip through {@code incoming/} is needed since
     * the content already arrived in-memory, not as a dropped file. The filename is a
     * slug of the book's title (de-duplicated with a numeric suffix on collision), per
     * `docs/05-business-architecture.md`'s "Book file naming" section.
     *
     * <p>Resubmitting byte-identical content (e.g. clicking "Submit book" more than
     * once without editing anything) is idempotent — it reuses the already-written
     * {@code slug[_N].json} file instead of minting a new numbered variant each time
     * (see {@link #findIdenticalSubmission}). Two genuinely different books that
     * happen to share a title still each get their own numbered file, as designed.
     *
     * @return the validated {@link Book}, with its final slug {@code id}
     * @throws BookValidationException carrying every failed rule if rejected
     */
    public Book registerBook(String rawJson) {
        ensureDirectoriesExist();

        Book parsed;
        try {
            parsed = parseContent("pending", rawJson);
        } catch (RuntimeException e) {
            throw new BookValidationException(List.of("book payload is empty or not valid JSON: " + e.getMessage()));
        }

        List<String> errors = parsed.validate();
        String slug = slugify(parsed.title());
        String filename = findIdenticalSubmission(slug, rawJson).orElseGet(() -> uniqueFilename(slug));

        if (!errors.isEmpty()) {
            writeInvalid(filename, rawJson, errors);
            log.warn("Book submission rejected: filename={}, reasons={}", filename, errors);
            throw new BookValidationException(errors);
        }

        writeValid(filename, rawJson);
        String finalId = stem(filename);
        log.info("Book registered via API: id={}", finalId);
        return new Book(finalId, parsed.title(), parsed.author(), parsed.difficulty(), parsed.sections());
    }

    private void processIncomingFile(Path file) {
        String filename = file.getFileName().toString();
        String bookId = stem(filename);
        String content;
        try {
            content = Files.readString(file);
        } catch (IOException e) {
            log.warn("Could not read incoming book file {}: {}", filename, e.getMessage());
            writeInvalid(filename, "", List.of("book file could not be read: " + e.getMessage()));
            deleteQuietly(file);
            return;
        }

        List<String> errors = new ArrayList<>();
        Book book = null;
        try {
            book = parseContent(bookId, content);
        } catch (RuntimeException e) {
            errors.add("book file is empty or not valid JSON: " + e.getMessage());
        }

        if (book != null) {
            errors.addAll(book.validate());
        }

        if (errors.isEmpty()) {
            writeValid(filename, content);
            log.info("Book ingested as valid: id={}", bookId);
        } else {
            writeInvalid(filename, content, errors);
            log.warn("Book ingested as invalid: id={}, reasons={}", bookId, errors);
        }
        deleteQuietly(file);
    }

    private Book parse(Path file) {
        String bookId = stem(file.getFileName().toString());
        String content;
        try {
            content = Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return parseContent(bookId, content);
    }

    private Book parseContent(String bookId, String content) {
        Book parsed = jsonMapper.readValue(content, Book.class);
        return new Book(bookId, parsed.title(), parsed.author(), parsed.difficulty(), parsed.sections());
    }

    private void writeValid(String filename, String content) {
        try {
            Files.writeString(validDir.resolve(filename), content);
            Files.deleteIfExists(invalidDir.resolve(filename));
            Files.deleteIfExists(invalidDir.resolve(errorsFilename(filename)));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write valid book file " + filename, e);
        }
    }

    private void writeInvalid(String filename, String content, List<String> errors) {
        try {
            Files.writeString(invalidDir.resolve(filename), content);
            String errorsText = errors.stream().map(error -> "FAILED: " + error).reduce("", (a, b) -> a + b + "\n");
            Files.writeString(invalidDir.resolve(errorsFilename(filename)), errorsText);
            Files.deleteIfExists(validDir.resolve(filename));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write invalid book file " + filename, e);
        }
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not remove processed file {} from incoming/: {}", file.getFileName(), e.getMessage());
        }
    }

    private List<Path> listJsonFiles(Path dir) {
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            log.error("Failed to read books directory {}", dir, e);
            return List.of();
        }
    }

    private void ensureDirectoriesExist() {
        for (Path dir : List.of(incomingDir, validDir, invalidDir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not create books directory " + dir, e);
            }
        }
    }

    private static String stem(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private static String errorsFilename(String filename) {
        return filename + ".errors.txt";
    }

    /**
     * Slugifies a book title into a filename stem — lowercased, non-alphanumeric runs
     * collapsed to a single {@code _}, leading/trailing {@code _} trimmed (e.g.
     * {@code "The Crystal Caverns"} -&gt; {@code "the_crystal_caverns"}). Falls back to
     * {@code "book"} if the title has no alphanumeric characters at all.
     */
    private static String slugify(String title) {
        String source = title == null ? "" : title;
        String slug = source.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return slug.isEmpty() ? "book" : slug;
    }

    /**
     * Appends an incrementing numeric suffix ({@code _2}, {@code _3}, ...) until a
     * filename that doesn't already exist in either {@code valid/} or {@code invalid/}
     * is found, per `docs/05-business-architecture.md`'s collision rule.
     */
    private String uniqueFilename(String slug) {
        String candidate = slug + ".json";
        int suffix = 2;
        while (Files.exists(validDir.resolve(candidate)) || Files.exists(invalidDir.resolve(candidate))) {
            candidate = slug + "_" + suffix + ".json";
            suffix++;
        }
        return candidate;
    }

    /**
     * Resubmitting the exact same JSON (e.g. a user clicking "Submit book" more than
     * once without changing anything) must not pile up a new {@code _2}/{@code _3}/...
     * file every time — this looks for an existing {@code slug[_N].json} file (in
     * either {@code valid/} or {@code invalid/}) whose content is byte-identical to
     * what's being submitted now, and reuses its filename so the resubmission is a
     * no-op rather than a new entry.
     */
    private Optional<String> findIdenticalSubmission(String slug, String rawJson) {
        Pattern sameSlug = Pattern.compile(Pattern.quote(slug) + "(_\\d+)?\\.json");
        for (Path dir : List.of(validDir, invalidDir)) {
            for (Path candidate : listJsonFiles(dir)) {
                String name = candidate.getFileName().toString();
                if (!sameSlug.matcher(name).matches()) {
                    continue;
                }
                try {
                    if (Files.readString(candidate).equals(rawJson)) {
                        return Optional.of(name);
                    }
                } catch (IOException e) {
                    log.warn("Could not read {} while checking for a duplicate submission: {}",
                            name, e.getMessage());
                }
            }
        }
        return Optional.empty();
    }
}
