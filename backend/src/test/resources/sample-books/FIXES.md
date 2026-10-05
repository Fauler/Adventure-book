# Fixes Applied to the Provided Book JSON Fixtures

This document explains, proactively and transparently, the exact changes made to turn the
originally-provided book JSON fixtures (`backend/src/test/resources/sample-books/*.json`)
into valid, playable books (`*_FIXED.json`, same folder). No narrative content was
invented — every fix below either removes a clearly test-only artifact or corrects an
evident typo, using only text that was already present in the provided files.

All three fixed files were verified against `Book.validate()` (the four structural
rules) by running them through the real backend loader, both via the full ingestion
pipeline (`backend/data/books/incoming` &rarr; `valid`/`invalid`) and the automated test
suite (`BookFileAdapterTest`); each now loads successfully into the catalog.

## Why fixes were needed

The originally-provided fixtures are intentionally used by the test suite to exercise
the validator, so some of them contain deliberately broken structures. To use their real
story content as actual in-app sample books, those deliberate breaks needed to be
resolved first — each resolution is recorded below.

## `the-prisoner.json` &rarr; `the-prisoner_FIXED.json`

- **Problem:** an extra section with `id: "666"` (text: *"Some unreachable node"*), type
  `NODE`, with no `options` — violates "every non-END section must have at least one
  option". It is a self-admitted, unreachable artifact: no option in the file has
  `gotoId: 666`.
- **Fix:** removed the section entirely. Nothing else in the file referenced it, so no
  other section or option needed to change.

## `crystal-caverns.json` &rarr; `crystal-caverns_FIXED.json`

- **Problem:** a section with `id: 666` (text: *"An unreachable dead end. You shouldn't
  be here."*), type `NODE`, with no options — same structural violation as above. Unlike
  `the-prisoner.json`, this one **is** reachable: section 900's option "Investigate the
  movement" points to it (`gotoId: 666`).
- **Fix:** changed the section's `type` from `NODE` to `END`, keeping its existing text
  unchanged. Since the text already reads as a dead-end/bad-outcome, this reclassifies
  it as a (deliberately bad) ending rather than inventing new narrative — choosing
  "Investigate the movement" now ends the playthrough there instead of leading to a
  structurally invalid dead node.

## `pirates-jade-sea.json` &rarr; `pirates-jade-sea_FIXED.json`

Two separate problems were found in this file:

1. **Same `666` trap pattern** as above (text: *"An unreachable whirlpool drags you into
   darkness. You shouldn't be here."*), reachable from section 1's "Take the helm and
   steer toward the Isle of Serpents" and from section 600's "Swim closer to
   investigate".
   - **Fix:** same approach — changed `type` from `NODE` to `END`, text unchanged.
2. **Dangling reference:** section 1's option "Interrogate the captured pirate in the
   brig" points to `gotoId: 999`, which does not exist anywhere in the file.
   - **Investigation:** section `20`'s text is *"The captured pirate snarls but whispers
     about a secret cove guarded by a sea monster."* — a direct narrative match for
     "interrogating the captured pirate" — and section 20 was itself unreferenced by any
     option in the original file (an orphan). This strongly suggested `999` was a typo
     for `20`.
   - **Fix:** changed the option's `gotoId` from `999` to `20`. This was confirmed with
     the user before applying (not assumed), since it affects the story graph (which
     path the player reaches).

## `dragon-quest.json` — no fixed version

The provided file is genuinely empty (0 bytes). There is no content to infer a fix
from, so no `_FIXED.json` variant was created for it — inventing narrative content
would go beyond "fixing structural/typo issues" into fabricating story data, which this
exercise deliberately avoids.

## How this was verified end-to-end

All 4 original fixtures plus the 3 `_FIXED` variants were copied into
`backend/data/books/incoming/` and the real ingestion pipeline (`BookIngestionRunner` on
app startup) was run against them:

- `*_FIXED.json` (3 files) &rarr; moved to `backend/data/books/valid/` and served
  successfully by `GET /api/books`.
- The 4 original, still-broken files &rarr; moved to `backend/data/books/invalid/` with a
  sibling `<name>.json.errors.txt` recording the exact rule(s) that failed, e.g.:
  - `crystal-caverns.json.errors.txt`: `section 666 is a NODE with no options`
  - `pirates-jade-sea.json.errors.txt`: both the dangling-`gotoId` and the orphan-`NODE`
    errors, reported independently
  - `the-prisoner.json.errors.txt`: `section 666 is a NODE with no options`
  - `dragon-quest.json.errors.txt`: `book file is empty or not valid JSON: ...`

Both the original and fixed files are kept side by side in `incoming/` intentionally —
it demonstrates the pipeline correctly discriminating between still-invalid and
now-valid input from the same source material, rather than only ever seeing "clean"
data.
