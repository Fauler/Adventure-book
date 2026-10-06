# 05 - Business Architecture

Where `03-technical-architecture.md` defines *how* the system is built (stack, ECB
layers, API shapes) and `02-user-stories.md` defines *what* the player can do, this doc
nails down the precise **domain behavior and edge-case rules** that make those stories
unambiguous to implement — the kind of decisions that are easy to gloss over until
you're mid-implementation and hit the edge case for real.

---

## Book validation (Rules 1-5)

Lives in `Book.validate()` (Entity layer), used uniformly by `BookCatalogService`
(Control layer) in exactly two places: **initial load** (the startup pipeline,
`03-technical-architecture.md`) and **when a new book is registered** (US-11, add a
book at runtime). There is only one validator — no separate/looser rules for
"official" sample books vs. user-submitted ones.

1. Exactly **1** section with `type == BEGIN`.
2. At least **1** section with `type == END`.
3. Every `gotoId` referenced in `options` must exist as the `id` of some section in the
   same book (comparison must be type-safe: section `id` and `gotoId` are compared as
   the same type — recall the sample data mixes numeric and string ids, e.g. `1` vs.
   `"500"`, so this must be normalized, not compared with mixed types).
4. Every non-`END` section must have non-empty `options` — checked for **all** sections
   in the file, not only the ones reachable from `BEGIN`. (This is what makes
   `the-prisoner.json`'s orphaned section `666` invalid, even though a player could
   never actually reach it by playing.)
