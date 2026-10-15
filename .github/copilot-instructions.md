# Repository instructions

## Project docs (source of truth)

All business/technical decisions for Adventure Book live in `docs/`, in numbered order
(`00-PARKING_LOT.md`, `01-challenge-understanding.md`, `02-user-stories.md`,
`03-technical-architecture.md`, `04-non-functional-requirements.md`,
`05-business-architecture.md`, `06-implementation-plan.md`). Read the relevant doc before making a non-trivial decision
— don't re-derive or contradict what's already decided there. If something is genuinely
undecided, add it to `docs/00-PARKING_LOT.md` rather than guessing.

Path-scoped rules: `.github/instructions/backend.instructions.md` (`backend/**`) and
`.github/instructions/frontend.instructions.md` (`frontend/**`).

Core cross-cutting guardrails (details in the docs above):

- The backend is the single source of truth; the frontend never duplicates business/
  validation/HP logic.
- Objectives are implemented in order (see `docs/01-challenge-understanding.md`); the
  current work queue and per-milestone STATUS live in `docs/06-implementation-plan.md`.

## Keep instructions up to date

These instructions, the skills in `.github/skills/` and the docs they point to must
always match reality. Whenever a change you make (or a decision the user takes) makes
any of them stale, update them **in the same task**, without being asked:

- Finishing a milestone → update its STATUS in `docs/06-implementation-plan.md`.
- A decision that changes an architecture/business rule → update the affected `docs/`
  file and any rule repeated in `.github/instructions/*.md` or `.github/skills/*`.
- Starting or finishing work that makes an "out of scope" or "stateless" statement in
  the instruction files false → fix the statement in the same change.
- A new open question → `docs/00-PARKING_LOT.md` (in English, as documentation).

### Documentation sync checklist (any code or config change)

Documentation is part of the change, not a follow-up. Before finishing, update whatever
the change touched:

- API (endpoint, DTO, status code, `ErrorCode`) → API contract and "Error contract" in
  `docs/03-technical-architecture.md`; Swagger annotations if relevant.
- Package/class moves, new layers or dependencies → package tree and diagrams in
  `docs/03-technical-architecture.md`, and the architecture rules in
  `.github/instructions/*.md`.
- Business rule or behavior (validation, HP, save/resume, add-book) →
  `docs/05-business-architecture.md` and the affected user story in `docs/02-user-stories.md`.
- Run/build/test commands, ports, Docker or env changes → `README.md` and the Docker
  notes in `docs/03-technical-architecture.md`.
- Parking-lot item resolved, decided or newly opened → its status marker in the
  heading (`✅ Resolved`, `🟡 Partially resolved`, `🚧 Open`) plus a resolution note.
- New or changed tests/tooling → keep the README "Tests" section accurate.
- Remove or rewrite statements that became false (e.g. "later", "stretch", "not
  implemented"), rather than only adding new text.

Before finishing a task, check whether any file under `.github/` now contradicts the
code or docs, and mention in the final report what was updated.

## Git

- Never run any `git` command (status checks for your own context are fine,
  but `git add`, `git mv`, `git rm`, `git commit`, `git push`, `git checkout`,
  branch/merge/rebase operations, etc. are not). Staging, committing, and all
  other git operations are always manual, explicit actions performed by the
  user themselves.

