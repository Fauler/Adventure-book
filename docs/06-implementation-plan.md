# 06 - Implementation Plan

This is the sequencing plan for actually writing code, derived from
`docs/01-challenge-understanding.md` (objectives, in order), `docs/02-user-stories.md`,
`docs/03-technical-architecture.md`, `docs/04-non-functional-requirements.md`, and
`docs/05-business-architecture.md`. It does not introduce any new decisions — it only
orders the work already agreed in those docs.

## Ground rules for executing this plan

- **One objective at a time**, per the brief's own tip ("focus on one objective at a time;
  it's fine not to finish everything — quality over quantity"). We do not start objective
  N+1 work while objective N is incomplete.
- Milestones below are **stop points**: once a milestone is done (code + a quick manual/
  automated check that it works), I will summarize what changed, propose a commit message,
  and **wait for you to review and commit manually** before continuing to the next
  milestone. I will never run `git` myself (per `.github/copilot-instructions.md`).
- If something ambiguous comes up mid-milestone, it goes to `docs/00-PARKING_LOT.md` with
  a stated interim assumption rather than silently guessing — then I continue.
- Stretch items (Docker, security) stay **after** Objectives 1-5 regardless of how much
  time is left, per NFR grouping in `docs/04-non-functional-requirements.md`.

## Milestone map

| # | Milestone | Objective(s) | User stories | Status |
|---|---|---|---|---|
| M0 | Project scaffolding | prerequisite | — | ✅ Done |
| M1 | Book validation + Home/Library | Objective 1 | US-01, US-02, US-03, US-04 (nav only) | ✅ Done |
| M2 | Play a game — basic interactions | Objective 2 | US-05 | ✅ Done |
| M3 | Consequences, health, game end | Objective 3 | US-06, US-07, US-08 (phase 1: simple Stop) | ✅ Done |
| M4 | Save & resume | Objective 4 (extra) | US-09, US-10, US-08 (phase 2: revisit Stop/Pause) | ✅ Done |
| M5 | Add a new book | Objective 5 (extra) | US-11 | 🚧 To do |
| M6 | Stretch: containerize app | NFR-02 (Group C) | — | 🚧 To do |
| M7 | Stretch: token-based security + JWT | NFR-03 → NFR-04 (Group D) | — | 🚧 To do |

Update this column as each milestone is actually finished (not when it's merely planned
below) — it's the at-a-glance source of truth for "where are we right now".

Group A (NFR-01, centralized exception handling) and Group B (NFR-05 monolith, NFR-06
flat-file books) are **not separate milestones** — NFR-01 is folded into M1 (built in from
the start, not bolted on later) and Group B are already-locked decisions with no build work.

---

## M0 — Project scaffolding

**Goal**: both apps exist, build, and run empty, so every later milestone is additive.

- Backend: `spring init` (or manual `pom.xml`) for Spring Boot 4.1.1 / Java 21, with the
  `boundary/control/entity/common` package skeleton from `docs/03-technical-architecture.md`,
  Lombok + Bean Validation + Spring Data JPA + H2 dependencies, a basic
  `GlobalExceptionHandler` in `common/` (this *is* NFR-01 — built in now, not deferred).
  Verify `mvn spring-boot:run` starts cleanly.
- Frontend: `ng new frontend` (Angular 22.2.1, SCSS, no SSR unless default), routes stubbed
  per the routes table in `docs/03-technical-architecture.md`, a thin `core/` API module
  placeholder. Verify `ng serve` renders an empty shell.
- Root **README.md** with build/run instructions for both apps (required by the brief).
- Copy the real sample books (`crystal-caverns.json`, `pirates-jade-sea.json`,
  `the-prisoner.json`, `dragon-quest.json`) into `backend/data/books/incoming/` so the
  pipeline has real fixtures from the start.

**Stop here** → I propose a commit message, you commit, we move to M1.

---

## M1 — Book validation + Home/Library (Objective 1)

**Goal**: all 4 sample books are correctly sorted into `valid`/`invalid` at startup, and a
user can see, search, and filter the valid ones on a home page.

Backend:
- `entity.Book` + `Section`/`Option`/`Consequence` domain model, `Book.validate()`
  implementing the 4 rules exactly (type-safe `gotoId` comparison; checks **all** sections
  for the empty-options rule, not just reachable ones).
- Book ingestion pipeline (`incoming` → `valid`/`invalid` + `.errors.txt`) per the mermaid
  flowchart in `docs/03-technical-architecture.md`, run at startup (US-01).
- `GET /api/books` (list, valid only) with search/filter query params, per the API contract
  table.
- Unit tests: all 4 validity rules individually, plus the real sample files. A full
  read of all 4 files (not just `the-prisoner.json`/`dragon-quest.json`) shows **all are
  actually invalid** — `crystal-caverns.json` and `pirates-jade-sea.json` each contain
  their own orphan `NODE` section `666` with no options, and `pirates-jade-sea.json` also
  has a dangling `gotoId: 999`. See the correction note in
  `docs/01-challenge-understanding.md`'s sample-data table.

Frontend:
- Home/library page (US-02, US-03): book grid, search box, filter pills, calling
  `GET /api/books`. No business logic client-side — rendering only.
