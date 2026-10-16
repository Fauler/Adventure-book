# Parking Lot — Future Improvement Ideas

This document collects ideas raised during development/review that are not being acted
on now. Nothing here is a decision or a commitment — it's a holding area to revisit
later, deliberately, so we don't forget good questions but also don't derail the
current scope.

Each entry: what was raised, when, and open questions to resolve before acting on it.

Status legend in each heading: ✅ Resolved / Decided, 🟡 Partially resolved, 🚧 Open. Entries
without a marker are not used. Resolution notes sit at the end of each entry.

---

## 1 - i18n-readiness of player-facing text 🚧 Open
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

## 2 - Behavior when a request references an invalid/non-existent section ✅ Resolved (M6.2)
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

**Resolution (M6.2)**: `400` with the stable code `INVALID_SECTION` (and `INVALID_OPTION`
for a bad option index); never a 500. Full error table in
`docs/03-technical-architecture.md`.

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

## 4 - Admin module / book registration UX (one-by-one vs. batch) 🟡 Partially resolved (M5: single-book add; batch still open)
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

## 5 - Mockup shows book metadata that doesn't exist in our JSON/API today ✅ Resolved (06-10-2026)
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

## 6 - Show the player's current HP more consistently across the gameplay screen 🚧 Open (cosmetic)
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

## 7 - How malformed/inconsistent book input (beyond the 4 official validity rules) is handled ✅ Resolved (M1, with interim assumption)
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

## 8 - Mockup's per-section title and per-option "hint"/"Requires" tags don't exist in our data model 🚧 Open (deliberately dropped from the UI)
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

## 9 - Multiple user logins: how should saved progress be partitioned per-player in the DB? 🚧 Open (see #18 / M9)
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

## 10 - Orphaned saved progress when a book is later removed; no explicit "discard save"; book-id stability ✅ Resolved (M6.3, see #16)

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

## 11 - Home-page "Continue" listing (US-10 AC1) — scoped out, documented deliberately ✅ Decided (not implemented)
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

## 13 - Show the HP amount gained/lost alongside the consequence text ✅ Resolved (06-10-2026)
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

**Status update (15-10-2026)**: superseded by #20 — the amount now comes from the backend
(`healthChange`), not from a frontend subtraction.

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

---

## 14 - Delivery and documentation hardening (M6 + OpenAPI + repo hygiene + README) ✅ Resolved (M6.1)

Raised: 13-10-2026

Context: a comparison against another candidate implementation of the same brief
(`rhribeiro25/Adventure-Library`) surfaced delivery/documentation items that are cheap
and that an evaluator hits first. Grouped here as a single package of work.

Items:
- **Containerization (M6 / NFR-02)**: `docker compose up` must start the whole app.
  Two services only — backend (Spring Boot) and frontend (static build served by
  nginx, proxying `/api` to the backend). No PostgreSQL/Redis: books stay as flat
  files (NFR-06) and saves stay in file-based H2, so both `backend/data` and the H2
  file need a mounted volume to survive container restarts.
