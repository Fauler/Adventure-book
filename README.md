# Adventure Book

A choose-your-own-adventure game: play through branching story books, track HP,
save/resume progress, and add new books at runtime.

This is a monolith with two independently buildable/runnable apps:

- `backend/` — Spring Boot 4.1.1 (Java 21) REST API. Single source of truth for all
  game rules (book validity, move resolution, HP, win/death). See
  `.github/instructions/backend.instructions.md`.
- `frontend/` — Angular 22 SPA. Pure "dumb" view: renders backend responses, forwards
  user input, no business logic duplicated client-side. See
  `.github/instructions/frontend.instructions.md`.

Full design decisions and rationale live in `docs/` (read in numbered order, starting
with `docs/01-challenge-understanding.md`); the build sequencing plan is in
`docs/06-implementation-plan.md`.

## Prerequisites

- Java 21
- Maven 3.9+ (or use the bundled `./mvnw` wrapper)
- Node.js 26+ and npm 11+

## Run with Docker (one command)

Requires Docker with Compose. From the repo root:

```bash
docker compose up --build
```

- App: http://localhost:4200 (nginx serves the Angular build and proxies `/api` to the backend)
- API: http://localhost:8080 — Swagger UI: http://localhost:8080/swagger-ui/index.html
- `backend/data/` is mounted as a volume, so the book catalog and saved games survive
  restarts. Drop new book JSON files in `backend/data/books/incoming/` and restart, or
  use the "Add book" screen.
- Stop with `docker compose down`.

## Backend

```bash
cd backend
./mvnw spring-boot:run
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Health check: http://localhost:8080/actuator/health
- Book catalog flat files: `backend/data/books/{incoming,valid,invalid}` — JSON files
  dropped in `incoming/` are validated and sorted into `valid/`/`invalid/`
  (+ `<name>.errors.txt` on failure) on every startup. The repo ships with the sample
  books already sorted and committed in `valid/`/`invalid/` (3 corrected/playable books
  in `valid/`, the 4 originally-provided files + their error reports in `invalid/` —
  see `backend/src/test/resources/sample-books/FIXES.md` for what was corrected and
  why); `incoming/` starts empty and is only the drop zone for adding new books.
- Saved-game data: file-based H2 database under `backend/data/db/` (gitignored,
  recreated automatically).

Run backend tests:

```bash
cd backend
./mvnw test
```

## Frontend

```bash
cd frontend
npm install
npm start   # ng serve
```

- App: http://localhost:4200
- `ng serve` proxies `/api/*` to `http://localhost:8080` (see `frontend/proxy.conf.json`),
  so start the backend first if you want the library page to show real data.

Run frontend unit tests:

```bash
cd frontend
npm test
```

Build a production bundle:

```bash
cd frontend
npm run build
```

## API errors

Every error response has the same body, `{ timestamp, status, code, error, messages[] }`,
where `code` is a stable identifier (e.g. `BOOK_NOT_FOUND`, `INVALID_OPTION`,
`GAME_ALREADY_FINISHED`). The full table is in `docs/03-technical-architecture.md`.

## AI usage

The brief allows AI-assisted development provided it is documented. This is how it was
used here.

**Tool**: GitHub Copilot CLI (Claude models), working inside this repository.

**How it was steered** — rather than ad-hoc prompting, the decisions were written down
first and the assistant was constrained by them:
- `docs/` holds every business/technical decision, numbered in reading order, plus a
  `00-PARKING_LOT.md` where open questions are recorded instead of being guessed.
- `.github/copilot-instructions.md`, `.github/instructions/` (backend/frontend rules)
  and `.github/skills/` (`implement-next`, `grill-me`) make the assistant read those docs,
  follow the ECB layout, never run git, and keep the docs up to date.
- Work was done milestone by milestone (`docs/06-implementation-plan.md`), stopping after
  each one for review and a manual commit.

**What the AI did**: read the brief and sample books and summarised the rules; drafted
user stories, architecture and NFR documents; generated the Spring Boot and Angular
code, unit tests and Docker setup for each milestone; ran the builds and tests.

**What was decided or done manually**:
- Interpreting the brief: the 4 sample books were treated as examples that must pass
  through the same validation (so all 4 are rejected, and corrected `_FIXED` copies were
  added); the extra validity rule for duplicate section ids; HP having no upper cap.
- Architecture choices (DDD-lite + ECB, backend as source of truth, flat-file books,
  file-based H2 for saves) and every parking-lot decision.
- UI/UX review against the mockups, bug reports (stop-confirmation bypass, double
  submit, layout shift on "Saved!") and the commits themselves.
- Reviewing the generated code and docs before each commit.

## Project layout

```
backend/            Spring Boot API (boundary/control/entity/common packages)
frontend/            Angular SPA (home/game/shared/core)
docs/                Architecture & business decisions (source of truth)
.github/instructions/ Path-scoped implementation rules for backend/** and frontend/**
```
