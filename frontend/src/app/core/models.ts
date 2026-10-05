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
