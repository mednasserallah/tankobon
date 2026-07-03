# CLAUDE.md — Tankobon

## What this project is

**Tankobon** is an unofficial fork of [Mihon](https://github.com/mihonapp/mihon) (which is itself the successor to Tachiyomi), an Android manga reader written in Kotlin and licensed under **Apache-2.0**. This fork narrows Mihon's scope to a **local-file-only, volume-based** manga reader: it will read manga you already own on-device, organized by *volume* rather than *chapter*, with no remote sources or extensions.

## Roadmap (NOT YET DONE — future work)

The fork's goals:

1. **Remove the extension system entirely.** ✅ **DONE** (Task 2, on `feature/remove-extensions`). No remote sources, no extension repo/installer/updater, no online browsing/search, no download subsystem. `LocalSource` (id `0L`) is now the only source the app can hold. See the "Extension System Map" section below for exactly what was removed vs. deferred.
2. **Local source rework — volumes, not chapters.** ⬅️ **NEXT / not started.** The local parser (`source-local/`) must treat each **volume** as the atomic reading unit and use an opinionated naming convention:
   ```
   Series Name/
     ├─ Series Name - Volume 01 (Year).cbz
     ├─ Series Name - Volume 02 (Year).cbz
     ├─ cover.jpg        (optional, ignore — not a volume)
     └─ details.json     (optional, ignore — not a volume)
   ```
   Edge cases to handle: single-volume/one-shots with no "Volume NN" (`Boy Meets Maria (2021).cbz`); series names containing parentheses (`BLAME! (Master Edition)`) must not be confused with the trailing `(Year)`; `cover.jpg`/`details.json` sidecars must never be parsed as volumes; empty series folders are skipped, not errored.
   Start point: `source-local/src/androidMain/kotlin/tachiyomi/source/local/LocalSource.kt`.
   ⬅️ **IN PROGRESS** on `feature/volume-based-migration` (Task 3). See "Chapter → Volume Migration Map" below.

## Module map (top-level)

| Module | Responsibility |
| --- | --- |
| `app` | Main Android application: UI (Compose, Voyager nav), library/updates/history/more, backup, tracking, reader. Package roots: `eu.kanade.tachiyomi.*`, `eu.kanade.presentation.*`, and `mihon.*`. (Extension, download-queue, browse-sources, migration, deeplink, and global-search subtrees have been removed.) |
| `core/common` | Shared core utilities/constants (`tachiyomi.core.common`, `mihon.core.common`). |
| `core/archive` | Archive (cbz/zip/epub) reading (`mihon.core.archive`). |
| `core-metadata` | ComicInfo.xml metadata model (`tachiyomi.core.metadata`). |
| `data` | Data layer: SQLDelight DB, repositories. |
| `domain` | Domain models + interactors (`tachiyomi.domain.*`, `mihon.domain.*`). |
| `source-api` | Source API abstractions (`eu.kanade.tachiyomi.source.*`). Now local-only: `Source`, `UnmeteredSource`, and the `model/**` classes remain; the online chain (`HttpSource`/`CatalogueSource`/`ConfigurableSource`/`ResolvableSource`/`ParsedHttpSource`/`SourceFactory`/`PreferenceScreen`) has been deleted. |
| `source-local` | **Local file source** — the only source; the module for the volume rework. `LocalSource` implements `Source, UnmeteredSource`. |
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

## Extension System Map (Task 2 — remove extensions, local-only)

**STATUS: ✅ DONE on `feature/remove-extensions`** (not yet merged to `develop`). Removed in build-green commits: source-to-source migration, global search, extensions browsing UI, extension repo/store settings + backend, the whole extension backend + `AndroidSourceManager` rewrite (Local is the only runtime source), the Browse bottom-nav tab (local browsing now launches from a Library toolbar action), the online affordances on the local catalog, the entire download subsystem (backend + queue UI + per-screen actions + reader decoupling), the deeplink feature, enhanced trackers (Komga/Kavita/Suwayomi), and the online source-api interfaces. Orphaned deps (shizuku, flexibleAdapter) dropped. Verified: `assembleDebug` + `spotlessCheck` + `testDebugUnitTest` green; app launches on the emulator without crashing.

Investigation results below (KEEP = needed for local/shared; REMOVE = remote/extension-only, now done; DEFER = harmless dead code/schema left in place).

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

## Chapter → Volume Migration Map (Task 3 — volume-based rework)

**STATUS: 🚧 IN PROGRESS on `feature/volume-based-migration`.** Phase 0 investigation results below. The goal: model the reading unit as a **volume** (one `.cbz`/`.zip`/`.cbr`/folder = one volume) instead of a chapter, and rewrite the local file parser for Tankobon's naming convention.

### Phase 0 — where "chapter" lives (investigated, verified)

Scope: **197 files** reference "chapter" (case-insensitive) — `app` 131, `domain` 35, `data` 22, `source-api` 5, `source-local` 2, others. A full type-rename is **high-churn and high-risk** (SQLDelight-generated code + live external trackers + backup proto). See the Phase 2 decision below.

1. **Data layer.** `Chapter` entity = `domain/.../tachiyomi/domain/chapter/model/Chapter.kt` (`chapterNumber: Double`, `read`, `bookmark`, `lastPageRead`, `sourceOrder`, `url`, `name`, …). Table = `data/.../tachiyomi/data/chapters.sq` (`chapter_number REAL NOT NULL`, FK `manga_id → mangas`). Mapped positionally by SQLDelight → `ChapterMapper`/`copyFromSChapter` (`eu/kanade/domain/chapter/model/ChapterMapper.kt`). `chapter_number` is `REAL/Double` to allow decimals like "10.5" — **volumes are whole numbers, so this is a semantic simplification, not just a rename.**
2. **Local parser.** `source-local/.../LocalSource.kt` `getChapterList()` builds one `SChapter` per file, name = filename-without-extension, and `chapter_number = ChapterRecognition.parseChapterNumber(...)` (`domain/.../chapter/service/ChapterRecognition.kt` — generic Tachiyomi "Ch. 12"/"Vol.1 Ch.4.5" regex). Files are sorted by **name, natural order, descending** (`compareToCaseInsensitiveNaturalOrder`). Sidecars are currently NOT excluded beyond dot-files (json/cover would be filtered only by format check). **This is what Phase 1 replaces.** Note: `SyncChaptersWithSource` (`app/.../domain/chapter/interactor/`) *re-runs* `parseChapterNumber` but preserves an already-recognized (`≥0`) number — so a number set by the parser survives.
3. **Progress tracking.** Per-`chapter` row: `read`/`bookmark`/`lastPageRead`. `ChapterGetNextUnread` + `getChapterSort` (`domain/.../chapter/service/ChapterSort.kt`: SOURCE / NUMBER / UPLOAD_DATE / ALPHABET). No chapter-*count*-specific logic that breaks under volumes; "next unread" is just first-unread in sorted order. Carries over conceptually to per-volume.
4. **Reader UI.** `ReaderViewModel` builds prev/next **positionally** from `chapterList.sortedWith(getChapterSort(...))` — so correct numeric sort (Phase 1) makes "next volume" work with no structural change. Transition card = `presentation/reader/ChapterTransition.kt` (shows `chapter.name` + a "missing N chapters" gap via `MissingChapters.calculateChapterGap`, which does `floor(a)-floor(b)-1` — **already integer-based**, harmless for whole volumes; it now means "missing volumes"). Decimal-specific display: `presentation/util/ChapterNumberFormatter.kt` (`DecimalFormat("#.###")`).
5. **Library / manga-detail UI.** Volume-list rows: `presentation/manga/MangaScreen.kt:696` → `display_mode_chapter` string with `formatChapterNumber(chapterNumber)` when `displayMode == CHAPTER_DISPLAY_NUMBER`, else raw `name`. Missing-count separators in `MangaScreenModel.chapterListItems`. Library unread badge = count of unread chapter rows (becomes volume count). All terminology + the `#.###` formatter are Phase 4.
6. **Trackers / sync — ⚠️ SURPRISE: STILL LIVE, NOT dead.** All **6 generic trackers** (MAL, Anilist, Kitsu, Shikimori, Bangumi, MangaUpdates) survived the extension removal and **actively push "last chapter read" (a number) to external services** (`data/track/*Api.kt`, `TrackChapter`, `AddTracks`, `RefreshTracks`, `DelayedTrackingUpdateJob`). `manga_sync.last_chapter_read` is `REAL/Double`; `Track.lastChapterRead: Double`. Only `SyncChapterProgressWithTrack` (remote→local) is a dead no-op. **Implication:** retyping/renaming chapter-number would ripple into 6 external API serializers + the `manga_sync` schema + backup `BackupTracking` proto. This is the single biggest argument for Option B. Kept as-is; the number now semantically carries a *volume* count to services that expect a chapter count — an imperfect-but-acceptable mapping, flagged for a future deliberate decision (some services have distinct volume/chapter fields).
7. **Backup / restore — proto format constrains us.** `BackupChapter.chapterNumber` = `@ProtoNumber(9)` **`Float`**; restore matches chapters **by `url`** (not by number), manga by url+source. So: renaming the DB column does NOT break restoring old Mihon/Tankobon backups (matching is url-based), **but** `BackupChapter.chapterNumber(9)` must stay `Float` at proto field 9 — do not retype it. New fields go under fresh ProtoNumbers (backup format is unversioned but uses default `ProtoBuf`, which tolerates unknown fields): `BackupChapter` next free = **14**; `BackupManga` next free = **113**.

### Phase 2 decision — **Option A (full rename)** — chosen by the user

Phase 0 recommended Option B (semantic reuse) because of the deep coupling below, but the **user explicitly chose Option A (full rename)** after that risk was surfaced. So: rename **`Chapter` → `Volume`** and **`chapter_number` → `volume_number` (integer)** throughout the data layer — domain entity, `chapters` table → `volumes` table, SQLDelight queries/views/generated code, mappers, `SChapter` → `SVolume`, reader/library identifiers, and the tracker/`manga_sync` layer.

**Non-negotiable safety constraints (kept even under "full rename"):**
- **Backup proto field *numbers* are wire format — never renumber.** Kotlin property names may change (`chapterNumber` → `volumeNumber`) but the `@ProtoNumber(9)` stays `9` and stays `Float`, so old Mihon/Tankobon backups still restore (matching is url-based anyway). New fields use fresh numbers: `BackupVolume` next free = **14**, `BackupManga` edition = **113**.
- **External tracker API JSON keys are the remote services' contract — never rename.** MAL's `num_chapters_read`, Anilist/Kitsu `progress`, Shikimori `chapters`, Bangumi `ep_status`, MangaUpdates `chapter` stay exactly as the services require. Only the *internal* Kotlin identifiers rename (`Track.lastChapterRead` → `lastVolumeRead`, `manga_sync.last_chapter_read` → `last_volume_read`). NOTE: the value now carries a *volume* count to services that model it as chapters — an imperfect but accepted mapping (some services have distinct volume fields; deferred as a future product decision).
- **Migrations are additive / non-destructive** — table/column renames use `ALTER TABLE … RENAME` (SQLite preserves data), and new columns default `NULL`. No user data destroyed on upgrade.

**Concrete data-model changes (real migrations — central to the task):**
- `chapter_number REAL` → `volume_number INTEGER` (whole-number; the Phase 1 parser only emits integers). Retyped at the DB, domain (`Volume.volumeNumber: Long`), mapper, backup (`BackupVolume.volumeNumber` stays `@ProtoNumber(9) Float` for compat, converted at the boundary), and the reader/UI.
- **NEW `volumes.volume_number_end INTEGER` (nullable)** — set only for omnibus range files (`Volume 01-02` → `volume_number = 1`, `volume_number_end = 2`); `null` for single-volume files. Sorting + next/prev key off `volume_number` (the start). Plumbed: schema + migration, `Volume` model, SQLDelight mappers, `SVolume` + `copyFromSVolume`, `BackupVolume @ProtoNumber(14)`.
- **NEW `mangas.edition TEXT` (nullable)** — the extracted edition tag (e.g. `"Omnibus Edition"`), a **series/folder-level** property (one edition per folder), `null` when absent. Plumbed: `mangas.sq` + migration, `Manga` model, `MangaMapper` (all overloads), `SManga` + `toDomainManga`, `BackupManga @ProtoNumber(113)`.
- **DB migrations from v13**: additive columns (`volume_number_end`, `edition`) + the `chapters→volumes` / `chapter_number→volume_number` renames, expressed as non-destructive `.sqm` steps.

### Naming-convention parser rules (Phase 1 spec — `VolumeRecognition`, in `domain/.../volume/service/` after the rename)

Input is a **series folder name** and a **volume file name** (basename, extension stripped). Rules:
- **Year** = the **last** parenthetical group matching `^\d{4}$`. This disambiguates trailing `(2016)` from an edition like `(Master Edition)`. Parsed **per file** (never inherited across volumes).
- **Edition** (folder-level) = any parenthetical group in the *series folder name* that is **not** a 4-digit year, captured verbatim (`Gantz (Omnibus Edition)` → title `Gantz`, edition `Omnibus Edition`). No hardcoded edition whitelist.
- **Volume number** = `Volume\s+(\d+)(?:-(\d+))?` (case-insensitive), capturing an optional range end. `Volume 01-02` → start `1`, end `2`, display `Volume 01-02` (range shown verbatim, not collapsed). Sort by **start**.
- **Single-volume / one-shot** (no "Volume NN" segment, e.g. `Boy Meets Maria (2021)`): treated as **Volume 1** (convention: assign volume number `1`, `volume_number_end = null`; display uses the file's own title text). *(Documented choice: number = 1 so it sorts and tracks like any single volume.)*
- **Sidecars excluded**: `cover.*`, `details.json`, `ComicInfo.xml`, `.noxml`, dot-files, and any non-archive/non-directory/non-epub file are never volumes.
- **Empty series folder** (no valid volume files): produces an empty volume list — skipped gracefully, no error/crash.
- **Sort**: numeric by volume start (`1, 2, … 9, 10`), never lexical.
- **Volume-count badge**: counts **volumes represented** (an omnibus `Volume 01-02` counts as 2), not files — better reflects what the user owns.
- **Fallback** (filename matches nothing above): use the whole basename as the display title, volume number `1`, and log it (don't crash / don't silently drop).

## Deferred cleanup (Task 2 backlog — intentionally left as harmless dead code/schema)
DB / schema (do NOT migrate now — bump version + add `.sqm` in a deliberate future pass):
- `extension_store` table (`data/src/main/sqldelight/tachiyomi/data/extension_store.sq`) — unused now the extension-repo feature is gone. Empty = harmless.
- `sources` table (`.../data/sources.sq`) — stub/uninstalled-source name cache; still written by `StubSourceRepositoryImpl` from backups but never needed for local-only.
- `mangas.sq` network-fetch query blocks (`insertNetworkManga`, etc.) — logically dead, schema-harmless.
- Chapter "downloaded" filter flags: `Manga.downloadedFilterRaw` / `CHAPTER_DOWNLOADED_MASK`, `LibraryPreferences.filterDownloaded`, `UpdatesPreferences.filterDownloaded`, `SetMangaChapterFlags.awaitSetDownloadedFilter` — inert now downloads are gone; kept to avoid a chapter-flag migration.

Code / prefs (low-priority, non-build-gating):
- Unused i18n strings for removed features (`ext_*`, `action_global_search*`, `extensionStores`, download/migration strings) — left in `base/strings.xml`; removing translatable strings touches 30+ locale files.
- Inert `global` flag in `MangaScreen`/`MangaInfoHeader` title/author taps (now no-ops), and unused `SourcePreferences` browse prefs (`enabledLanguages`, `disabledSources`, `pinnedSources`, `lastUsedSource`, `showNsfwSource`, `globalSearchFilterState`).
- A few unused injected fields left by the removals (e.g. `addTracks`/`trackerManager` in some screen models), and the now-unreferenced `ui/webview/WebViewScreen.kt` Voyager screen (generic WebView infra kept).
- `SyncChapterProgressWithTrack` is now a no-op (only ever did work for the removed enhanced trackers).
- `.github/AndroidManifest`/shortcuts still reference `SHORTCUT_EXTENSIONS`/search intents that no longer resolve (inert).

## Gotchas

- After deleting code, run `./gradlew spotlessApply` — it auto-prunes unused imports and reformats. (`spotlessCheck` is the CI gate; ktlint's unused-import rule is inconsistent here, so don't rely on it to catch every orphaned import — prune them yourself / via `spotlessApply`.)
- On this machine there is no JDK on PATH — every Gradle/`adb`-driving shell must `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` first. A running emulator is `emulator-5554`; `adb` lives at `~/Library/Android/sdk/platform-tools/adb`; debug app id is `app.mihon.dev`.
- `app_name` is `translatable="false"`; only edit it in `base/strings.xml`, not per-locale files.
- Don't confuse the `mihon.*` Kotlin package (internal namespace, Phase B) with the word "Mihon" as a display/brand string (Phase A).
</content>
