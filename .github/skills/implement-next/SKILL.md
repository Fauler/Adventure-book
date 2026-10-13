---
name: implement-next
description: Picks up the next pending objective/user story for Adventure Book, checks the relevant architecture/business docs, implements it, and logs any newly discovered open question to the parking lot instead of guessing. Use when asked to "implement the next thing", "pick up where we left off", or "continue the build".
---

You are implementing the Adventure Book technical assessment incrementally, one objective
at a time, strictly following the project's own documentation rather than improvising.

## Process

1. **Find the next pending milestone.** Open `docs/06-implementation-plan.md` and take
   the first milestone whose STATUS is "To do" (candidates marked "candidate" only when
   the user asks for them). Use `docs/01-challenge-understanding.md` (objectives 1 → 5,
   in order) and `docs/02-user-stories.md` for the business context. Don't start
   milestone N+1 while N is incomplete, unless explicitly told otherwise.
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
6. **Keep everything up to date.** Set the milestone's STATUS in
   `docs/06-implementation-plan.md`, update any doc or `.github/` instruction/skill that
   the change made stale (see "Keep instructions up to date" in
   `.github/copilot-instructions.md`).
7. **Report back concisely**: what milestone/story was implemented, which docs it followed,
   any parking-lot entries added, which docs/instructions were updated, and the next milestone.
