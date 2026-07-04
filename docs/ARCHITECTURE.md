# Architecture

Tankobon is an unofficial fork of [Mihon](https://github.com/mihonapp/mihon) (successor to
Tachiyomi), narrowed to a **local-file-only, volume-based** manga reader. This page summarizes the
module layout, what the fork removed vs. added, and roadmap status.

## Module map

| Module | Responsibility |
| --- | --- |
| `app` | Main Android app: Compose UI + Voyager navigation, library/updates/history/more, reader, backup, tracking. Package roots `eu.kanade.tachiyomi.*`, `eu.kanade.presentation.*`, `mihon.*`. |
| `core/common` | Shared core utilities/constants. |
| `core/archive` | Archive (cbz/zip/rar/7z/epub…) reading. |
| `core-metadata` | `ComicInfo.xml` metadata model. |
| `data` | Data layer: SQLDelight database + repositories. |
| `domain` | Domain models + interactors. |
| `source-api` | Source API abstractions — now **local-only** (the online/HTTP source chain was deleted). |
| `source-local` | The **local file source** — the only source. `LocalSource` (id `0L`). |
| `presentation-core` | Shared Compose presentation utilities. |
| `presentation-widget` | Home-screen widgets. |
| `i18n` | moko-resources localized strings (source of truth: `i18n/.../base/strings.xml`). |
| `telemetry` | Firebase (`firebase` flavor) / no-op (`noop` flavor) telemetry — off by default. |
| `baseline-profile` | Startup baseline/benchmark profiles. |
| `gradle/build-logic` | Custom Gradle convention plugins. |

## What was removed

The fork deliberately removed everything online, so Tankobon only ever reads local files:

- **The entire extension system** — installer/loader, extension repo/store, trust system, update
  jobs, and the extensions browse UI.
- **All online sources** — the `HttpSource` / `CatalogueSource` / `ParsedHttpSource` /
  `SourceFactory` chain and global search. `LocalSource` is the only runtime source.
- **The download subsystem** — downloader, download queue/manager, and per-screen download actions.
- **Source-to-source migration** and the deeplink feature.

Some inert scaffolding and internal `mihon.*` namespaces remain (see "Deferred").

## What changed: volumes, not chapters

The reading unit was reworked from *chapter* to **volume** (one archive/folder = one volume):

- `Chapter` → `Volume` throughout the data/domain/UI layers.
- Database: `chapters` table → `volumes`; `chapter_number REAL` → `volume_number INTEGER`
  (whole numbers); new `volumes.volume_number_end` (omnibus ranges) and `mangas.edition` columns
  (schema migration **v14**, additive/non-destructive).
- A new naming-convention parser — see **[LOCAL_LIBRARY_FORMAT.md](LOCAL_LIBRARY_FORMAT.md)**.

The external-tracker layer (MAL/AniList/Kitsu/etc.) intentionally **keeps** "chapter" terminology,
because those services genuinely track chapters — the local volume number is mapped onto their
chapter fields.

## What was added

- **Per-volume cover thumbnails + a grid view** — see **[COVERS.md](COVERS.md)**.
- **On-device English OCR + English→Arabic translation** in the reader — see
  **[TEXT_DETECTION.md](TEXT_DETECTION.md)**.
- **Rebrand** to Tankobon, including `applicationId = app.tankobon`.

## Roadmap status

The major planned work is **done**: extension removal, the volume-based rework, per-volume covers,
legacy cleanup, text detection/translation, and the first release (v0.1.0). Remaining work is
polish/backlog.

## Deferred / intentionally left

- The internal Kotlin **`mihon.*` package namespace**, the Gradle version catalog (`mihonx`), and
  some build-logic package names — cosmetic renames, high churn, deliberately not done.
- **Branding assets** (`ic_mihon.xml` splash/in-app marks) — see **[BRANDING.md](BRANDING.md)**.
- Tracker `User-Agent` strings and the app update checker still reference Mihon (functional
  identifiers; changed only deliberately).
- A little inert dead code/schema kept to avoid risky migrations.

Tankobon is built on Mihon / Tachiyomi and licensed under **Apache-2.0**.
