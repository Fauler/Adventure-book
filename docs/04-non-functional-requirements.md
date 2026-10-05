# 04 - Non-Functional User Stories

These are cross-cutting / infrastructure concerns, as opposed to `02-user-stories.md`
(player-facing business behavior). Each one gets: the story, the trade-offs between
realistic options, and **when** we'd do it. At the end we group them into buckets so we
can later turn that into an ordered plan — the grouping is the goal of this doc, not a
final schedule yet.

---

## NFR-01 — Centralized exception handling

**As a** developer consuming or maintaining the API
**I want** every error (validation failure, invalid move, invalid book, unexpected
exception) to come back as a consistent, predictable shape
**So that** the frontend can render errors uniformly and the backend never leaks stack
traces or ambiguous 500s for things that are actually normal business outcomes (like an
invalid book upload)

### Trade-off

| Option | Pros | Cons |
|---|---|---|
| **`@ControllerAdvice` / `GlobalExceptionHandler`** (one place maps domain exceptions → HTTP status + body) | Single source of truth for error shape; easy to test; keeps controllers thin | Requires discipline to always throw typed domain exceptions instead of ad-hoc responses |
| Per-controller try/catch | No upfront design needed | Inconsistent shapes across endpoints; duplicated logic; easy to forget a case |
| Let Spring's default error handling show through | Zero effort | Leaks stack traces/internal detail; inconsistent for API consumers; looks unfinished |

**Decision: do it now, as part of Objective 1-2 groundwork.** This isn't really
"extra" — the brief's own validity rules (US-01) and the `POST /api/books` 400 response
(API contract) only work cleanly if there's a consistent error-handling mechanism from
the start. Cheap to build early, expensive to retrofit once every controller has its
own ad-hoc error handling.

---

## NFR-02 — Containerize the app (Docker)

**As a** developer/reviewer running this project
**I want** to start the whole app (backend + frontend) with one command, in a
reproducible environment
**So that** "how to build and run" in the README is bulletproof regardless of what's
installed on the reviewer's machine

### Trade-off

| Option | Pros | Cons |
|---|---|---|
| **Docker (Dockerfile per service + docker-compose)** | Reproducible; matches how real teams ship Spring Boot + Angular; demonstrates ops maturity | Extra time cost; another thing that can break under time pressure; not required by the brief |
| No containerization, just document local `mvn`/`npm` commands | Fastest; brief only asks for build/run instructions, not Docker | Reviewer needs matching local toolchain (Java 21, Node 26, Maven) installed correctly |

**Decision: later / stretch, after Objectives 1-5 are solid.** The brief doesn't
require it, and a broken Dockerfile under time pressure is worse than a clean README
with plain `mvn`/`npm` instructions. Revisit only once the core app works end-to-end
and there's time left.

---

## NFR-03 — Security: token-based protection of the API

**As an** operator of this API
**I want** the `/api/**` endpoints to require some form of token before accepting
requests
**So that** the API isn't wide open to anyone who can reach the host

### Trade-off

| Option | Pros | Cons |
|---|---|---|
| **Static API key / shared secret header** (e.g. `X-API-Key`, checked by a simple filter) | Very cheap to add; stops casual/accidental access; no user concept needed | Not real authentication — one shared secret, no per-user identity, easy to leak |
| **Full JWT auth** (see NFR-04) | Real per-user identity, foundation for per-player save data | Meaningfully more work: login endpoint, token issuance/validation, Spring Security config |
| Nothing (open API) | Zero effort, matches "single local player" scope of the brief | Not production-appropriate; fine only because this is a local technical exercise, not a deployed service |

