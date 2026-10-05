import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { BookSummary, Difficulty } from './models';

/**
 * Thin wrapper around `GET /api/books`, mirroring the backend API contract 1:1
 * (see docs/03-technical-architecture.md). No client-side filtering/transformation —
 * `search`/`difficulty` are forwarded as-is for the backend to apply.
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
}
