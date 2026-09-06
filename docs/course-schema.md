# ViboLearning course format — `vibolearning/v1`

A course is one JSON file. The Android app is a **renderer**: whatever the file
declares is what the learner sees. No backend, no build step — write JSON, import
it (by URL or from a file), done.

The bundled example is [`../app/src/main/assets/courses/cpp-basics.json`](../app/src/main/assets/courses/cpp-basics.json).

## Top level

```jsonc
{
  "schema": "vibolearning/v1",        // required, must start with "vibolearning/"
  "id": "cpp-basics",                 // optional — derived from name if omitted; keep it stable, progress is keyed on it
  "name": "C++ с азов",               // required
  "iconUrl": "https://…/icon.png",    // optional
  "accent": "#5AC8E6",                // optional accent color (#RRGGBB); falls back to the app default
  "description": "Самые основы",      // optional, shown under the title
  "author": "…",                     // optional
  "version": 1,                       // bump on re-publish; re-import refreshes content and keeps progress
  "startingHearts": 5,                // lives per lesson / exam attempt
  "source": {                         // optional provenance / attribution
    "title": "cppreference — Basic concepts",
    "url": "https://en.cppreference.com/w/cpp/language/basic_concepts"
  },
  "modules": [ /* Module[] */ ]        // required, ≥1
}
```

A machine-readable [JSON Schema](../schema/vibolearning-v1.schema.json) is the
source of truth; see [`AUTHORING.md`](AUTHORING.md) for generating a course with an LLM.

## Module

```jsonc
{
  "name": "Основные понятия",   // required
  "order": 1,                    // optional; sorts modules. Omit → file order
  "practice": true,              // show a "practice" node after the lessons (default true)
  "lessons": [ /* Lesson[] */ ], // required, ≥1
  "exam": { /* Exam */ }         // optional end-of-module checkpoint
}
```

On the path a module renders as: a header, its lesson nodes, an optional
**practice** node (enabled once every lesson is done), then the **exam** node.
The next module unlocks only when this one is cleared (all lessons + exam passed).

## Exam — "контрольные вопросы"

```jsonc
{
  "title": "Контрольные: основы",
  "passScore": 3,     // min correct answers to pass; 0 = just finish with a heart left
  "xp": 30,
  "questions": [ /* Question[] */ ]   // required, ≥1
}
```

## Lesson

```jsonc
{
  "name": "Первая программа",  // required
  "order": 1,                   // optional; sorts lessons within the module
  "xp": 10,                     // awarded on completion
  "blocks": [ /* Block[] */ ]   // required, ≥1 — the cards the learner swipes through
}
```

A lesson is complete when the learner reaches the end. Wrong quiz answers cost a
heart; running out ends the attempt (retry from the start).

## Blocks

Every block has a `type`.

### `text`
Prose. Inline markdown: `**bold**`, `*italic*` / `_italic_`, `` `code` ``.
Optional `code` / `output` show a short illustrative snippet on the same card
(not runnable — use a `code` block for that). Pair almost every concept card
with an example.
```jsonc
{
  "type": "text",
  "text": "Переменная — именованная ячейка памяти. Формат: `тип имя = значение;`",
  "code": "int age = 18;\ncout << age;",   // optional
  "output": "18"                            // optional
}
```

### `code`
A code sample. `editable` shows an editor; `runnable` adds a **Запустить** button
that compiles online (wandbox.org) and falls back to `output` when offline.
```jsonc
{
  "type": "code",
  "language": "cpp",           // cpp | c | python
  "editable": true,
  "runnable": true,
  "caption": "Поменяй текст в кавычках и запусти",
  "code": "#include <iostream>\nint main(){ std::cout << \"Hi\"; }",
  "output": "Hi"               // shown when no compiler is reachable
}
```

### `quiz`
One graded question shown as its own card. `variant` picks the interaction:

| variant    | `answer` shape        | extra fields |
|------------|-----------------------|--------------|
| `single`   | `1` (0-based index)   | `options` (≥2) |
| `multiple` | `[0, 2]`              | `options` (≥2) |
| `type`     | `["cout", "std::cout"]` — accepted strings, case-insensitive | — |
| `order`    | `[0, 1, 2]` — `items` in correct order | `items` (≥2) |

```jsonc
{
  "type": "quiz",
  "variant": "single",
  "prompt": "С какой функции начинается программа?",
  "code": "",                  // optional code shown above the question
  "options": ["start()", "main()", "run()"],
  "answer": 1,
  "explanation": "Точка входа — main()."
}
```

### `practice`
Inline drill. The engine auto-builds a matching card (+ multiple choice if there
are more than four terms) from a term list.
```jsonc
{
  "type": "practice",
  "prompt": "Сопоставь тип и то, что он хранит",
  "terms": [
    { "display": "int",    "meaning": "Целые числа" },
    { "display": "double", "meaning": "Числа с дробной частью" }
  ]
}
```
Terms from every `practice` block also feed the module's practice node and the
app-wide **Практика** tab.

## Questions (in an `exam`)

Same shape as a `quiz` block, minus the `"type": "quiz"` line:
```jsonc
{ "variant": "single", "prompt": "…", "options": ["…"], "answer": 0, "explanation": "…" }
```

## What import checks

- `schema` starts with `vibolearning/`
- course has `name` and ≥1 module; every module has ≥1 lesson; every lesson ≥1 block
- each question has a non-empty `prompt` and a well-formed `answer` for its `variant`
- `practice` blocks have ≥2 terms

Unknown fields are ignored, so newer files still open in an older app.
