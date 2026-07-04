# Building Tankobon from source

Tankobon is an Android app written in Kotlin (Jetpack Compose, Voyager navigation),
built with Gradle.

## Requirements

- **JDK 21** (see `.github/.java-version`).
- **Android SDK** (compile SDK 37; min SDK 26 / Android 8.0).
- **Android Studio** is the recommended IDE, but a command-line Gradle build works too.

> **No system JDK?** Android Studio ships its own JDK (JBR). Point `JAVA_HOME` at it before
> running Gradle, e.g. on macOS:
> ```bash
> export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
> ```

## Common commands

```bash
./gradlew assembleDebug          # build the debug APK (primary sanity check)
./gradlew testDebugUnitTest      # run unit tests
./gradlew spotlessCheck          # ktlint / spotless format check (CI-gating)
./gradlew spotlessApply          # auto-fix formatting + prune unused imports
./gradlew verifySqlDelightMigration   # validate DB migrations
```

The debug build installs as **`app.tankobon.dev`** (a `.dev` suffix), so it coexists with a
release install (`app.tankobon`).

## Code style

- `.editorconfig` governs formatting: 4-space indent for `.kt`/`.kts`/`.xml`, `max_line_length = 120`,
  final newline, trimmed trailing whitespace.
- Formatting is enforced by **ktlint via Spotless**. **Unused imports fail CI** — run
  `./gradlew spotlessApply` after removing code, since ktlint's unused-import rule is not always
  reliable here.
- Compose `@Composable` functions are exempt from function-naming rules.

## Continuous integration

CI (`.github/workflows/build.yml`) runs `spotlessCheck`, the unit tests, and a release build on
pull requests and on pushes to `master` / `develop`.

## Building a signed release

Release signing reads credentials from a git-ignored `keystore.properties` at the repo root; the
build is then produced with `./gradlew assembleRelease`. See **[RELEASING.md](RELEASING.md)** for
the full, keystore-and-all release runbook.
