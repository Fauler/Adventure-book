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

## 3 - How should `consequence.text` be surfaced to the player? ✅ Resolved (M3)

Raised: 05-10-2026 — Resolved: 05-10-2026 (M3 implementation)

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

**Decision**: Option B. `PlayResponse` now includes a `consequenceText` field (`String`,
nullable) — `null` when the chosen option carried no `Consequence`, otherwise the
consequence's flavor text. This keeps the frontend "dumb" (no need to hold/correlate
prior book-detail state) and satisfies US-06's "I'm shown the consequence text" AC.
`docs/03-technical-architecture.md`'s API contract table has been updated to match.
There is no separate "consequence happened but has empty text" case in practice — a
`Consequence` is only ever attached to an `Option` with non-blank flavor text in the
sample data; if that ever changes, an empty string (not `null`) would still correctly
mean "a consequence fired, with no text," so the `null`-vs-"happened" distinction holds.

~~Open questions to resolve later (design decision, not yet made):~~
- ~~**Option A** — keep `/play` as-is...~~
- ~~**Option B** — extend the `/play` response...~~
- ~~If Option B: how do we distinguish...~~
- ~~This directly affects the `/play` API contract...~~

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

**Status update (05-10-2026, UI polish pass)**: re-raised when comparing the implemented
Home/Library screen against the mockup side-by-side. Asked the user how to proceed
(author real per-book `description`/`estimatedMinutes`/`chapterCount`/`tags`, derive
`chapterCount` only, or skip) — the user was unavailable to answer synchronously.
**Decision**: left as a documented gap, no change made. Cards continue to show only
real data (`title`/`author`/`difficulty`); no placeholder/fabricated text was added, to
avoid presenting invented content as if it were real per-book data. Still awaiting
explicit sign-off before touching the `GET /api/books` contract.

**Status update (06-10-2026)**: discovered the provided book JSON format actually *does*
already carry one more real field beyond `title`/`author`/`difficulty` — a top-level
`type` (free-text genre/category, e.g. intended for something like "Fantasy"/
"Adventure") — it was being silently dropped by `@JsonIgnoreProperties(ignoreUnknown =
true)` on `Book`. Every sample file leaves it blank (`""`) or omits it entirely, so
nothing has ever displayed, but since this is real per-book data (not fabricated), it's
now mapped end-to-end: `Book.type` (normalized blank/absent → `null`), exposed on both
`BookSummaryResponse`/`BookDetailResponse` as `type: string | null`, and rendered on the
frontend's book card as a genre pill next to the difficulty badge **only when present**
— hidden entirely for every book shipped today, since they all have a blank/absent
`type`.

**Status update (06-10-2026, later same day)**: the remaining four fields
(`estimatedDuration`, `chapterCount`, `tags`, `description`) were also added, following
the exact same pattern as `type` — optional, author-provided display metadata on
`Book` (normalized blank/absent → `null`, `tags` defaults to an empty list with
blank/null entries filtered out), exposed on `BookSummaryResponse`/`BookDetailResponse`,
and rendered on the frontend book card **only when present** (duration/chapter-count
row, tag pills, description paragraph). `chapterCount` was kept author-provided (not
derived from the section graph), matching `type`'s simplicity — deriving it remains a
possible future improvement if it ever drifts from the real section count, but wasn't
pursued here. None of these fields participate in validation or game rules. A fully
populated sample file demonstrating all 5 fields together was added at
`assessment-material/New-books/lost-kingdom-eldoria_v2.json`. The `GET /api/books`
`tags` query parameter still has no filtering effect — that remains open.

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

---

## 7 - How malformed/inconsistent book input (beyond the 4 official validity rules) is handled

Raised: 05-10-2026 (M1)

The brief's 4 validity rules don't say anything about *technical* input inconsistencies
below the rule level — things like ids/`gotoId`s being authored as either a JSON number
(`"gotoId": 1000`) or a quoted string (`"gotoId": "1000"`) in the same file, or a
section's `type` being blank/unrecognized (`"type": ""`). Left unhandled, these would
either crash the whole file's parse with a generic Jackson exception (surfaced to the
operator as "book file is empty or not valid JSON", which is misleading — the JSON
itself is syntactically valid) or silently misbehave.

**This is my own decision, not something the brief or the architecture docs required** —
documenting it here rather than silently baking it in:

