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

## Backend

```bash
cd backend
./mvnw spring-boot:run
```

- API: http://localhost:8080
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

## Project layout

```
backend/            Spring Boot API (boundary/control/entity/common packages)
frontend/            Angular SPA (home/game/shared/core)
docs/                Architecture & business decisions (source of truth)
.github/instructions/ Path-scoped implementation rules for backend/** and frontend/**
```
