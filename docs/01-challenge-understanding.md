# 01 - Challenge Understanding

## Context

Technical interview exercise. Expected duration: 4 hours. Solutions copied/downloaded
from the internet disqualify the candidate.

## Goal

Develop an interactive adventure book application:
- **Backend**: Java + Spring Boot + Maven
- **Frontend**: Angular
- Dependencies may be used freely from Maven Central.
- A **README** with clear build/run instructions is required.

## Subject

An adventure book is composed of small, numbered **sections**. At the end of each
section the reader/player picks one of several **options**, which leads to another
section, and so on until an **ending** section is reached.

The application must let a user:
- View the list of available books
- Search and filter books
- Play a book as a game (succeed/fail)

### Book validity rules

A book is **invalid** if any of the following is true:
- It has none, or more than one, `BEGIN` (beginning) section
- It has no `END` (ending) section (it can have multiple endings)
- Any option has a `gotoId` that does not match an existing section id
- A non-ending section (`NODE`) has no options

### Game mechanics

- The header shows: stop/pause control, current book name, player life (health), and
  a way to save progression.
- The main content area shows the current section's text and the list of choices.
- Choosing an option can move the player to the next section, possibly applying a
  **consequence** (e.g. losing health points from combat, falling, etc.).
- The player starts with **10 health points**. At 0 HP the player dies and the game
  ends (in addition to reaching a normal `END` section).

## Objectives (to be implemented **in order**)

1. **Objective 1**: Home page presenting the game, listing all books, with search and filter.
2. **Objective 2**: Start a game and execute basic interactions (no consequences yet).
3. **Objective 3**: Handle consequences, player health points, and game end (death or
   normal ending).
4. **Objective 4 (extra)**: Save the user's progression in a book.
5. **Objective 5 (extra)**: Add new books functionality (implementation free to decide).

Tip from the exercise brief: focus on one objective at a time; it's fine not to finish
everything — **quality over quantity**.

## Provided design (mockups, extracted from the PDF)

- **Main screen** ("Adventure Awaits"): hero banner, "Adventure Library" section with a
  search box, filter pills (genre tags + difficulty: Easy/Medium/Hard), and a responsive
  grid of book cards. Each card shows title, author, short description, difficulty +
  genre tags, duration estimate, chapter count, theme tags, and a "Begin Quest" button.
- **Game screen**: header with "Back to Library", current book name badge, and
  "Save Progress" button. Main panel shows the section title, body text (multiple
  paragraphs), and a "What do you choose?" list of numbered option cards; an option can
  carry a requirement tag (e.g. "Requires: Strength").
  - Note: the mockup doesn't visibly show the HP indicator, but the spec text requires
    it in the header — we'll add it there.

## Sample data provided (`assessment-material/adventure-book-fullstack/books/`)

| File | Status | Notes |
|---|---|---|
| `crystal-caverns.json` | looks valid | used as the home-page mockup example |
| `pirates-jade-sea.json` | looks valid | |
| `the-prisoner.json` | **invalid** | contains section `666` (`NODE`) with no options — orphaned/unreachable node, violates the "non-ending section has no options" rule. Good edge case for validation logic. Also mixes numeric and string `id` types (`1`, `"500"`, `"1000"`). |
| `dragon-quest.json` | **invalid / corrupt** | file is 0 bytes (empty). Good edge case for "book fails to parse" handling. |

### Book JSON shape (from `the-prisoner.json`)

```json
{
  "title": "string",
  "author": "string",
  "difficulty": "HARD | MEDIUM | EASY",
  "sections": [
    {
      "id": 1,
      "text": "string",
      "type": "BEGIN | NODE | END",
      "options": [
        {
          "description": "string",
          "gotoId": 20,
          "consequence": {
            "type": "LOSE_HEALTH",
            "value": "6",
            "text": "string"
          }
        }
      ]
    }
  ]
}
```

Notes:
- `id` appears as both number and numeric string across the sample file — parsing must
  tolerate both.
- `options` and `consequence` are optional (absent on `END` sections, and on `NODE`
  sections without consequences).
- Only consequence type observed so far: `LOSE_HEALTH`. Others (e.g. combat, falling)
  are implied by the spec text but not shown in samples — we may need to design a
  small, extensible set of consequence types.

## Repository plan

- `backend/` — Spring Boot + Maven application
- `frontend/` — Angular application
- `doc/` — numbered markdown files tracing understanding, decisions, and progress
  made during this exercise (this file is `01-*`; next steps continue the sequence)
