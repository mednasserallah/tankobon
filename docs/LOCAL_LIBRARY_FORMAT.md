# Local library format

Tankobon reads manga you already own on-device. Unlike the app it was forked from, the reading
unit is a **volume** (one `.cbz` / `.zip` / `.cbr` / folder = one bound volume), not a chapter.
This page describes exactly how Tankobon interprets your files.

## Folder layout

Organize each series as a folder, with one file (or subfolder) per volume:

```
Series Name/
  ├─ Series Name - Volume 01 (2016).cbz
  ├─ Series Name - Volume 02 (2016).cbz
  ├─ cover.jpg        (optional — series cover, not a volume)
  └─ details.json     (optional — series metadata, not a volume)
```

Point Tankobon's local library at the parent directory that contains your series folders.

## Supported volume formats

Archives: **`.cbz`, `.zip`, `.cbr`, `.rar`, `.7z`, `.cb7`, `.tar`, `.cbt`**, plus **EPUB** and
plain **folders** of images. (Archive *writing* isn't involved — extraction is read-only.)

## Naming convention

Tankobon parses the **series folder name** and each **volume file name** (extension stripped)
with these rules:

### Year
The **last** parenthetical group that is exactly four digits, e.g. `(2016)`. Parsed **per file**
(never inherited between volumes). Taking the *last* one disambiguates a trailing year from an
edition — see below.

### Edition (series-level)
Any parenthetical group **in the series folder name** that is *not* a 4-digit year, captured
verbatim. One edition per folder; `null` when absent. There is no hard-coded whitelist.

- `BLAME! (Master Edition)` → title `BLAME!`, edition `Master Edition`
- `Gantz (Omnibus Edition)` → title `Gantz`, edition `Omnibus Edition`

The edition shows as a badge on the series detail header.

### Volume number
Matched as `Volume NN`, case-insensitive, with an optional range end `Volume NN-MM`.

- `... - Volume 03 (2016).cbz` → volume **3**
- `... - Volume 01-02 (2016).cbz` → an **omnibus**: start **1**, end **2**, displayed verbatim
  as "Volume 1-2". Sorting and next/previous key off the **start** number.

### One-shots / single volumes
A file with **no** `Volume NN` segment (e.g. `Boy Meets Maria (2021).cbz`) is treated as
**volume 1**, and its display name is the file's own title with the year stripped
("Boy Meets Maria").

### Sidecars (never treated as volumes)
`cover.*`, `details.json`, `ComicInfo.xml`, `.noxml`, any dot-file, and any file that isn't a
supported archive / folder / EPUB are excluded.

### Empty series folders
A folder with no valid volume files yields an empty list — skipped gracefully, never an error.

## Sorting & gaps

- Volumes sort **numerically** by start number: `1, 2, … 9, 10` — never lexically (`1, 10, 2`).
- Missing numbers are surfaced (e.g. "Missing 5 volumes") between rows and at reader transitions.

## Worked examples

| File / folder | Parsed as |
| --- | --- |
| `Ao Haru Ride/Ao Haru Ride - Volume 05 (2013).cbz` | Series "Ao Haru Ride", volume 5, year 2013 |
| `BLAME! (Master Edition)/... - Volume 02 (2016).cbz` | Edition "Master Edition", volume 2 |
| `Gantz (Omnibus Edition)/... - Volume 01-02 (2000).cbz` | Edition "Omnibus Edition", volumes 1–2 (omnibus) |
| `Boy Meets Maria (2021).cbz` | One-shot → volume 1, named "Boy Meets Maria" |
| `Some Series/cover.jpg` | Ignored (series cover sidecar) |

## Notes

- The per-volume unread badge counts **volume rows** (an omnibus `Volume 01-02` is one row).
- Volume numbers are **whole numbers** — there is no `10.5`-style decimal chapter concept.
- Entries scanned by an older build keep their previously-parsed values until re-scanned; a fresh
  scan applies the current rules.
