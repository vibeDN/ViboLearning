# ViboLearning

A SoloLearn-style learning app that is a **pure renderer**: you write a course as
one JSON file, the app turns it into an interactive course — a path of lesson
nodes, swipeable lesson cards (prose, editable/runnable code, quizzes), inline
practice, per-module checkpoints, hearts, XP and streaks.

- **Android client** — Kotlin + Jetpack Compose. `app/`.
- **Web app** — one self-contained file, `web/index.html`. Same JSON format, same
  design (Nocturne). Works offline (service worker + inlined bundled course),
  installable to the iOS home screen. Online code-run via wandbox.org, canned
  `output` fallback.
- **Course format** — [`docs/course-schema.md`](docs/course-schema.md) (`vibolearning/v1`).

## Status

MVP. Bundled course: **«C++ с азов»** (4 modules, 12 lessons, per-module exams).
Courses can also be imported by URL or from a file.

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
