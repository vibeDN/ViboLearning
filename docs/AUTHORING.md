# Authoring a course (with or without an AI)

ViboLearning is a **runtime for learning content**, not an AI product. A course is
one JSON file in the [`vibolearning/v1`](../schema/vibolearning-v1.schema.json)
format; any model — or a human — can produce it, and the app just renders it.

The intended flow:

```
source material (docs, a spec, lecture notes, a cheat sheet)
        │
        ▼   any LLM  (ChatGPT / Claude / Gemini / a local model)
course.json  (vibolearning/v1)
        │
        ▼   import by URL or file
📖  interactive course — path, lessons, code, quizzes, checkpoints
```

## Prompt to hand an LLM

Paste the schema and one reference course, then the prompt below.

- Schema: [`schema/vibolearning-v1.schema.json`](../schema/vibolearning-v1.schema.json)
- Reference course: [`app/src/main/assets/courses/cpp-basics.json`](../app/src/main/assets/courses/cpp-basics.json)
  (also mirrored at `https://vibedn.github.io/ViboLearning/courses/cpp-basics.json`)

> You are turning source material into a **ViboLearning course** (`vibolearning/v1`).
> Output **only** a single valid JSON document that conforms to the attached JSON Schema — no prose, no markdown fences.
>
> Rules:
> - `schema` is exactly `"vibolearning/v1"`. Give the course a stable lowercase `id`.
> - Split the material into 4–10 `modules`, each 2–5 `lessons`. Order matters: earlier lessons are prerequisites for later ones.
> - Every lesson is a list of `blocks` (cards the learner swipes through):
>   - `text` — one idea, 1–3 sentences. Use `**bold**` and `` `code` `` inline. Add a short `code` (and `output`) field whenever an example makes the idea concrete.
>   - `code` — a runnable sample. Always fill `output` with the exact stdout; keep samples short and self-contained.
>   - `quiz` — one question. Prefer `single`; use `multiple`, `type`, `order` when they fit. Put any referenced code in the question's `code` field, because the learner sees each card in isolation. Add an `explanation`.
>   - `practice` — 2–4 `terms` ({display, meaning}) for a matching drill. Use it 2–3 times per module for the key vocabulary.
> - End **every** module with an `exam`: 4–6 `questions`, `passScore` about 60% of them.
> - Set `answer` per variant: `single` → integer index; `multiple` → array of indices; `type` → array of accepted strings (case-insensitive); `order` → array of `items` indices in the right order.
> - Record where the material came from in `source: { title, url }`.
> - Language: write the course in the same language as the source material.
>
> Source material follows:
> ```
> <PASTE DOCS HERE>
> ```

## Validate before importing

```bash
# any JSON Schema validator, e.g. ajv-cli
npx ajv-cli validate -s schema/vibolearning-v1.schema.json -d your-course.json --spec=draft2020

# or open the studio (paste JSON → see errors + a live preview)
#   local:  web/studio.html
#   hosted: https://vibedn.github.io/ViboLearning/studio.html
```

The runtime also validates structurally on import and shows a plain-language
error if something is off (missing `name`, an exam with no questions, a `practice`
block with fewer than two terms, an unknown `variant`, …).

## Hosting a course

Anything that serves the raw JSON with permissive CORS works:

- **GitHub** — commit `course.json`, import `https://raw.githubusercontent.com/<user>/<repo>/<branch>/course.json` (the app also accepts the `github.com/.../blob/...` link and rewrites it).
- **GitHub Pages / any static host** — `https://<you>.github.io/<repo>/course.json`.
- **A gist**, an S3 object, a pastebin raw link — all fine.

Re-importing a course with the same `id` refreshes its content and keeps the
learner's progress (progress is keyed on course / module / lesson ids).

## Full field reference

See [`course-schema.md`](course-schema.md).