- **OpenAPI / Swagger UI**: add `springdoc-openapi` so the REST API is browsable and
  testable at `/swagger-ui/index.html`. Document request/response models and the
  `ApiError` shape (see entry #15).
- **Repo hygiene**: `.DS_Store` files exist across the tree (`backend/`, `.github/`,
  `backend/data/`, ...). `.gitignore` already lists them; the existing files must be
  deleted from disk and confirmed absent from the index (the user performs any git
  action manually).
- **README sanity check**: verify from a clean clone that the documented steps work
  as written (`./mvnw spring-boot:run`, `npm install && npm start`). Add:
  - a "How to run" section for Docker (`docker compose up --build`, ports, volumes);
  - an "AI usage" section, **required by the brief**: which tools were used, which
    parts were AI-assisted vs. implemented/decided manually, and how the
    `docs/` + `.github/instructions` setup was used to steer the assistant.
    Documentation of real usage, not a transcript of prompts.

Open questions:
- Frontend container: nginx static build vs. `ng serve`? (Leaning nginx.)
- Single exposed port (nginx proxy) or two (4200 + 8080)?

Priority: high for the README AI-usage section (explicit brief requirement); the rest
is medium.

---

## 15 - API quality: complete business-level error coverage ✅ Resolved (M6.2)

Raised: 13-10-2026

Context: `GlobalExceptionHandler` currently maps shape validation (400), unreadable
body (400), `DomainException` (400), `BookValidationException` (400),
`BookNotFoundException` (404) and a catch-all (500). A caller of the API still cannot
tell several distinct business failures apart, and some framework-level errors fall
through to the generic handler.

Missing errors to add:
- **Invalid/unknown section** referenced by `/play` or `/progress` (entry #2) —
  dedicated 400/404 with a clear reason instead of a generic message.
- **Invalid option**: option not belonging to the current section.
- **Saved progress not found** (`GET /progress` / `DELETE /progress` when none
  exists) with an explicit, consistent response contract.
- **Saved progress for a removed book** (entry #10) — 404 for the book, not a stale
  save.
- **Game already finished** (play requested from an `END`/`DEAD` state).
- **Framework-level errors** currently masked as 500: unsupported method (405),
  unsupported media type (415), missing/mistyped parameter (400), unmatched route (404).
- **Stable machine-readable error code** in `ApiError` (e.g. `BOOK_NOT_FOUND`,
  `INVALID_SECTION`, `INVALID_OPTION`, `PROGRESS_NOT_FOUND`) so clients do not parse
  message text; keep human-readable messages alongside.
- Guarantee that no stack trace or internal detail reaches the response body.

Open questions:
- Add `code` to `ApiError` (contract change; the frontend must be adapted) or keep the
  contract and only fix status codes?
- Is the 500 catch-all enough for unexpected failures, or should it carry a
  correlation id that is also logged?

Each new error needs a test at the controller level.

---

## 16 - Save feature: explicit "discard save" endpoint and UI ✅ Resolved (M6.3)

Raised: 13-10-2026

Context: closes the gap described in entry #10. `DELETE /api/books/{id}/progress`
already exists, but today it is only triggered indirectly (Restart, or auto-clear on
`WON`/`DEAD`). There is no way to discard a save without starting a new playthrough,
and saves for books that no longer exist are never cleaned up.

Scope:
- Make the discard operation an explicit, documented endpoint with a defined contract:
  `204` on success; behavior when no save exists (idempotent `204` vs. `404` — to be
  decided together with entry #15).
- Frontend: a "Discard save" action on the "Resume your adventure?" modal, separate
  from Restart, with a confirmation step because the action is destructive.
- Orphan cleanup: when the catalog reload detects a removed book, delete its
  `saved_progress` row (or reject access to it) so a stale save is never returned.
- Tests: backend (endpoint + orphan cleanup) and frontend (modal action).

Out of scope: home-page saved-games list (entry #11) and per-player partitioning of
saves (entry #9).

Open questions:
- Idempotent `204` or `404` when nothing is saved?
- Should orphan cleanup happen eagerly at reload or lazily on access?

### Resolution notes for #14-#16 (M6, 13-10-2026)

- **Docker**: two services (backend, frontend on nginx), exposed on two ports (`8080`,
  `4200`); `backend/data` is bind-mounted so books and the H2 file survive restarts.
  Verified with `docker compose up --build` (health, catalog, SPA route fallback).
  Infrastructure files live in `infrastructure/docker/{backend,frontend}/` (Dockerfiles,
  `nginx.conf`); images run as non-root (uid 1000, matching typical Linux hosts so the bind-mounted
  `backend/data` stays writable; only verified on macOS) with JVM RAM-percentage flags.
  Deliberately left out as unnecessary for two services: custom network, restart policy,
  frontend healthcheck, remote-debug port.
- **OpenAPI**: `springdoc-openapi-starter-webmvc-ui` 3.0.3, UI at `/swagger-ui/index.html`.
- **Repo hygiene**: all `.DS_Store` files deleted from disk (already in `.gitignore`).
- **Errors (#15)**: stable `code` added to `ApiError` (`ErrorCode` enum, full table in
  `docs/03-technical-architecture.md`); framework errors (404/405/415/bad parameter) no
  longer fall into the generic 500. Decision: `GET /progress` keeps `204` when nothing is
  saved (the frontend relies on it to skip the resume modal), so no `PROGRESS_NOT_FOUND`
  code exists. The correlation-id question is left to Micrometer tracing (trace ids are
  already in the logs).
- **Save (#16, closes #10)**: `DELETE /progress` is idempotent `204`. `GET /progress` of a
  removed book is `404 BOOK_NOT_FOUND` and deletes the orphaned row; orphans are also
  purged eagerly at startup after ingestion. The frontend resume modal has a confirmed
  "Discard save" action (deletes and returns to the library), and the game screen shows
  a specific "book no longer exists" message. The same-filename-different-content case
  from #10 stays a conscious "no".

---

## 17 - Server-side book search: pagination, sorting and filters 🚧 Open (M10)
Raised: 13-10-2026

Context: `GET /api/books` returns the full list and the frontend filters it. That is
enough for a handful of books, but it does not scale and puts search logic in the
client, against the "backend is the source of truth" principle.

Scope:
- Query params on `GET /api/books`: free-text query, title, author, difficulty,
  category/type, `page`, `size`, `sort`, `direction`.
- Response wrapper with items plus paging metadata (page, size, total elements/pages).
- Defaults and limits (e.g. `size` capped) and validation of `sort` against an
  allow-list of fields.
- Frontend: move filtering to the API and add a pager.

Open questions:
- Books live in memory (flat files, NFR-06), so this is in-memory filtering and slicing
  with no database benefit; is the contract change worth it at the current volume?
- Does the existing `type` field count as the "category"? Editable categories (add and
  remove) would be a separate feature.

Priority: low.

---

## 18 - Multiplayer: independent progress per player 🚧 Open (M9)
Raised: 13-10-2026

Context: the brief lists supporting multiple players, each with independent game
progress, as an extra objective. Today there is no player identity and `SavedProgress`
is keyed by `bookId` alone (see entry #9, which covers the save-partitioning part of
this problem).

Scope:
- A `Player` concept (identity, name) and a per-player game record: player + book +
  current section + HP + status.
- All game and progress endpoints operate on the current player, resolved server-side
  and never taken from a client-supplied body field.
- Migration decision for existing single-slot saves (discard, or attach to a default
  player).

Open questions:
- Identity mechanism: simple player registration/selection, or real auth (ties to
  NFR-03/NFR-04, M7)? Real auth without players protects nothing, so this decision
  comes first.
- Relation to entry #19: per-player games are naturally stored server-side, so the two
  should be designed together.

---

## 19 - Server-side game state (backend as the real source of truth) 🚧 Open (M8)
Raised: 13-10-2026

Context: `/play` is stateless — the client sends `{currentSectionId, optionIndex,
health}` and the server trusts the `health` value. A client can therefore send any HP
(error or cheating), and the `/progress` save also stores client-supplied values. The
backend validates moves but does not own the state, so it is not fully the source of
truth.

Scope:
- Persist an active game on the server: `POST` to start a game returns a `gameId`;
  moves are `POST .../choices` with only the chosen option; the server loads the
  current section and HP, applies the consequence, persists and returns the new state.
- The client never sends `health` or `currentSectionId`.
- Save, pause and resume become server operations on the game record (status such as
  `PLAYING`/`PAUSED`/`WON`/`DEAD`), removing the client-built save payload.
- Add optimistic locking (`@Version`) so concurrent requests on one game cannot
  overwrite each other.
- Update `docs/03-technical-architecture.md`, which currently documents the stateless
  contract and the single-user trust assumption.

Open questions:
- Migration of the frontend (`game.ts`) and of the M4 save flow (modal, restart,
  auto-clear on `WON`/`DEAD`) onto the game resource.
- Abandoned games: expiry/cleanup policy for games never finished.
- Keep a compatibility path for the existing `/play` and `/progress` endpoints, or
  replace them outright?

Priority: highest of this group; it is the largest change and is a prerequisite for
#18.


## 20 - New requirement: backend returns the consequence HP amount ✅ Resolved (15-10-2026)
Raised: 15-10-2026

Context: new requirement added by the product owner; it did not exist before. Today
the "(-N HP)" badge shown next to the consequence text (see #13) is derived in the
frontend from `health` before/after the move. The requirement is that the backend
returns the amount and the frontend only displays it, and that the amount always
matches the option's consequence.

Scenario (book "The Prisoner", section 500, option "Gather your thoughts" →
`LOSE_HEALTH 3`, text "You're getting a little more crazier..."):
- At 10 HP: `health: 7`; the player sees "(-3 HP)".
- At 1 HP: the option is the same and still costs 3 HP, so the player must see
  "(-3 HP)". Only `health` is clamped at 0 (→ `status: DEAD`). With the current
  frontend derivation the badge shows "(-1 HP)", so the message changes for the same
  option.

Requirement:
- `MoveResult`/`PlayResponse` get a `healthChange` field: the option's nominal signed
  consequence value (`-3`, `+5`; `0` when the option has no consequence), independent of
  the player's HP.
- The frontend renders `healthChange` as-is and no longer subtracts HP values.
- Implementation order: backend + tests first, then frontend, then docs.
- Docs to update when done: API contract and `MoveResult` in
  `docs/03-technical-architecture.md`, `docs/06-implementation-plan.md`, the resolution
  note of #13, and the affected user story in `docs/02-user-stories.md`.

**Status update (15-10-2026, resolved)**: implemented. `Consequence.signedValue()` gives
the nominal value; `Book.resolveMove` returns it as `MoveResult.healthChange` (exposed in
`PlayResponse`), `0` without a consequence. The frontend renders `response.healthChange`
and no longer computes anything. Tests: `BookGameTest` (nominal value kept when HP is
clamped, zero without consequence), `BookControllerPlayTest`, and `game.spec.ts` (a -3
shown when the player dies from 1 HP).