5. Every section `id` must be unique within the book — a file with two sections sharing
   the same `id` is rejected with an explicit "duplicate section id" error, rather than
   silently keeping only one of them (added 06-10-2026, see
   `docs/00-PARKING_LOT.md` #12 for the discussion that led to this rule).

Books that fail validation are **excluded from the catalog** — logged with every
failed rule, never crash the application. This applies identically whether the book is
one of the bundled samples or submitted through the add-book feature. (Mechanically,
per `03-technical-architecture.md`, this means the file lands in `invalid/` with a
`.errors.txt` instead of `valid/` — the *rule evaluation* described above is identical
either way, only the filesystem side-effect differs by entry point.)

---

## Game-end states: only `WON` or `DEAD`

Since validation (Rules 3 and 4, applied **structurally to every section**, reachable
or not) already guarantees that:
- every referenced `gotoId` points to an existing section (Rule 3), and
- every non-`END` section has `options` (Rule 4),

...a book that passed validation can **never**, during actual gameplay, land on a
non-existent section or on a section with no options. For that reason, **there is no
third game-state such as "Game Over due to a dead end"** — there are only two possible
outcomes:

- **`WON`** — the player reached an `END` section.
- **`DEAD`** — the player's HP reached 0.

As a defensive-engineering practice (not a business rule), we still keep a generic
technical error handler on the backend for the case where something unexpected happens
at runtime anyway (e.g. corrupted data bypassing validation somehow) — that becomes a
technical error (`5xx`), never a fabricated third "Game Over" business state, since it
should be structurally impossible with a book that passed validation.

---

## Cyclic paths between sections

A book's sections form a directed graph (`gotoId` edges). Rules 3 and 4 guarantee every
edge resolves and every non-`END` node has outgoing edges, but they do **not**
guarantee the graph is acyclic, or that every path eventually reaches an `END`. Two
sections can legitimately point back to each other (`A → B → A`), and a player who
keeps choosing that path never reaches an ending.

This is **not a deadlock**: no thread, lock, or request ever blocks waiting on another.
Each `/play` call is a single stateless, synchronous request/response — the backend
never holds a session open between two client calls (per the stateless design in
`03-technical-architecture.md`). It is a **content/design concern** (a non-terminating
path through the story graph), entirely driven by which option the player picks at each
step, not a system failure. It mirrors how printed gamebooks work: a passage is allowed
to send the reader back to an earlier one.

Because "every path from `BEGIN` eventually reaches an `END` (or a certain death)" is a
general graph-reachability property — not a structural file-shape check like Rules
1-5 — it is **not** added as a formal additional validation rule (Rules 1-5 remain the
complete contract for what makes a book loadable). Instead, it's treated as an
**authoring-quality check** applied to the books shipped with this project: a backend
test walks each bundled book's graph and asserts that every reachable section has at
least one path toward an `END` section, so the sample content itself is never stuck in
a mandatory loop. (Implementation idea: a reverse breadth-first search from every `END`
section finds every section that *can* reach an ending; a forward breadth-first search
from `BEGIN` finds every section a player can actually land on; any id in the second set
but not the first is flagged.) This is a test-suite/content-quality concern, not
something the running application enforces or rejects at upload time.

---

## Save / Resume behavior

Builds on US-09/US-10 (`02-user-stories.md`) and the `/api/books/{id}/progress`
endpoint (`03-technical-architecture.md`):

- Each book has a **single save slot** on the backend (`GET`/`PUT`/`DELETE
  /api/books/{id}/progress`) — saving again simply **overwrites** the previous save;
  there is no history of saves.
- On opening the game screen for a book, the book detail is fetched **first**, then
  the backend is asked whether a save exists for it (sequential, not parallel — the
  save-exists check needs the book's sections to detect a stale/broken resume, see
  `docs/00-PARKING_LOT.md` if this changes). If one does, a
  **"Resume your adventure?"** modal appears **before any section is shown**, offering
  **Continue** (jump straight to the saved section/health) or **Restart** (delete the
  save immediately and begin at `BEGIN` with full starting HP). If no save exists, the
  player goes straight into a fresh playthrough — no modal.
- A **Save** button appears in the header next to **Stop** once a playthrough is
  underway (ties into US-08's "Stop" and the header requirements) — it stores the
  current section + health and shows a brief "Saved!" confirmation.
  - The confirmation renders on its own reserved-height line directly below the
    header (not inline among the header buttons) — the space for it is always present
    while playing (hidden via `visibility`, not `display`/conditional rendering), so
    the header and section content never shift position when the message appears or
    disappears.
- The save is **cleared automatically** the moment the game ends (`WON` or `DEAD`) — a
  finished playthrough has nothing left to "continue," so the next visit to that book
  always offers the fresh-playthrough flow again, never a resume-into-a-finished-game
  state.
- **Restart** also clears the save immediately (not just on next save) — choosing
  Restart with an existing save is itself a decision to discard it.

Both "auto-clear on end" and "clear on restart" are explicit product decisions (not
left as an implementation afterthought) and should be reflected as acceptance criteria
when US-09/US-10 are implemented.

### HP gained/lost display (`docs/00-PARKING_LOT.md` #13)

Alongside the consequence banner's flavor text, the actual signed HP change for that
move (e.g. "(-6 HP)"/"(+5 HP)") is now shown too — colored red for a loss, green for a
gain. Computed entirely client-side as `health after the move - health before the
move`, both values already present in the stateless `/play` response; no new backend
field, no HP/clamping logic duplicated. Shown only when the move has a consequence
(`null` on consequence-free moves, so nothing renders for a plain "(+0 HP)").

---

## Add a new book (US-11, extra)

A dedicated screen (linked from the library, e.g. a "+ Add a new book" entry point)
lets the user paste a book's JSON — the same shape as any file in `data/books/` — and
submit it via `POST /api/books`.

- **The frontend does no business-rule validation of its own.** It only checks that
  the pasted/typed text is syntactically valid JSON (a fast, friendly "that's not valid
  JSON" message for typos, before even calling the backend). Every actual rule (has a
  `BEGIN`, no dangling `gotoId`, etc. — Rules 1-5 above) is applied by the backend,
  identically to startup validation. The frontend simply displays whatever list of
  rejection reasons comes back, **one per line, verbatim** — it never invents its own
  wording for *why* a book was rejected (consistent with "backend is the source of
  truth" in `03-technical-architecture.md`).
- On success, the new book immediately appears in the library (same in-memory catalog,
  no restart needed — per the pipeline's on-demand re-ingestion in
  `03-technical-architecture.md`), and the screen offers "Add another" or "Go to
  Library".
- The JSON can be provided three ways, all feeding the same textarea/submit flow:
  typed/pasted directly, picked via a "Choose file" button (a hidden native
  `<input type="file">` triggered by the visible button), or drag-and-dropped as a
  `.json` file onto the textarea. All three just read the file as text in the browser
  and populate the same textarea — no extra backend behavior or validation path per
  input method.

### Book file naming (runtime-registered books)

When a book is registered at runtime (not one of the originally bundled samples), it is
persisted as a **slug of its own title** — lowercased, non-alphanumeric characters
collapsed to `_` (e.g. `"The Crystal Caverns"` → `the_crystal_caverns.json`) — rather
than an opaque `book-{id}.json` scheme, so the filename itself hints at which book it
is without opening it. If the slug is already taken (e.g. two submitted books happen to
share the same title), a numeric suffix is appended and incremented until a free name is
found (`the_crystal_caverns_2.json`, `_3.json`, ...).

This **only disambiguates filenames on disk** — it does not solve (and was never meant
to solve) telling two same-titled books apart *in the UI* itself; that's tracked
separately as an open question in `00-PARKING_LOT.md`.

**Resubmitting identical content is idempotent.** If a user clicks "Submit book" more
than once without changing anything (same title *and* byte-identical JSON), the numeric
suffix is **not** incremented again — the existing `slug[_N].json` file is reused as-is
(same outcome: same book id if valid, same rejection reasons if invalid). Two genuinely
different submissions that merely happen to share a title still each get their own
numbered file, per the paragraph above.

---

## Traceability back to user stories

| Section above | User story / doc |
|---|---|
| Book validation (Rules 1-5) | US-01 (`02-user-stories.md`), pipeline (`03-technical-architecture.md`) |
| Game-end states (`WON`/`DEAD`) | US-07 |
| Cyclic paths (authoring-quality check) | US-01 / US-05 (not a formal rule, a test-suite concern) |
| Save/Resume behavior | US-09, US-10 |
| Add a new book + file naming | US-11 |