- **Mixed numeric/string ids and `gotoId`s** (e.g. `"id": 1` vs `"id": "500"`,
  `"gotoId": 1000` unquoted vs `"gotoId": "1000"` quoted) are normalized to `String` at
  parse time (`FlexibleStringDeserializer`), so rule 3's `gotoId` comparison is always
  type-safe regardless of how the source file authored each id. This was already in
  place from the start of M1 (the sample data itself mixes both forms).
- **A blank/missing/unrecognized section `type`** (e.g. `"type": ""` or a typo like
  `"BEGUN"`) is now normalized to `null` at parse time (`FlexibleSectionTypeDeserializer`)
  instead of aborting the whole book's parse. `Book.validate()` turns a `null` type into
  its own specific, diagnosable error ("section X has a missing or unrecognized type")
  rather than one generic "not valid JSON" message that hides which section/field is
  actually broken.

Open questions to resolve later (not blocking, interim assumption in place):
- Should a blank/unrecognized `type` be treated as a 5th official invalidity rule (listed
  explicitly in `docs/05-business-architecture.md`), or is "technical parse leniency with
  a clear error message" sufficient without formalizing it as a named rule? Currently
  implemented as the latter.
- Should the same leniency be extended to `Difficulty`/`ConsequenceType` (also strict
  enums today), or are those lower-risk/out of scope since no sample data currently
  exercises a blank/unrecognized value for them?

---

## 8 - Mockup's per-section title and per-option "hint"/"Requires" tags don't exist in our data model

Raised: 05-10-2026 (M3 UI pass)

Context: the assessment mockup's game screen (`Figure 2`) shows a per-section title
("The Cave Entrance"), an italic one-line hint under each option *before* it's chosen
("The blue light might be magical, but the passage looks dangerous"), and a
"Requires: Strength" tag on one option — none of these exist in the real book JSON/API
(`Section` only has `id`/`text`/`type`/`options`; `Option` only has `description` and an
optional `consequence`, which is deliberately only revealed *after* it fires, per
`docs/00-PARKING_LOT.md` #3). Fabricating this content client-side (inventing a title or
a hint string from nothing) would violate "the frontend never duplicates/invents
business data" — so the M3 UI redesign intentionally **drops** these three mockup
elements rather than fake them, while keeping the mockup's layout, color palette,
numbered-option badges, and typography.

Open questions to resolve later:
- Should `Section` gain an optional `title` field, and `Option` an optional
  `hint`/`requires` field, authored in the book JSON? That's a book-schema change
  (affects validation rules + sample data), not just a UI change — needs explicit
  sign-off before implementing.
- If added, would `hint`/`requires` purely be flavor text (no gameplay effect), or does
  "Requires: Strength" imply an actual stat/requirement system not in scope today?

---

## 9 - Multiple user logins: how should saved progress be partitioned per-player in the DB?

Raised: 05-10-2026 (M4 — Save & resume)

Context: `docs/05-business-architecture.md`'s "Save / Resume behavior" explicitly
defines **one save slot per book** (not per player), because there is no auth yet
(`NFR-03`/`NFR-04` in `docs/04-non-functional-requirements.md` defer login entirely).
`SavedProgress` (new in M4) therefore uses `bookId` alone as its primary key — any
visitor who opens a given book sees/overwrites the *same* single save as anyone else
who played that book on this server. That's fine for the current single-user/no-auth
scope, but will silently misbehave the moment multiple real users share a deployment
(player B overwrites player A's save on the same book, with no isolation and no
warning).

Open questions to resolve later (once auth/NFR-03/NFR-04 are picked up):
- Schema change: `SavedProgress`'s primary key would need to become a composite
  `(bookId, userId)` (or a surrogate key + unique constraint on that pair), with
  `userId` sourced from whatever auth mechanism is eventually added (session, JWT,
  etc.) — not decided yet since no auth exists today.
- `GET`/`PUT`/`DELETE /api/books/{id}/progress` would need an implicit "current user"
  (from auth context, not a client-supplied body field — never trust the client to
  self-identify) rather than operating on the single global row.
- Migration concern: any saves created before auth exists (today's single-slot-per-book
  rows) would need an explicit decision on what happens to them — discard, or attach to
  a default/anonymous user?
- Out of scope for M4: implemented as single-slot-per-book, matching the current
  documented (no-auth) business rule as-is — this entry is purely a forward-looking
  note for whenever login/auth becomes a real objective.

---

## 10 - Orphaned saved progress when a book is later removed; no explicit "discard save"; book-id stability

