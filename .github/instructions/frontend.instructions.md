---
applyTo: "frontend/**"
description: Frontend (Angular) implementation rules for Adventure Book
---

# Frontend instructions

Source of truth for all decisions below: `docs/03-technical-architecture.md` (frontend
module layout, routes, backend↔frontend communication) and `docs/02-user-stories.md`
(expected UX flows/acceptance criteria). Read the relevant section before implementing
anything non-trivial; do not re-derive flows from scratch, and do not silently contradict
them. If a decision is genuinely missing, add an entry to `docs/00-PARKING_LOT.md` instead
of guessing.

## Core principle

- **The frontend is dumb by design.** The backend is the single source of truth for all
  game rules, validation, and HP/state logic. Never re-implement or duplicate business
  logic client-side (no local HP math, no local re-validation of book rules, no inferring
  game-end state from anything other than what the backend returns). The frontend's job is
  to render backend responses and forward raw user input.
- Only exception: the add-a-book flow's *JSON syntax* check (not business-rule validation —
  that's still re-applied by the backend) — see `docs/05-business-architecture.md`.

## Structure

- Keep a thin `core/` layer of API services that mirror the backend contract 1:1 (one
  method per endpoint, typed request/response matching `docs/03-technical-architecture.md`'s
  API contract table). Don't add client-side transformation beyond what's needed to bind to
  the view.
- Routes must match the routes table in `docs/03-technical-architecture.md`. Don't add or
  rename routes without updating that doc first.

## Styling

- Plain SCSS only, as scaffolded by `ng new` — no UI component library/CSS framework
  unless the technical architecture doc is updated to say otherwise.

## State handling

- Treat every `/play`-style call as stateless: send the current state the backend gave you,
  render exactly what comes back. Don't cache or locally mutate health/section/status
  between calls.
- Save/resume, pause/stop behavior must follow `docs/02-user-stories.md` (US-08, US-09) —
  check the current phase (pre- or post-Save implementation) described there before building
  stop/pause UI.

## Error handling

- Surface backend validation/rejection messages verbatim (e.g. add-book rejection reasons,
  one per line) — never invent or reword backend error text.
