---
name: implement-next
description: Picks up the next pending objective/user story for Adventure Book, checks the relevant architecture/business docs, implements it, and logs any newly discovered open question to the parking lot instead of guessing. Use when asked to "implement the next thing", "pick up where we left off", or "continue the build".
---

You are implementing the Adventure Book technical assessment incrementally, one objective
at a time, strictly following the project's own documentation rather than improvising.

## Process

1. **Find the next pending objective.** Check `docs/01-challenge-understanding.md` for the
   ordered objectives (1 → 5) and `docs/02-user-stories.md` for the user stories backing
   them. Objectives must be treated in order — don't start objective N+1 work while N is
   incomplete, unless explicitly told otherwise.
2. **Cross-check the relevant docs before writing code**:
   - `docs/03-technical-architecture.md` for stack, ECB package layout, API contract,
     validation/logging conventions, frontend routes.
   - `docs/04-non-functional-requirements.md` for what's in/out of scope right now (e.g.
     auth, Docker, DB-for-books are explicitly deferred).
   - `docs/05-business-architecture.md` for exact business-rule wording (validation rules,
     game-end states, save/resume, add-book flow, file naming).
   - `docs/00-PARKING_LOT.md` for already-known open questions that might affect this piece
     of work.
3. **Implement**, following `.github/instructions/backend.instructions.md` and
   `.github/instructions/frontend.instructions.md` for anything under `backend/**` or
   `frontend/**` respectively.
4. **When you hit an ambiguous design question not answered by the docs**, do not silently
   assume an answer. Add a new, dated entry to `docs/00-PARKING_LOT.md` using its existing
   template, then proceed with the most reasonable interim assumption, clearly called out
   as such.
5. **Never run any `git` command** (see `.github/copilot-instructions.md`) — leave staging
   and committing to the user. You may propose a commit message once a meaningful chunk of
   work is done, but never execute it.
6. **Report back concisely**: what objective/story was implemented, which docs it followed,
   any parking-lot entries added, and what the logical next objective would be.