Raised: 05-10-2026 (M4 — Save & resume)

Context/questions raised: "if I save a game for book A, close the app, then remove
book A — what should happen? Do we have a way to remove the game (save)? Is the book
id idempotent?"

**What happens today (as implemented in M4), and the gaps found while answering**:

- **Book id stability**: yes, it's idempotent/deterministic — `BookFileAdapter` derives
  `bookId` from the book file's **name stem** (e.g. `dragon-quest.json` → id
  `dragon-quest`), not a random/generated value, so the same filename always produces
  the same id across restarts and re-runs of the ingestion pipeline. `SavedProgress`
  uses that same `bookId` as its primary key (see entry #9), so a save correctly
  survives an app/server restart as long as the book file (same name) still exists in
  `valid/`.
- **Removing book A while a save exists (the actual question raised)**: if the book
  file is deleted from `valid/` (or moved out), the next catalog reload simply drops it
  — `GET /api/books/{id}` and `POST /api/books/{id}/play` both correctly 404 for that
  id. **However, the `saved_progress` row for that `bookId` is never cleaned up** —
  `ProgressService`/the `/progress` endpoints don't check whether the book still
  exists before reading/writing it, so `GET /api/books/{id}/progress` would still
  happily return the stale save even though the book itself is gone. The **frontend**
  gap: `Game`'s `ngOnInit` fetches book detail first — if that 404s, it falls into the
  generic `error()` state ("Something went wrong. Please try again.") rather than a
  specific "this book no longer exists, and any saved progress for it is now orphaned"
  message. This is a *different* edge case from the one already handled
  (`resumeBroken` — book still exists, but the saved **section id** inside it no
  longer matches, e.g. after a content edit); removing the whole book was not
  specifically handled.
- **No standalone "discard/forget my save" action**: `DELETE /api/books/{id}/progress`
  exists and works, but today it's only ever invoked indirectly, via **Restart** (which
  also immediately begins a fresh playthrough) or the server-side auto-clear on
  `WON`/`DEAD`. There is no UI affordance for "just delete my save, without starting a
  new game right now" (e.g. a trash icon next to a "Continue" entry, if/once a
  home-page saved-games list is built — see the open question already in entry #9's
  sibling discussion and the still-unresolved "where do saved games show on the home
  page" question from the M4 design notes).
- **Same filename, different content** (not asked, but adjacent): if a book file is
  deleted and *replaced* with a new file using the **same name** (so the same `bookId`
  is reassigned to effectively different content/sections), any still-present save row
  for that id would suddenly look "valid" again on a stale `currentSectionId` that may
  no longer mean the same thing in the new content — a subtler version of the same
  orphaning problem, currently undetected.

Open questions to resolve later (not blocking M4, interim behavior: orphaned rows are
left in place, no cleanup job, no dedicated "forget save" UI):
- Should book removal (detected at the next ingestion/catalog reload) proactively
  delete any orphaned `saved_progress` row for that `bookId`, rather than leaving it to
  be discovered lazily (or never) the next time someone tries to resume?
- Should `Game`'s book-detail-fetch failure path distinguish "book not found" (likely
  removed) from other errors, with a clearer message than the current generic
  "Something went wrong"?
- Should there be an explicit "discard save" control independent of Restart (e.g. on a
  future home-page saved-games list), so a player can clear a save without immediately
  starting a new playthrough?
- Should the ingestion pipeline guard against silently reassigning a `bookId` to
  unrelated new content (e.g. warn/refuse if `valid/<name>.json` changes meaningfully
  while a save exists for that id)? Probably over-engineering for this brief's scope,
  but worth a conscious "no" rather than an unconsidered gap.

---

## 11 - Home-page "Continue" listing (US-10 AC1) — scoped out, documented deliberately

Raised: 05-10-2026 (M4 — Save & resume, closing note)

US-10's first acceptance criterion says: "Given I have one or more saved games, I can
see them (e.g. **on the home page or a 'Continue' section**)..." — this reads as if a
home-page listing might be required. `docs/05-business-architecture.md`'s "Save /
Resume behavior" section (the tie-breaker doc for exact business-rule wording) only
specifies the **"Resume your adventure?" modal on the game screen** as the mechanism
for surfacing a save — it does not mention a home-page listing at all, and the
Objective-4 mockup notes in `docs/01-challenge-understanding.md` don't show one either.

