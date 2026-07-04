# Covers

Tankobon has **two distinct kinds of cover**, which are easy to confuse:

## 1. Series cover (`cover.jpg`)

The optional `cover.jpg` inside a series folder is the thumbnail for the **whole series** —
shown on the library card and the detail header. This is long-standing behaviour and is
unchanged. If present, it's surfaced as a `content://` URI and rendered through the standard
cover pipeline.

## 2. Per-volume covers (grid view)

Each **individual volume** also shows **its own cover**, extracted from the first image *inside*
that volume's archive/folder. These appear in a **grid (covers) view** you can toggle alongside
the existing list view from the manga detail toolbar. The list view remains the default and is
unchanged.

### How the cover is chosen

The per-volume cover is **the alphanumerically-first image in the archive/folder, using natural
sort** — nothing more.

- **Natural sort** means `page2` comes before `page10`, and `p000 < p001 < p010 < p100`.
- There is deliberately **no** special-casing of `[Cover]` tags, `p000` names, or `cover.*`
  filenames. Scanlation naming conventions vary too widely for that to be reliable, and
  natural-sort-first already resolves real-world patterns correctly (it picks the right page for
  both `... p000 [Digital-HD] ...` and `... p000 [Cover] ...` styles).
- Extraction reads entry **names** first, picks the target, then decodes **only that one entry** —
  so it's fast even on very large archives (hundreds of MB to multiple GB) and never blocks the UI
  on a full decode.
- If an archive contains **zero images**, there's simply no thumbnail (graceful, not a crash).

The selection logic is a small, pure, unit-tested function (`VolumeCoverSelector.selectCover`).

### Caching & invalidation

Per-volume thumbnails are **downscaled** and stored in a dedicated on-disk cache
(`volume_covers/`) as small JPEGs — far smaller than the source pages. The cache key is
`MD5("<volume file URL>;<lastModified>")`, embedding the file's modification time. So if you
replace a volume file, its key changes and the thumbnail is re-extracted automatically — the cache
is **self-invalidating**, mirroring how the series `CoverCache` works.

RAR/CBR, 7z, TAR, ZIP/CBZ, folders and EPUB all go through the same single extraction path.
