/**
 * Shared TypeScript types mirroring the backend API contract 1:1
 * (see docs/03-technical-architecture.md). Populated incrementally as each
 * milestone's endpoints land — kept here so `core/` API services and components
 * share one source of truth for shapes, never duplicating backend business logic.
 */

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';

/**
 * Response shape of `GET /api/books` list items. `type`/`estimatedDuration`/
 * `chapterCount`/`tags`/`description` are optional, author-provided display metadata —
 * real fields in the book JSON (see `docs/00-PARKING_LOT.md` #5), `null`/empty when
 * the book's own JSON leaves them blank/absent (true for every bundled sample book
 * today).
 */
export interface BookSummary {
  id: string;
  title: string;
  author: string;
  difficulty: Difficulty;
  type: string | null;
  estimatedDuration: string | null;
  chapterCount: number | null;
  tags: string[];
  description: string | null;
}

/** Mirrors the backend's `GameStatus` enum (Objective 2/US-05). */
export type GameStatus = 'PLAYING' | 'WON' | 'DEAD';

/** A single selectable choice, as rendered — no `gotoId`/`consequence` exposed. */
export interface Option {
  description: string;
}

/** The section currently shown to the player. `options` is empty for an ending. */
export interface Section {
  id: string;
  text: string;
  options: Option[];
}

/**
 * Request body for `POST /api/books/{id}/play`. Omit `currentSectionId` (and
 * therefore `optionIndex`/`health`) to start a fresh game at BEGIN.
 */
export interface PlayRequest {
  currentSectionId?: string;
  optionIndex?: number;
  health?: number;
}

/**
 * Full detail of a book (`GET /api/books/{id}`) — includes every section. Used both to
 * render the "begin quest" step and, per `docs/05-business-architecture.md`'s "Save /
 * Resume behavior", to look up a saved section's content locally when the player
 * chooses **Continue** (resuming never calls `/play` for a move that was already made).
 */
export interface BookDetail {
  id: string;
  title: string;
  author: string;
  difficulty: Difficulty;
  sections: Section[];
  type: string | null;
  estimatedDuration: string | null;
  chapterCount: number | null;
  tags: string[];
  description: string | null;
}

/**
 * Response body for `GET`/`PUT /api/books/{id}/progress` (US-09/US-10). A `GET` with no
 * save returns `204 No Content` (modeled as `null` by `BooksApi.getProgress`).
 */
export interface SavedProgress {
  currentSectionId: string;
  health: number;
  updatedAt: string;
}

/** Request body for `PUT /api/books/{id}/progress` (US-09). */
export interface SaveProgressRequest {
  currentSectionId: string;
  health: number;
}

/** Response body for `POST /api/books/{id}/play`. `consequenceText` is the just-applied
 * option's consequence flavor text (US-06), `null`/absent when the option had none. */
export interface PlayResponse {
  section: Section;
  health: number;
  status: GameStatus;
  consequenceText: string | null;
}

/**
 * Uniform error body returned by the backend for any rejected request (see
 * `ApiError` in the backend). Used by the add-a-new-book screen (US-11) to display
 * every failed validation rule verbatim, one per line.
 */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  messages: string[];
}