**Decision**: treat "e.g. on the home page..." as one *illustrative* example of how a
player could see their save, not a second mandatory surface — the modal already shows
book title (via the game screen itself), last saved section id, and HP at save time,
satisfying the AC's literal requirement ("I can see them... with book title, last
section reached, and HP at save time"). A home-page "Continue" card/badge was
**not implemented** in M4; this is a deliberate scope decision, not an oversight.

Open question if revisited later: would a home-page "Continue" entry (e.g. a badge on
the book card, or a dedicated section) be a meaningful UX improvement beyond the
"quality over quantity" ceiling intended for this exercise? Low priority — the modal
flow already fully satisfies the documented business rule.

---

## 12 - What happens if a book's JSON has duplicate section ids? ✅ Resolved (06-10-2026)

Raised: 06-10-2026 — Resolved: 06-10-2026

Idea: the brief's 4 official validity rules don't explicitly say "every section `id`
must be unique within a book." If a book file has two sections sharing the same `id`
(e.g. two objects with `"id": "5"` in the `sections` array), what should happen?

**Decision**: formalized as a new **Rule 5** in `Book.validate()` — every section `id`
must be unique within the book; a duplicate produces an explicit
`"duplicate section id: X"` validation error (rejecting the whole file at ingestion,
same mechanism as Rules 1-4), rather than silently keeping only one of the duplicates.
`docs/05-business-architecture.md`, `docs/03-technical-architecture.md`, and
`.github/instructions/backend.instructions.md` have been updated from "Rules 1-4"/
"the 4 rules" to "Rules 1-5"/"the 5 rules" to match.

---

## 13 - Show the HP amount gained/lost alongside the consequence text

Raised: 06-10-2026

Context: the `DEAD`/`WON`/consequence banner (e.g. "⚠️ The destruction of the Crown
consumes nearly all your remaining strength.") shows only the flavor text
(`consequenceText`, entry #3), not the actual numeric HP delta that just applied (e.g.
"-6 HP"). A player sees their HP drop (or the final "0 HP"/death screen) but has to
infer the exact amount from the before/after header value rather than being told
directly in the message itself.

Open questions to resolve later:
- Does `PlayResponse` need a new field (e.g. `consequenceValue`/`healthDelta`, signed
  int) alongside `consequenceText`, or can the frontend already derive it locally from
  "health before this move" minus "health after" (it already holds both, per the
  stateless `/play` contract) without a new backend field?
- If derived client-side: is that "computing business data" (forbidden — frontend
  never duplicates backend logic) or just "subtracting two numbers the backend already
  gave it" (presentation-only, not a new rule)? Leaning the latter since HP clamping
  logic itself still lives entirely server-side and isn't being reimplemented — only
  needs a decision before implementing either way.
- Where would it be shown — appended into the existing consequence banner text (e.g.
  "...consumes nearly all your remaining strength. (-6 HP)"), or as a separate small
  badge near the HP indicator?
- Relates to entry #3 (how `consequence.text` is surfaced) and entry #6 (HP display
  consistency across outcome screens) — same general area of the gameplay screen.

**Status update (06-10-2026, resolved)**: implemented as a client-side derivation, no
new backend field. In `Game.chooseOption()`, the frontend already holds "health before
this move" (`current.health`, sent in the `/play` request) and receives "health after"
in the response (`response.health`) — the delta is simply `response.health -
healthBeforeMove`, computed only when the response carries a `consequenceText` (`null`
otherwise, so no stray "(+0 HP)" shows on consequence-free moves). This is pure
subtraction of two numbers the backend already returned; no HP clamping/business logic
is reimplemented client-side, so it doesn't violate "frontend never duplicates backend
logic". Rendered appended to the consequence banner, e.g. "⚠️ ...consumes nearly all
your remaining strength. **(-6 HP)**" — colored red for a loss, green for a gain (CSS
class `.gain`). `PlayResponse`/API contract unchanged. Covered by 3 new tests in
`game.spec.ts` (loss, gain, and "no delta shown when there's no consequence").

**Status update (06-10-2026, follow-up UI fix)**: while verifying this, found the
unrelated "✅ Saved!" confirmation (US-09) was inline among the header's flex buttons,
so its appearance/disappearance shifted the header (and the whole page below it) —
fixed by moving it to its own reserved-height line below the header, always present
with `visibility: hidden`/`visible` toggling (not conditional rendering), so layout
never shifts whether or not the message is showing. See
`docs/05-business-architecture.md`'s "Save / Resume behavior" section.