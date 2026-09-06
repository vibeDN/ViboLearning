# Format roadmap & design decisions

ViboLearning is a **model-agnostic runtime for learning content**. The format is
the product; renderers (Android, web) are interchangeable. Two rules keep it that
way:

1. **Semantic, not visual.** A block is `{ type, meaning }`. No `margin`, `width`,
   `color`, `radius` — ever. The renderer owns all layout, so the whole app's
   design can change without touching a single course.
2. **The JSON outlives the renderer.** An unknown block type must never brick a
   course (see *Forward compatibility*).

## Blocks

### Shipped
| type | fields | notes |
|---|---|---|
| `text` | `text`, `code?`, `output?` | prose + optional inline example |
| `code` | `code`, `language?`, `editable?`, `runnable?`, `output?`, `caption?` | one type covers read-only, editable, and runnable |
| `quiz` | `variant` (`single`/`multiple`/`type`/`order`), `prompt`, `code?`, `options?`/`items?`, `answer`, `explanation?` | the graded primitive |
| `practice` | `prompt?`, `terms[]` | auto-built matching drill |

### Batch F (in progress)
| type | fields |
|---|---|
| `heading` | `text`, `level?` (1–3) |
| `callout` | `variant` (`note`/`tip`/`warning`/`important`), `text` |
| `image` | `url`, `alt?`, `caption?` |
| `quote` | `text`, `cite?`, `url?` |

Plus: **forward-compatible parsing** — an unrecognised `type` renders its
`fallback` block if present, otherwise a small "not supported in this version"
placeholder. The import never fails on an unknown block.

### Next
- `table` — `headers[]`, `rows[][]`, `caption?`. Scrolls horizontally on narrow screens.
- `hints: [string]` on `quiz` questions — progressive reveal, covers the
  "accordion / why" need without a new type.

### Deferred (need real infrastructure, not just a renderer)
- **`exercise` container** with `evaluation: { type: "tests", tests: [...] }` —
  run unit tests against the learner's code. Worth doing *only* alongside a
  Wandbox test harness; until then `quiz` + `practice` are the exercise
  primitives and wrapping them buys nothing.
- **`tabs`** (same concept in C++ / Python / JS) — needs per-card tab state.
- **`terminal`** (shell command + expected output) — needs a shell runner; we
  only have a compiler (Wandbox) for compiled languages.

## Lesson kinds

Optional `kind` on a lesson changes only its path-node presentation (icon,
label) and whether hearts apply:

| kind | node | hearts |
|---|---|---|
| `lesson` (default) | play | yes |
| `reading` | book | no (pure text/callout, no quizzes) |
| `project` | wrench | yes |
| `challenge` | flame | yes, fewer |

Not yet implemented; drop-in when the path gets more visual variety.

## Localization

**Not** runtime machine translation — it wrecks technical terms, code comments
and `type`-quiz accepted strings, there is no reliable free CORS MT service, and
it breaks rule 1.

Localization is a **pipeline** concern:

- One course file per language: `c-lang` (`lang: "ru"`), `c-lang-en`
  (`lang: "en"`, optional `translationOf: "c-lang"`).
- The same LLM that generated the course translates it, with a fixed prompt:
  *keep every `code`/`output` verbatim; keep all `answer` indices; translate
  `type` accepted strings only when they are natural language, not code; set
  `lang` and a new `id`.*
- The studio will offer a one-click "generate translation prompt".
- The library groups variants of the same `translationOf` under one entry.

## Forward compatibility

```jsonc
{
  "type": "interactive_diagram",     // renderer from 2027, unknown to this app
  "fallback": {                      // ← rendered instead, on any older runtime
    "type": "image",
    "url": "https://…/diagram.png",
    "caption": "State machine"
  }
}
```

- Known `type` → validated strictly against the schema.
- Unknown `type` with `fallback` → the fallback block is rendered.
- Unknown `type` without `fallback` → a muted "this block needs a newer app"
  card. The rest of the lesson works.

## Authoring pipeline

`docs/AUTHORING.md` — feed an LLM the schema + a reference course + the source
material; it emits a `vibolearning/v1` document. `web/studio.html` (planned)
validates it against the schema and previews it as a real course.
