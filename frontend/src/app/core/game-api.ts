import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { PlayRequest, PlayResponse } from './models';

/**
 * Thin wrapper around `POST /api/books/{id}/play`, mirroring the backend API contract
 * 1:1 (see docs/03-technical-architecture.md). Stateless: every call sends the full
 * current state (`currentSectionId`/`optionIndex`/`health`) back to the backend, which
 * re-derives everything — this service never caches/trusts client-side game state.
 */
@Injectable({ providedIn: 'root' })
export class GameApi {
  private readonly http = inject(HttpClient);

  play(bookId: string, request: PlayRequest): Observable<PlayResponse> {
    return this.http.post<PlayResponse>(`/api/books/${bookId}/play`, request);
  }
}
