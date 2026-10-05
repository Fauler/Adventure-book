package com.adventurebook.boundary.file;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.adventurebook.control.BookCatalogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs the book ingestion pipeline once on every backend startup (US-01), per
 * `docs/03-technical-architecture.md`.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookIngestionRunner implements ApplicationRunner {

    private final BookCatalogService bookCatalogService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Running book ingestion pipeline on startup");
        bookCatalogService.ingestAndReload();
    }
}
