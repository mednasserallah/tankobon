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

## Extension System Map (Phase 0 investigation for Task 2 — remove extensions, local-only)

Goal of Task 2: make **Local source the only source the app can have**. Investigation results below (KEEP = needed for local/shared; REMOVE = remote/extension-only; DEFER = harmless dead code/schema, do not touch now).

### Source abstraction (`source-api`)
- **`Source`** (`source-api/.../source/Source.kt`) — base interface; this fork moved the catalogue methods (`getPopularManga`/`getLatestUpdates`/`getSearchManga`/`getMangaUpdate`/`getPageList`) onto `Source` itself. **KEEP.**
- **`UnmeteredSource`** — marker; `LocalSource` implements it. **KEEP.** Model classes (`Filter`, `FilterList`, `MangasPage`, `Page`, `SChapter`, `SManga`, `SMangaUpdate`, `UpdateStrategy`). **KEEP.**
- **Online chain** — `CatalogueSource`, `ConfigurableSource`, `HttpSource`, `ParsedHttpSource`, `ResolvableSource`, `SourceFactory`, `PreferenceScreen`. No concrete implementations exist in-repo (real ones live in external extension APKs). Once `ExtensionManager` is gone nothing can instantiate them, so **they are dead abstractions**. Deleting them ripples into `Downloader` (HttpSource-only), deeplink (`ResolvableSource`), `SourceManager.getOnlineSources()`, backup validator. **DEFER full deletion** (keep as dead code) unless we accept that ripple.
- **`LocalSource`** (`source-local/.../LocalSource.kt`) implements `Source, UnmeteredSource`. `ID = 0L`. Locality is `manga.source == LocalSource.ID`; helpers `Manga.isLocal()`/`Source.isLocal()`/`DomainSource.isLocal()` at end of that file. **KEEP (core).**

### The linchpin: `AndroidSourceManager`
`app/.../source/AndroidSourceManager.kt` composes the source map from `LocalSource` **+** `extensionManager.installedExtensionsFlow`. It is injected everywhere via the `SourceManager` interface (`domain/.../source/service/SourceManager.kt`). **Rewrite** it to only ever hold `LocalSource` and drop the `ExtensionManager` dependency. `getOnlineSources()` → `emptyList()`, `getStubSources()` → trivial. This single change makes Local the only runtime source. `getOrStub`/`get` must still work (library/backup call them).

### Extension backend — REMOVE
- `app/.../extension/ExtensionManager.kt`; `extension/util/*` (ExtensionInstaller, ExtensionLoader, ExtensionInstallReceiver, ExtensionInstallActivity, ExtensionInstallService); `extension/installer/*` (Installer, ShizukuInstaller, PackageInstallerInstaller); `extension/api/*` (ExtensionApi, ExtensionUpdateNotifier); `extension/model/*` (InstallStep, LoadResult).
- `eu/kanade/domain/extension/**` (GetExtensionsByType, GetExtensionSources, GetExtensionLanguages, TrustExtension, `model/Extensions.kt`); `domain/.../eu/kanade/tachiyomi/extension/model/Extension.kt`.
- Extension **store/repo** feature: `mihon/domain/extension/**`, `mihon/data/extension/**` (ExtensionStoreService/RepositoryImpl + Network* models).
- Migration `mihon/core/migration/migrations/TrustExtensionRepositoryMigration.kt` (+ remove from `Migrations.kt` list).
- DI (`di/AppModule.kt` L119 `ExtensionManager`; `DomainModule.kt` L178-180, L196, L198-204 extension bindings). Manifest (`AndroidManifest.xml`): `ExtensionInstallActivity`, `ExtensionInstallService`, `ShizukuProvider`, install/`QUERY_ALL_PACKAGES` perms, `add-repo`/`extension-store` deeplink intent-filters. `MainActivity.kt`: `ExtensionApi().checkForUpdates` (L330-337), extension-store deeplink handling (L572-595). No `ExtensionUpdateJob`/WorkManager (update check is inline from MainActivity).

### UI surfaces (`ui/browse/**`, `presentation/browse/**`)
- **KEEP (local browse/add-to-library entry point):** `ui/browse/source/SourcesTab.kt` + `SourcesScreenModel.kt` (trim global-search/filter/pin actions); `ui/browse/source/browse/BrowseSourceScreen*.kt` + `SourceFilterDialog.kt` (strip WebView, `SourcePreferencesScreen`, `MissingSourceScreen`/StubSource, migrate branch); presentation `BrowseSourceScreen.kt` + `components/BrowseSource*`, `Base*Item`, `BrowseBadges/Icons`, `BrowseSourceDialogs`. Flow: `SourcesTab → BrowseSourceScreen(LocalSource.ID=0L)`; `GetEnabledSources` already includes Local via `isLocal()`; `GetRemoteManga` (misnamed) routes id 0 → `LocalSource` paging — **KEEP `GetRemoteManga`**.
- **REMOVE:** `ui/browse/extension/**` + presentation `Extension*`; `ui/browse/source/globalsearch/**` + presentation `GlobalSearch*`; `ui/browse/source/SourcesFilterScreen*` + presentation `SourcesFilterScreen`; `ui/browse/migration/**` and `mihon/feature/migration/**` + presentation `Migrate*` (source-to-source, meaningless with one source); `ui/deeplink/**` (resolves via `ResolvableSource`, online-only) + its manifest filters.
- **Nav restructure:** `ui/home/HomeScreen.kt` `TABS` (L74-80) + `Tab.Browse(toExtensions)` sealed type; `BrowseTab.kt` (drop extensions/migrate sub-tabs + global-search onReselect); `MainActivity` shortcuts `SHORTCUT_SOURCES`(keep→local)/`SHORTCUT_EXTENSIONS`(remove). Decide: keep Browse tab hosting only local sources list, or point it straight at `BrowseSourceScreen(0L)`.

