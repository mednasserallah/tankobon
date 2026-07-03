# CLAUDE.md — Tankobon

## What this project is

**Tankobon** is an unofficial fork of [Mihon](https://github.com/mihonapp/mihon) (which is itself the successor to Tachiyomi), an Android manga reader written in Kotlin and licensed under **Apache-2.0**. This fork narrows Mihon's scope to a **local-file-only, volume-based** manga reader: it will read manga you already own on-device, organized by *volume* rather than *chapter*, with no remote sources or extensions.

## Roadmap (NOT YET DONE — future work)

These are the fork's goals. **Neither is implemented yet** — the codebase is still essentially upstream Mihon plus a Phase A rebrand.

1. **Remove the extension system entirely.** No remote sources, no extension repo/installer/updater. The app should read local files only. Relevant upstream code lives under `app/.../extension/`, `mihon/domain/extension/`, `data/.../extension/`, `source-api`, and the browse/migration UI.
2. **Local source rework — volumes, not chapters.** The local parser (`source-local/`) must treat each **volume** as the atomic reading unit and use an opinionated naming convention:
   ```
   Series Name/
     ├─ Series Name - Volume 01 (Year).cbz
     ├─ Series Name - Volume 02 (Year).cbz
     ├─ cover.jpg        (optional, ignore — not a volume)
     └─ details.json     (optional, ignore — not a volume)
   ```
   Edge cases to handle: single-volume/one-shots with no "Volume NN" (`Boy Meets Maria (2021).cbz`); series names containing parentheses (`BLAME! (Master Edition)`) must not be confused with the trailing `(Year)`; `cover.jpg`/`details.json` sidecars must never be parsed as volumes; empty series folders are skipped, not errored.
   Start point: `source-local/src/androidMain/kotlin/tachiyomi/source/local/LocalSource.kt`.

## Module map (top-level)

| Module | Responsibility |
| --- | --- |
| `app` | Main Android application: UI (Compose, Voyager nav), features, backup, tracking, downloads, reader. Package roots: `eu.kanade.tachiyomi.*`, `eu.kanade.presentation.*`, and `mihon.*`. |
| `core/common` | Shared core utilities/constants (`tachiyomi.core.common`, `mihon.core.common`). |
| `core/archive` | Archive (cbz/zip/epub) reading (`mihon.core.archive`). |
| `core-metadata` | ComicInfo.xml metadata model (`tachiyomi.core.metadata`). |
| `data` | Data layer: SQLDelight DB, repositories. |
| `domain` | Domain models + interactors (`tachiyomi.domain.*`, `mihon.domain.*`). |
| `source-api` | Source/extension API abstractions (`eu.kanade.tachiyomi.source.*`). To be gutted when extensions are removed. |
| `source-local` | **Local file source** — the module for the volume rework. |
| `presentation-core` | Shared Compose presentation utilities. |
| `presentation-widget` | Home-screen widgets. |
| `i18n` | moko-resources localized strings. Source of truth: `i18n/src/commonMain/moko-resources/base/strings.xml`. `app_name` is `translatable="false"` (only defined in `base`). |
| `telemetry` | Firebase (`firebase` flavor) / no-op (`noop` flavor) telemetry. |
| `baseline-profile` | Startup baseline/benchmark profiles. |
| `gradle/build-logic` | Custom Gradle convention plugins (`mihon.gradle.*`). Version catalog is named `mihonx`, sourced from `gradle/mihon.versions.toml`. |

## Build & test

Requires **JDK 21** (see `.github/.java-version`) and the Android SDK.
On this machine there is no system JDK on PATH; use Android Studio's bundled JBR:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```
Commands:
```bash
./gradlew assembleDebug            # build debug APK (primary sanity check)
./gradlew testDebugUnitTest        # unit tests
./gradlew spotlessCheck            # ktlint/spotless format check (CI-gating)
./gradlew spotlessApply            # auto-fix formatting
./gradlew verifySqlDelightMigration
```
CI (`.github/workflows/build.yml`) runs spotlessCheck + tests + a release build on PRs and on pushes to `master`/`develop`.

## Code style

- `.editorconfig` governs: 4-space indent for `.kt`/`.kts`/`.xml`, 2-space elsewhere; `max_line_length = 120`; final newline; trim trailing whitespace.
- ktlint (`intellij_idea` style) via Spotless. **Unused imports fail CI** — clean them up after removing code.
- Compose functions are exempt from function-naming rules.
- **License headers:** never delete existing copyright headers (`Copyright © 2015 Javier Tomás`, `Copyright © 2024 Mihon Open Source Project`). When materially editing a file that has them, you may *add* `Copyright © 2026 Tankobon Contributors` beneath — never replace. Leave `LICENSE` (Apache-2.0 text) untouched. Most source files have no per-file header; do not add one where none exists.

## Git / branching

- Default branch: **`master`** (renamed from `main`). `develop` branches off `master`; feature branches (e.g. `feature/rebrand-tankobon`) branch off `develop`.
- Remotes: `origin` → `mednasserallah/tankobon`, `upstream` → `mihonapp/mihon`.

## Rebrand status

### Phase A — DONE (on `feature/rebrand-tankobon`)
User-facing / documentation rename Mihon → Tankobon:
- `README.md` rewritten for the fork (roadmap, no Mihon store/Discord/website badges).
- `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, issue templates, `ISSUE_TEMPLATE/config.yml` rebranded; official Mihon links removed.
- `.github/FUNDING.yml` removed (pointed to Mihon's Patreon).
- `app_name` string: `Mihon` → `Tankobon` (`base/strings.xml`). Onboarding strings use `%s`/`app_name`, so they inherit the new name automatically.
- About screen (`AboutScreen.kt`): removed official Mihon website/Discord/X/Facebook/Reddit links and the `mihon.app/privacy` item; GitHub link repointed to `mednasserallah/tankobon`; orphaned imports cleaned.
- `release.yml`: visible labels + APK artifact names rebranded, Mihon donation links removed. `build.yml` push trigger `main` → `master`/`develop`.

### Phase B — DEFERRED (do NOT do blindly; separate deliberate tasks)
- **Kotlin package namespace `mihon.*`** (dirs under `*/src/*/java|kotlin/mihon/`, `app/src/main/aidl/mihon/`, package decls, imports). Hundreds of files — use Android Studio *Refactor → Rename Package* + full rebuild, not find-and-replace.
- **`applicationId = "app.mihon"`** in `app/build.gradle.kts` — changing it breaks update paths / signing identity for existing installs. Deliberate decision.
- **Gradle version catalog `mihonx`** + `gradle/mihon.versions.toml` filename, and `mihon.gradle.*` build-logic package.
- **Branding assets:** `ic_mihon.xml`, `ic_mihon_splash.xml` drawables, splash in `app/src/main/res/values/themes.xml`, `.github/assets/logo.png`. Design task.
- **Tracker `User-Agent` headers** `"Mihon v..."` in `app/.../data/track/*Interceptor.kt` / `KomgaApi.kt` (some may be tied to API registrations, e.g. bangumi `antsylich/Mihon/...`). Functional identifiers — change deliberately.
- **`Constants.kt` URLs** (`URL_HELP`, `URL_DONATE_*`, `URL_DISCORD` → `mihon.app`/Mihon Patreon) and the **donation/support UI** (`SupportUsScreen`, `donationCampaign.*` / `supportUsScreen.*` strings). Left as-is intentionally: swapping "Mihon"→"Tankobon" here would falsely claim Tankobon has patrons / a lead dev soliciting funds. Decide alongside removing the donation feature.
- **App update checker** (`AppUpdateChecker.kt`, `RELEASE_URL`, `MIHON_GITHUB_RELEASE` env) still points at Mihon releases; CONTRIBUTING advises forks disable/repoint it.
- **Release/website CI infra:** `release.yml` job guards `if: github.repository == 'mihonapp/mihon'` (keeps it inert on the fork), `secrets.MIHON_BOT_TOKEN`, and `.github/workflows/update_website.yml` (dispatches to `mihonapp/website`). Needs holistic fork setup (bot token, signing secrets, whether a website exists) — not a cosmetic pass.

## Gotchas

- `assembleDebug` compiles fine with unused imports (warnings), but **`spotlessCheck` (CI) fails on them** — always prune imports after deleting code.
- `app_name` is `translatable="false"`; only edit it in `base/strings.xml`, not per-locale files.
- Don't confuse the `mihon.*` Kotlin package (internal namespace, Phase B) with the word "Mihon" as a display/brand string (Phase A).
</content>
