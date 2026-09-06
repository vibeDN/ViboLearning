# ViboLearning

A **model-agnostic runtime for learning content**. A course is one JSON file
([`vibolearning/v1`](schema/vibolearning-v1.schema.json)); the app renders it as
an interactive course — a path of lesson nodes, swipeable cards (prose with inline
examples, editable/runnable code, quizzes), inline practice, per-module
checkpoints, hearts, XP and streaks.

There is **no AI at runtime**. Any model — ChatGPT, Claude, Gemini, a local model —
or a human can turn source material into a course JSON; the app just plays it.
See [`docs/AUTHORING.md`](docs/AUTHORING.md).

```
source material ──▶ any LLM ──▶ course.json (vibolearning/v1) ──▶ import ──▶ 📖 course
```

- **Android client** — Kotlin + Jetpack Compose. `app/`.
- **Web app** — one self-contained file, `web/index.html`. Same format, same design
  (Nocturne). Offline (service worker), installable to the iOS home screen.
  Online code-run via wandbox.org with a canned `output` fallback.
  Live: **https://vibedn.github.io/ViboLearning/**
- **Format** — [`schema/vibolearning-v1.schema.json`](schema/vibolearning-v1.schema.json) (authority) · [`docs/course-schema.md`](docs/course-schema.md) (prose).

## Status

MVP. Bundled courses: **«C++ с азов»** (4 modules) and **«Язык C»** (10 modules,
30 lessons), each with per-module checkpoint exams. Import more by URL, file, or a
`?import=<url>` link; share a course with `?course=<id>`; move progress between
devices via export/import in the course list.

## Web app

`web/index.html` runs standalone (open the file) or hosted (GitHub Pages: point
Pages at `/web`). No build step. `web/courses/cpp-basics.json` is the same course
the Android app bundles; the HTML inlines a copy and prefers the file when hosted.

## Build

No Android toolchain is set up on the dev machine used to scaffold this — build
it in Android Studio (Ladybug+) or with a local SDK:

```
# needs JDK 17 + Android SDK (compileSdk 35)
./gradlew :app:assembleDebug
```

If the Gradle wrapper jar is missing, run `gradle wrapper --gradle-version 8.11.1`
once (or just open the project in Android Studio, which regenerates it).

## Architecture

```
data/
  model/Course.kt        course → modules → lessons → blocks; Exam; Question
  CourseJson.kt           parse + validate + normalize (fills ids, sorts by `order`)
  CourseRepository.kt      built-in (assets) + imported courses, progress, XP
  local/Db.kt              Room: courses (raw JSON + meta), lesson_progress
  remote/CourseDownloader  import-by-URL (normalizes github blob links)
  run/CodeRunner.kt        WandboxRunner (online compile) → CannedRunner fallback
engine/
  Cards.kt                 SessionBuilder: blocks/exam/practice → List<Card>
  Answers.kt               grades the flexible Question.answer field
ui/
  theme/                   Nocturne design tokens, per-course accent
  home/                    the path + practice tab + course library sheet
  session/                 the running session (SessionViewModel + SessionScreen)
  result/                  complete / failed screens
```

Progress is keyed on course/module/lesson **ids**. Re-importing a newer version
of a course keeps progress as long as ids (or positions) are stable.

## Design

Ported from the "Nocturne" design system + the SoloLearn-style prototype in
[`design/reference/`](design/reference/) (de-branded). Dark-only, one accent used
as a line and a glow; the accent is per-course (`"accent": "#RRGGBB"`).

## Adding a course

Write a JSON file per [`docs/course-schema.md`](docs/course-schema.md), then
either drop it in `app/src/main/assets/courses/` (bundled) or import it at runtime
by URL / file. The bundled [`cpp-basics.json`](app/src/main/assets/courses/cpp-basics.json)
is the reference for every block type.
