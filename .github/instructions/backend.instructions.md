---
applyTo: "backend/**"
description: Backend (Spring Boot) implementation rules for Adventure Book
---

# Backend instructions

Source of truth for all decisions below: `docs/03-technical-architecture.md` and
`docs/05-business-architecture.md`. Read the relevant section there before implementing
anything non-trivial; do not re-derive architecture decisions from scratch, and do not
silently contradict them. If a decision is genuinely missing from both docs, add an entry
to `docs/00-PARKING_LOT.md` instead of guessing.

## Core principle

- **The backend is the single source of truth.** All game rules, validation, and HP/state
  logic live here. Never design an API response that assumes the frontend will compute,
  validate, or re-derive business data — the frontend only renders what it's given.

## Architecture: DDD-lite + ECB

Keep code in these packages, matching `docs/03-technical-architecture.md`:

- `boundary/` — REST controllers, the book file-system adapter, JPA repositories. Only
  translation/transport concerns live here (DTO ↔ domain mapping, HTTP status mapping).
- `control/` — stateless services (`BookCatalogService`, `GameEngineService`,
  `ProgressService`, ...) orchestrating use cases. No business rule should be hidden in a
  controller or inlined in a repository.
- `entity/` — the domain model (`Book`, `Section`, `Option`, `Consequence`) and **all**
  domain rules, including the 5 book-validity rules. `Book.validate()` is the single place
  those rules are implemented and tested — do not duplicate rule checks elsewhere (e.g. in
  a controller or the file-ingestion adapter).
- `common/` — cross-cutting concerns (`GlobalExceptionHandler`, shared constants).

## Book validity rules (exact, see `docs/05-business-architecture.md`)

1. Exactly one `BEGIN` section.
2. At least one `END` section.
3. Every `gotoId` must match an existing section `id` — compare as a normalized type
   (e.g. `String`), since sample data mixes numeric and string ids.
4. Every non-`END` section must have non-empty `options` — checked for **all** sections in
   the file, not just sections reachable from `BEGIN`.
5. Every section `id` must be unique within the book.

Only `Book.validate()` enforces these. The ingestion pipeline (`incoming/valid/invalid`
folders) calls it and never re-implements or relaxes any rule.

## Validation layering

- **Boundary (Bean Validation)**: structural/shape checks on incoming DTOs (`@NotNull`,
  `@NotBlank`, `@Valid`, etc.) — "is this a well-formed request," not "is this a legal game
  state."
- **Entity (domain rules)**: the 5 book-validity rules, HP clamping (never below 0; no
  hard-coded max — 10 is only the documented *starting* value), game-end state derivation
  (`WON`/`DEAD` only), and any other business rule belong in the domain model, not in
  controllers or Bean Validation annotations.

## Persistence

- Book *content* lives only in flat files under `backend/data/books/{incoming,valid,invalid}`
  — never duplicate book JSON into a database (see NFR-06 in
  `docs/04-non-functional-requirements.md`).
- Saved *game progress* uses file-based H2 via Spring Data JPA — single save slot per book,
  no save history, auto-cleared the moment a game ends (`WON`/`DEAD`).

## Logging & observability

- Use `@Slf4j` (Lombok) for logging; no `System.out`/raw `printStacklnTrace`.
- Favor Spring Boot's built-in observability (trace/span IDs, Micrometer/OpenTelemetry
  integration) over custom correlation-id plumbing, per `docs/03-technical-architecture.md`.

## API contract

- Keep `/api/books/{id}/play` and related endpoints stateless as specified in
  `docs/03-technical-architecture.md` — no server-side "game session" resource; every call
  takes the current state and returns the next state.
- Don't invent new response fields without checking the API contract table in
  `docs/03-technical-architecture.md` first; if a frontend need isn't covered there, flag it
  (update the doc or add to the parking lot) rather than freelancing the contract.

## Out of scope for now

Docker containerization and token/JWT auth are backlog items (see
`docs/03-technical-architecture.md` backlog and `docs/04-non-functional-requirements.md`
NFR entries) — don't implement them unless explicitly asked to pick up that objective.
