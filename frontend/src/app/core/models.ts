/**
 * Shared TypeScript types mirroring the backend API contract 1:1
 * (see docs/03-technical-architecture.md). Populated incrementally as each
 * milestone's endpoints land — kept here so `core/` API services and components
 * share one source of truth for shapes, never duplicating backend business logic.
 */

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';

/**
 * Response shape of `GET /api/books` list items. Deliberately omits `description`/
 * `tags` shown in the original mockup — the real book JSON only has `title`/`author`/
 * `difficulty` (see `docs/00-PARKING_LOT.md` #5), so the backend doesn't fabricate them.
 */
export interface BookSummary {
  id: string;
  title: string;
  author: string;
  difficulty: Difficulty;
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

/** Response body for `POST /api/books/{id}/play`. */
export interface PlayResponse {
  section: Section;
  health: number;
  status: GameStatus;
}
