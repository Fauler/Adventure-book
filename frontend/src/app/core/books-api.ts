import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { BookDetail, BookSummary, Difficulty, SaveProgressRequest, SavedProgress } from './models';

/**
 * Thin wrapper around the book catalog + save/resume endpoints, mirroring the backend
 * API contract 1:1 (see docs/03-technical-architecture.md). No client-side filtering/
 * transformation — `search`/`difficulty` are forwarded as-is for the backend to apply.
 */
@Injectable({ providedIn: 'root' })
export class BooksApi {
  private readonly http = inject(HttpClient);

  list(search?: string, difficulty?: Difficulty | null): Observable<BookSummary[]> {
    let params = new HttpParams();
    if (search) {
      params = params.set('search', search);
    }
    if (difficulty) {
      params = params.set('difficulty', difficulty);
    }
    return this.http.get<BookSummary[]>('/api/books', { params });
  }

  /** `GET /api/books/{id}` — full detail (all sections), used by the game screen. */
  getDetail(bookId: string): Observable<BookDetail> {
    return this.http.get<BookDetail>(`/api/books/${bookId}`);
  }

  /**
   * `GET /api/books/{id}/progress` — the single saved game for this book, or `null`
   * when nothing is saved (backend returns `204 No Content`, which `HttpClient` surfaces
   * as a `null` body).
   */
  getProgress(bookId: string): Observable<SavedProgress | null> {
    return this.http
      .get<SavedProgress | null>(`/api/books/${bookId}/progress`, { observe: 'response' })
      .pipe(map((response) => response.body));
  }

  /** `PUT /api/books/{id}/progress` — saves/overwrites the single save slot (US-09). */
  saveProgress(bookId: string, request: SaveProgressRequest): Observable<SavedProgress> {
    return this.http.put<SavedProgress>(`/api/books/${bookId}/progress`, request);
  }

  /** `DELETE /api/books/{id}/progress` — clears the save slot (Restart/"discard"). */
  deleteProgress(bookId: string): Observable<void> {
    return this.http.delete<void>(`/api/books/${bookId}/progress`);
  }

  /**
   * `POST /api/books` (US-11) — registers a new book at runtime from raw JSON text.
   * Re-runs the exact same validation as startup ingestion; the backend returns `201`
   * + the new book's summary on success, or `400` + every failed rule (see
   * {@link ApiError}) on rejection. The frontend only pre-checks JSON *syntax* before
   * calling this — all business-rule validation happens backend-side.
   */
  addBook(rawJson: string): Observable<BookSummary> {
    return this.http.post<BookSummary>('/api/books', rawJson, {
      headers: { 'Content-Type': 'application/json' },
    });
  }
}