**Decision: deferred — "nothing" is acceptable for now, since the brief describes a
single local player with no accounts.** If time allows, a static API key is the
cheapest meaningful step up; full JWT (NFR-04) is only worth it if we also want
per-player saves (see NFR-04's note).

---

## NFR-04 — Auth: JWT (real user identity)

**As a** Player
**I want** to log in and have my saved progress tied to *me*
**So that** multiple people can use the same deployed instance without overwriting
each other's saves

### Trade-off

| Option | Pros | Cons |
|---|---|---|
| **JWT (login endpoint issues a signed token, Spring Security filter validates it on `/api/**`)** | Real multi-user support; standard, well-documented pattern in Spring Boot; natural key for `progress` rows (`userId` + `bookId`) | Needs user storage (even a minimal one), password handling, token expiry/refresh decisions — a few hours of work on its own |
| Session cookies instead of JWT | Simpler mental model, no token storage on client | Less natural for a SPA talking to a separate API origin; more server-side session state (goes against our stateless `/play` design) |
| Skip entirely, single implicit "local player" | Matches the brief's actual scope (no mention of multiple accounts) | `/progress` can only ever have one save per book, globally — fine for this exercise, but is the ceiling of NFR-03's "nothing" option |

**Decision: deferred, lowest priority of the three security-adjacent items.** Only
pick this up if NFR-03 (basic token gate) is already done *and* there's a real reason
to support multiple players — the brief's objectives never ask for multi-user support.
If done, JWT is the right choice over cookies (fits the stateless design in
`03-technical-architecture.md`).

---

## NFR-05 — Monolith vs. microservices

**As the** engineering team
**I want** to decide the deployment granularity up front
**So that** we don't over- or under-engineer the backend for a 4-hour exercise

### Trade-off

| Option | Pros | Cons |
|---|---|---|
| **Single Spring Boot monolith** (current plan) | Matches project scope/size; one `pom.xml`, one deploy unit, no network calls between "services"; far faster to build and reason about | Doesn't demonstrate distributed-systems skills; everything scales/deploys together |
| Split into services (e.g. `book-service`, `game-service`, `progress-service`) | Looks more "enterprise"; independent scaling/deploy | Massive overkill for this domain size; adds network calls, service discovery, inter-service contracts, more Docker complexity — directly fights the 4-hour budget and "quality over quantity" tip in the brief |

**Decision: monolith, now, not revisited unless explicitly requested.** The ECB
package split (`boundary`/`control`/`entity` in `03-technical-architecture.md`) already
gives us clean internal seams — if a real need to extract a service ever appeared,
those boundaries make it possible later, but there's no functional or scale reason to
pay the microservices tax for this exercise.

---

## NFR-06 — Store book JSON in the database instead of flat files

**As the** engineering team
**I want** to confirm whether books should live in H2 instead of `data/books/valid/`
**So that** we don't carry two half-solutions (files *and* DB) for the same content

### Trade-off

| Option | Pros | Cons |
|---|---|---|
| **Flat files** (`incoming/valid/invalid`, current decision in `03-technical-architecture.md`) | Matches the brief's own delivery format (books *are* JSON files); trivial to inspect/diff/version; no schema migration for book structure changes; plays well with the `.errors.txt` reporting requirement | Harder to query "by tag" efficiently at large catalog sizes (not a real concern at 4 sample books); no transactional guarantees across files |
| **Store book JSON in H2** (e.g. a `books` table with a JSON/CLOB column, or normalized tables for sections/options) | One persistence technology for everything; easier SQL-based search/filter at scale | Have to re-decide the on-disk `valid/invalid` + `.errors.txt` requirement's meaning (do we still write files, or does "invalid" become a DB flag + error column?); normalizing sections/options into tables is real modeling effort for no real payoff at this scale; migrating away from the provided file format adds risk for little benefit in a 4-hour build |

**Decision: keep flat files for book content now; this is not purely deferred, it's
a confirmed choice** (matches NFR-05's "don't over-engineer" spirit) — only revisit if
the catalog genuinely grows large enough that file-based search/filter becomes a
measurable problem, which isn't expected for this exercise. H2 stays scoped to what it's
already assigned in `03-technical-architecture.md`: **saved progress only**, never book
content.

---

## Grouping (for later sequencing)

| Group | Items | Why grouped together |
|---|---|---|
| **A — Do now (foundational)** | NFR-01 (exception handling) | Cheap now, expensive later; directly supports Objective 1-3 API quality |
| **B — Confirmed architectural decisions (no build work, just locked in)** | NFR-05 (monolith), NFR-06 (flat-file books) | Not tasks to schedule — decisions already made and documented; revisit trigger is explicit (real scale need), not "later in this project" |
| **C — Stretch, after Objectives 1-5** | NFR-02 (Docker) | Pure infra polish, no functional risk if skipped, same bucket as `03-technical-architecture.md`'s existing backlog |
| **D — Stretch, security, lowest priority, sequential dependency** | NFR-03 (token gate) → NFR-04 (JWT) | NFR-04 only makes sense after NFR-03, and both only matter if multi-user/production-exposure becomes a real goal — neither is implied by the brief's objectives |

Next step once you're ready: fold **Group A** into the actual Objective 1 implementation
work, and place **Groups C/D** at the end of the sequencing plan, after Objectives 1-5 —
exact order within C/D (Docker before or after security) still to be decided when we
get there.
