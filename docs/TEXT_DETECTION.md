# On-page text detection & translation

Tankobon can detect the English text on the page you're reading, list it in reading order, and
optionally translate each line to **Arabic** — entirely **on-device, offline, and free**.

## Using it

Tap the **detect-text** button in the reader's bottom bar. A bottom sheet lists the detected
lines. For each line you can:

- **Copy** it to the clipboard,
- **Edit** it (tap the text) to fix an OCR slip, or
- **Translate** it to Arabic (per line, or "Translate all").

Copy and translate always act on the **current** text, so an edit flows through to both.

## Offline & free

- **OCR:** Google **ML Kit Text Recognition v2** (Latin script). On-device, no API key, no network.
- **Translation:** Google **ML Kit on-device Translation**. The first Arabic translation triggers
  a **one-time ~30 MB language-model download** (over Wi-Fi); after that, translation is fully
  offline. No account required.
- Per Google's ML Kit / Cloud Translation terms, a **"powered by Google Translate"** notice is
  shown next to the results (and on the About screen).

## Translation engine (ML Kit or DeepL)

Under **Settings → Translation** you can choose the translation engine:

- **On-device (Google ML Kit)** — the default. Free, offline, no account (as above).
- **DeepL (online)** — often better on manga dialogue, but needs **your own DeepL API key** and a
  network connection. Get a free key from DeepL's API Free plan, paste it into the key field
  (masked, with a reveal toggle), and tap **Validate** to confirm it works. Free keys (ending
  `:fx`) and Pro keys are routed to the correct DeepL endpoint automatically.

The key is stored **encrypted on-device** (Android Jetpack Security) and never logged or backed up.
If DeepL is selected but fails (no/invalid key, quota reached, or no network), the app **falls back
to on-device ML Kit** for that translation and shows a brief notice, so you're never left with
nothing. Attribution always credits whichever engine actually produced each line, and "powered by
DeepL" is shown on the About screen alongside Google's.

## How it works

The detection pipeline runs off the main thread:

```
recognize → merge lines (+ collapse hyphenation) → sentence-case → drop number-only lines → sort into reading order
```

- **Line merge** joins wrapped lines within the same speech bubble (conservative thresholds — it
  prefers under-merging a split bubble over blending two separate bubbles).
- **Reading order** is **RTL-aware**: lines are clustered into rows by vertical overlap, ordered
  within a row by x (right-to-left for right-to-left manga, else left-to-right), and rows run
  top-to-bottom. The manga's existing per-title reading direction drives this — no new setting.
- **Number-only lines** (page numbers, number-only bubbles) are dropped; a line with any letter
  ("Chapter 1") is kept.
- **Zoom-scoped detection:** on a paged viewer, if you're zoomed in past fit-to-screen, OCR runs
  only on the visible region (which also makes small text effectively higher-resolution). At
  default zoom the whole page is used, unchanged. Webtoon/continuous mode always uses the whole page.

## Known limitations (heuristics, not magic)

These are readability heuristics tuned on real pages — best-effort, not perfect:

- **Sentence-casing** lowercases then capitalizes sentence starts. It will lowercase acronyms
  ("NASA" → "Nasa"), drop proper-noun capitals unless sentence-initial, mis-capitalize after an
  abbreviation's period ("Mr."), and treat an ellipsis as a sentence end.
- **Hyphenated line breaks** are resolved without a dictionary: a short fragment that prefixes the
  next word is treated as a **stutter** and dropped (`"PR- PREPARE"` → `"PREPARE"`); otherwise the
  pieces are spliced (`"ACCOM- PLISH"` → `"ACCOMPLISH"`). A genuinely short real hyphenated
  fragment that happens to prefix the next word can be misread as a stutter — rare, accepted.
- **Confidence flagging:** lines whose ML Kit confidence is below **0.5** are shown **dimmed** as a
  gentle "maybe edit this" hint (clean lettering scores ~0.9+; smudged/small/stylised text drops
  below 0.5). It's only a hint — copy and translate are never blocked — and the dim clears once you
  edit the line. The threshold is tunable in code.
- **Stylised SFX / sound-effect lettering** is often misread; confidence flagging dims the worst of
  it, but expect noise on heavily stylised text.

## Scope

English → Arabic only for now. The source and target languages are parameters in the code, so the
feature is built to extend, but there's no in-app language picker yet.

Not yet built (backlog): per-page caching of OCR results/edits across a reading session, and
dedicated SFX filtering.
