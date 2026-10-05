# Repository instructions

## Project docs (source of truth)

All business/technical decisions for Adventure Book live in `docs/`, in numbered order
(`00-PARKING_LOT.md`, `01-challenge-understanding.md`, `02-user-stories.md`,
`03-technical-architecture.md`, `04-non-functional-requirements.md`,
`05-business-architecture.md`). Read the relevant doc before making a non-trivial decision
— don't re-derive or contradict what's already decided there. If something is genuinely
undecided, add it to `docs/00-PARKING_LOT.md` rather than guessing.

Path-scoped rules: `.github/instructions/backend.instructions.md` (`backend/**`) and
`.github/instructions/frontend.instructions.md` (`frontend/**`).

Core cross-cutting guardrails (details in the docs above):

- The backend is the single source of truth; the frontend never duplicates business/
  validation/HP logic.
- Objectives are implemented in order (see `docs/01-challenge-understanding.md`).

## Git

- Never run any `git` command (status checks for your own context are fine,
  but `git add`, `git mv`, `git rm`, `git commit`, `git push`, `git checkout`,
  branch/merge/rebase operations, etc. are not). Staging, committing, and all
  other git operations are always manual, explicit actions performed by the
  user themselves.