### Settings — REMOVE (extract 1 toggle)
- `SettingsBrowseScreen.kt` is almost all extension/repo: keep only `hideInLibraryItems`; remove `extensionStores` nav + NSFW group; drop its entry from `SettingsMainScreen.kt`. Remove `presentation/more/settings/screen/browse/**` (ExtensionStores screens/components).
- `SourcePreferences.kt` — only `sourceDisplayMode` (+ maybe `hideInLibraryItems`) survives; everything else (enabledLanguages, disabledSources, pinnedSources, lastUsedSource, showNsfwSource, extensionRepos, extensionUpdatesCount, trustedExtensions, globalSearchFilterState, all migration prefs) is REMOVE.

### Background work & networking
- **KEEP:** `LibraryUpdateJob.kt` (+ scheduling), `LibraryUpdateNotifier.kt`, `MetadataUpdateJob.kt`, and `mihon/domain/source/interactor/UpdateMangaFromRemote.kt`. These are a **single polymorphic path** — they call `source.getMangaUpdate(...)` without branching on local vs remote; the local/remote split lives entirely inside the `Source` implementations. With extensions gone they simply drive `LocalSource` rescans. The name "…FromRemote" is misleading; do **not** delete. Phase 3 therefore has almost nothing to rip out — just verify local rescan still works.
- **KEEP (shared, NOT extension-only):** `core/common/.../network/NetworkHelper` and the network package — used by Coil covers, trackers, the app-updater, and WebView. Remove only its extension-specific *callers*.
- **`Downloader.kt`** is hard-coupled to `HttpSource` (`sourceManager.get(...) as? HttpSource ?: return`) and never downloads from Local. With online sources gone it is dead code but is deeply wired into DownloadManager/reader/UI. **DEFER** removal (leave as inert dead code) to avoid a large ripple.

### Data model / DB / backup / trackers
- **No destructive migration in this task.** Schema latest version = 13 (`data/src/main/sqldelight/tachiyomi/migrations/*.sqm`).
- **KEEP (required):** `mangas.source` column + `idx_mangas_source` (always `0` for local); `manga_sync` table + generic trackers (anilist/mal/kitsu/bangumi/shikimori/mangaupdates); `Manga.source` field; `BackupManga.source` + `getMangaByUrlAndSourceId` restore matching. Restore already tolerates unknown source ids (used only for error labels + url/source matching) → no crash on local-only.
- **REMOVE (Kotlin):** backup `SourcesBackupCreator`/`ExtensionStoresBackupCreator`/`ExtensionStoreRestorer` + models `BackupExtensionStore`/`BackupSource` (leave proto fields empty for format compat); `StubSourceRepositoryImpl` writers; enhanced trackers `komga/Komga`, `kavita/Kavita`, `suwayomi/Suwayomi` (bind to extension class names that can't exist — dead; removal candidates).
- **DEFER (harmless dead schema — see "Deferred DB cleanup"):** `extension_store` table, `sources` (stub-source cache) table.

### Execution ordering note (build-green constraint)
`ExtensionManager` is referenced by both backend and the extension UI, so the task's literal "installer first, UI second" order would break compilation mid-phase. Plan: remove **leaf UI consumers first** (migration → global search → extensions tab/settings/deeplink → browse-tab restructure → strip online bits from BrowseSourceScreen), **then** the extension backend + rewrite `AndroidSourceManager`, then data/deps. Each commit ends build-green. Task-phase labels are preserved in commit messages.

## Deferred DB cleanup (Task 2 backlog — do NOT migrate now)
- `extension_store` table (`data/src/main/sqldelight/tachiyomi/data/extension_store.sq`) — becomes unused once the extension-repo feature is removed. Leaving it empty is harmless. Drop in a future deliberate schema migration (bump version, add `.sqm`).
- `sources` table (`.../data/sources.sq`) — stub/uninstalled-source name cache; never written once `StubSourceRepositoryImpl` writers go. Harmless empty. Same deferred-migration treatment.
- `mangas.sq` network-fetch query blocks (`insertNetworkManga`, etc.) are logically dead with local-only but are schema-harmless; can be pruned as Kotlin/SQL cleanup later.

## Gotchas

- `assembleDebug` compiles fine with unused imports (warnings), but **`spotlessCheck` (CI) fails on them** — always prune imports after deleting code.
- `app_name` is `translatable="false"`; only edit it in `base/strings.xml`, not per-locale files.
- Don't confuse the `mihon.*` Kotlin package (internal namespace, Phase B) with the word "Mihon" as a display/brand string (Phase A).
</content>
