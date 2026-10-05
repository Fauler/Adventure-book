# Parking Lot — Future Improvement Ideas

This document collects ideas raised during development/review that are not being acted
on now. Nothing here is a decision or a commitment — it's a holding area to revisit
later, deliberately, so we don't forget good questions but also don't derail the
current scope.

Each entry: what was raised, when, and open questions to resolve before acting on it.

---

## 1 - i18n-readiness of player-facing text

Raised: 05-10-2026

Idea: Should `description` (and other player-facing text: section `text`, option
`description`, `consequence.text`) be language-agnostic / i18n-ready — i.e. designed so
translations could be added later without reshaping the book JSON format or the API
contract?

Open questions to resolve later:
- Do we actually need multi-language support, or just "don't paint ourselves into a
  corner" (avoid baking English-only assumptions into validation/display logic)?
- If i18n is ever wanted, does translation live inside the book JSON (per-section
  translated fields) or as a separate overlay/resource bundle keyed by section id?
- Out of scope for the current objectives — nothing in the brief asks for
  multi-language support.

---

## 2 - Behavior when a request references an invalid/non-existent section

Raised: 05-10-2026

Idea: What should happen if a client calls `/api/books/{id}/play` (or similar) with a
`currentSectionId` that doesn't exist in that book — e.g. stale client state after the
book changed, or a manually/maliciously crafted request? Define the exact backend error
response and frontend handling, rather than leaving it as an implicit edge case.

Open questions to resolve later:
- `400` (malformed move — client sent an inconsistent state) vs. `404` (section not
  found)? Leaning `400` since it's a bad move, not a missing resource, but not decided.
- What specific message/shape does `GlobalExceptionHandler` (NFR-01) produce for this
  case vs. the generic "invalid move" case?
- Should the frontend show a generic error state, or send the player back to the
  library, or offer "restart this book"?
- Note: a book that passed validation (Rules 3+4) guarantees every *referenced* section
  exists, so this should only be reachable via a stale/forged client request, not
  normal play — still needs a defined, non-500 response per NFR-01.

---

## 3 - How should `consequence.text` be surfaced to the player?

Raised: 05-10-2026

Idea: `consequence.text` (the flavor text, e.g. "You cut yourself on a rusty nail") is
naturally available on the book detail (`GET /api/books/{id}` → each option's nested
`consequence.text`), attached to the option *before* it's chosen. The `/play` response
(`section`, `health`, `status` — see `03-technical-architecture.md`'s API contract) does
not currently repeat which consequence just fired or its text, and `/play` is
deliberately stateless (the client always sends back the health it was last given), so
there is no server-side memory of "what just happened" to attach to the response
either. In practice, for the frontend to show "you cut yourself on a rusty nail" right
after a move, it would need to already hold the *previous* section's data (from an
earlier `GET /api/books/{id}`) and manually correlate it with the `optionIndex` it just
sent — the `/play` response alone doesn't explain "what happened."

Open questions to resolve later (design decision, not yet made):
- **Option A** — keep `/play` as-is: frontend always holds the full book detail in
  memory and correlates `optionIndex → consequence` itself. Simplest backend, more
  responsibility pushed to the frontend (tension with "frontend is dumb" principle in
  `03-technical-architecture.md`).
- **Option B** — extend the `/play` response to explicitly include the consequence that
  just applied (e.g. a `triggeredConsequence` field, null if the chosen option had
  none), so the response is self-sufficient and the frontend never needs to
  keep/correlate prior state.
- If Option B: how do we distinguish "no consequence happened" (chosen option had none)
  from "a consequence happened but has empty text" — `null` vs. an object with an empty
  string?
- This directly affects the `/play` API contract in `03-technical-architecture.md` —
  any change needs explicit discussion/approval before editing the contract or code.

---

## 4 - Admin module / book registration UX (one-by-one vs. batch)

Raised: 05-10-2026

Context: US-11 ("Add a new book", `02-user-stories.md`) already documents a
single-book-at-a-time registration flow via the frontend (`POST /api/books`), with
success/error feedback based on the same validation rules as startup ingestion — not
yet implemented, paused as part of a future phase.

Idea raised: should there be a dedicated admin screen for adding books (one at a time,
or in batch)? If batch is ever supported, how do we give per-file feedback on what
failed and why, instead of one combined pass/fail result for the whole batch?

Open questions to resolve later:
- Do we stick to US-11 as-is (one book at a time via the frontend), or extend it to
  support batch upload later?
- If batch: what does the response/UI look like — a per-file list of results
  (valid/invalid + reason each)?
- Does this need its own "admin" area/role, or is it just another screen available to
  any user (per current docs, no auth/roles are in scope yet — see NFR-03/NFR-04 in
  `04-non-functional-requirements.md`)?
- Relation to the startup book-ingestion pipeline's own `valid`/`invalid` folder split
  (`03-technical-architecture.md`) — should the runtime registration flow (US-11) reuse
  exactly the same validation-feedback mechanism, for consistency?

---

## 5 - Mockup shows book metadata that doesn't exist in our JSON/API today

Raised: 05-10-2026

Context: the assessment mockup's book cards show a short description, an estimated
reading time (e.g. "45-60 min"), a chapter count (e.g. "12 chapters"), and theme tags
(e.g. "Magic", "Underground", "Crystals") — none of which exist in the real book JSON
format or API today (only `title`, `author`, `difficulty` are real data, per
`01-challenge-understanding.md`). Matching the mockup's visual layout as-is would
require fixed placeholder text/values, identical for every book, not real per-book data.

Open questions to resolve later:
- Should the book JSON schema be extended with these fields (`description`,
  `estimatedMinutes`, `chapterCount`, `tags`), so each book shows its own real values
  instead of one shared placeholder?
- If yes: are these required fields (a new validation rule) or optional/nullable
  (hidden if absent, similar to how `difficulty` is already handled)?
- `chapterCount` could arguably be *derived* from the book's own section graph (e.g.
  count of reachable sections) instead of hand-authored in the JSON — worth deciding
  which approach before implementing either.
- This changes the public API contract (`GET /api/books` response shape) — needs
  explicit sign-off before implementing, same as any other contract change in this list.

---

## 6 - Show the player's current HP more consistently across the gameplay screen

Raised: 05-10-2026

Idea: today HP (e.g. "❤️ X HP") is shown in the header while actively playing
(`status === PLAYING`), but isn't shown at all on the `WON`/`DEAD` outcome screens
except inline in the `WON` message text ("You made it out alive, with X HP
remaining") — the `DEAD` screen doesn't restate final HP at all (it's implicitly 0).

Open questions to resolve later:
- Should the `DEAD` outcome screen also show the HP badge (even though it's always 0),
  for visual consistency with `PLAYING`/`WON`?
- Should HP be shown anywhere outside the game screen itself (e.g. on a library card
  for a book that has saved progress)?
- Is this purely cosmetic, or does it reveal a real UX gap (player unsure of their HP
  right before a risky choice)? Nothing reported as confusing so far — purely an idea
  to evaluate later, not a known bug.