- Routing to a (still-empty) game route on "Begin Quest" (US-04, navigation only — actual
  play logic is M2).

**Stop here** → propose commit message, wait for your confirmation, then M2.

---

## M2 — Play a game: basic interactions (Objective 2)

**Goal**: a player can start a book and click through options with no consequences yet.

Backend:
- `GameEngineService` + `POST /api/books/{id}/play` (stateless: takes
  `{currentSectionId, optionIndex, health}`, returns `{section, health, status}`), per
  the fixed API contract in `docs/03-technical-architecture.md` — the request/response
  shape is one contract shared across M2/M3, not redesigned incrementally; M2 simply
  passes `health` through unchanged (no `Consequence.applyTo()` wiring yet) and `status`
  naturally reflects `WON` on reaching an `END` section (`DEAD` cannot occur yet since
  health never decreases).
- Unknown book id → `404` (new `BookNotFoundException`); illegal in-book move (bad
  `currentSectionId`/`optionIndex`, acting on an already-ended session) → `400` via the
  existing `DomainException`, per `docs/00-PARKING_LOT.md` entry #2's 400 leaning.
- Boundary validation (Bean Validation) for the request shape; domain rules stay in
  `entity` (`Book.startGame()`/`resolveMove()`).

Frontend:
- Game screen (US-05): section text + options list, calling `/play` and re-rendering;
  end-of-game text shown once `status !== PLAYING` (full HP header/Stop control UI
  deferred to M3 per US-06/US-07/US-08).

**Stop here** → propose commit message, wait, then M3.

---

## M3 — Consequences, health, game end (Objective 3)

**Goal**: HP is tracked and clamped, consequences apply, and the game ends on `DEAD` or
`WON` — this is the core "succeed/fail" loop from the brief.

Backend:
- Consequence application (`LOSE_HEALTH` now; design the type extensibly per
  `docs/03-technical-architecture.md`) in `entity`, including HP clamping (floor 0, no
  hard-coded ceiling — 10 is only the starting value).
- `/play` now also takes/returns `health`, and returns `status` (`PLAYING`/`WON`/`DEAD`)
  plus `consequenceText` (the just-applied option's `Consequence.text`, `null` if none —
  resolves `docs/00-PARKING_LOT.md` #3).
- Tests: HP never below 0; `DEAD` triggered at/below 0 regardless of normal section type;
  `WON` only on reaching an `END` section; consequence ordering.

Frontend:
- HP indicator wired into the header (US-06).
- End-of-game screens for `WON`/`DEAD` (US-07), consistent HP display per the parking-lot
  note on HP-display consistency.
- US-08 phase 1: simple header **Stop** control (no unsaved-progress confirmation yet —
  that's deferred to M4, per the two-phase design in `docs/02-user-stories.md`).

**Stop here** → propose commit message, wait, then M4.

---

## M4 — Save & resume (Objective 4, extra)

**Goal**: progress persists across sessions, one slot per book.

Backend:
- JPA entity + repository for saved progress (file-based H2), `ProgressService`
  (save/load/delete), endpoints per the API contract table.
- Auto-clear save on `WON`/`DEAD` (per `docs/05-business-architecture.md`).

Frontend:
- "Save Progress" button (US-09); resume modal (Continue/Restart) shown on game-screen
  entry if a save exists (US-10).
- US-08 phase 2: revisit Stop behavior now that Save exists — add the unsaved-progress
  prompt distinguishing Stop-with-save-offered vs. plain exit, per the two-phase design.

**Stop here** → propose commit message, wait, then M5.

---

## M5 — Add a new book (Objective 5, extra)

**Goal**: a user can register a new book at runtime without restarting the app.

Backend:
- `POST /api/books` (paste/upload JSON) → re-runs the exact same `Book.validate()` used at
  startup; on success, writes to `valid/` using the slug-based file-naming scheme (with
  numeric collision suffix) from `docs/05-business-architecture.md`; on failure, returns
  rejection reasons.

Frontend:
- Add-book UI (US-11): paste / hidden file input / drag-and-drop, all feeding one textarea;
  frontend only checks JSON *syntax*, not business rules; backend rejection reasons shown
  verbatim, one per line.

**Stop here** → propose commit message, wait, then decide whether to continue into
stretch items (M6/M7) or stop, given time remaining.

---

## M6 — Stretch: containerize the app (NFR-02)

Dockerfile(s) for backend and frontend (+ optionally `docker-compose.yml`), per the backlog
entry in `docs/03-technical-architecture.md`. No functional changes to the app itself.

**Stop here** → propose commit message, wait, then M7 (only if time/priority allows).

---

## M7 — Stretch: token-based security + JWT (NFR-03 → NFR-04)

Token gate first (NFR-03), JWT real-identity auth after (NFR-04) — only pursued if there's
still time after Objectives 1-5, per the Group D NFR decision.

**Stop here** → propose commit message.

---

## Not milestones (already-decided, no build work)

- NFR-05 (stay monolithic) and NFR-06 (books stay flat-file, never in the DB) are locked
  architectural decisions, not tasks — nothing to schedule for them.
