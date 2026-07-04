# Branding & app icon

This document covers Tankobon's app-icon assets: what exists today, logo concept
directions for a future Tankobon-original mark, ready-to-paste image-generation
prompts, and the exact procedure for swapping in final artwork once it exists.

> **Status:** the current icon is still the **Mihon-derived** mark (a stylized
> hiragana "み"). No Tankobon-original artwork exists yet. Generating and swapping it
> in is a deliberate, user-driven step — see [Asset swap procedure](#asset-swap-procedure).

---

## Current icon assets (inventory)

Exact findings from the repo, so any generated replacement can be matched to the real
canvas sizes and slots.

### Adaptive icon (the launcher icon)

The app uses Android's **adaptive icon** system (separate foreground / background /
monochrome layers).

- **Config:** `app/src/main/res/mipmap/ic_launcher.xml` — an `<adaptive-icon>` referencing:
  - `<background>` → `@drawable/ic_launcher_background`
  - `<foreground>` → `@drawable/ic_launcher_foreground`
  - `<monochrome>` → `@drawable/ic_launcher_monochrome`
- **Manifest:** `AndroidManifest.xml` sets both `android:icon` and `android:roundIcon`
  to `@mipmap/ic_launcher`.
- **Note on location:** the config lives in the base `mipmap/` folder, *not* the
  conventional `mipmap-anydpi-v26/`. That's valid here because **`minSdk = 26`**
  (Android 8.0) — every supported device natively supports adaptive icons, so no
  API-qualified variant or legacy raster fallback is required.

### Layer drawables (all vector, all 108dp × 108dp canvas, `viewport 432×432`)

| Layer | File | Fill / palette |
| --- | --- | --- |
| Foreground | `app/src/main/res/drawable/ic_launcher_foreground.xml` | Single path, `#031019` (near-black navy) — the stylized "み" glyph |
| Background | `app/src/main/res/drawable/ic_launcher_background.xml` | Layered vector: base `#FAFAFA`, plus `#F2FAFF` and accent blue `#0058A0` (**not** a `colors.xml` value — baked into the drawable) |
| Monochrome | `app/src/main/res/drawable/ic_launcher_monochrome.xml` | `#FFFFFF` (system tints it for Android 13+ themed / Material You icons) — **a monochrome variant exists** |

- **Canvas / safe zone:** all three are a **108dp × 108dp** canvas (declared as a
  `432 × 432` viewport, i.e. 4× scale). The foreground content sits roughly within
  `x∈[169,263], y∈[171,261]` of the 432 viewport — comfortably inside the conventional
  **~66dp-diameter centered safe zone**. So the project **follows** the standard
  adaptive-icon convention (content confined to the center; outer margin is masked/cropped
  per-device).

### Debug-build variant

`app/src/debug/res/drawable/ic_launcher_background.xml` and `ic_launcher_foreground.xml`
override the two layers for debug builds with a **distinct dark palette**
(background `#2E3943` slate + `#F2FAFF`), so debug installs are visually distinguishable
from release. (No debug monochrome override — debug reuses the main monochrome.)

### Legacy raster launcher icons

**None exist, and none are needed.** The `mipmap-mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi`
folders contain only `ic_default_source.webp` and `ic_local_source.webp` (in-app source
-list icons, unrelated to the launcher). There is **no** pre-rendered `ic_launcher.png`
at any density, which is correct given `minSdk = 26`.

### Store / distribution metadata icons

**None exist.** There is no `fastlane/` directory, no
`fastlane/metadata/android/.../icon.png` (the usual 512×512 Play Store / F-Droid icon),
and no feature graphic (1024×500).

### Master / source design file

**No design-tool master** (`.ai`, `.sketch`, or a linked Figma) exists in the repo. Two
related, **Mihon-derived** raster/vector assets are present but are *not* the launcher
master:

- `.github/assets/logo.png` — **512 × 512** PNG, used as the repository / README logo.
- `.idea/icon.svg` — **512 × 512** (viewBox `432×432`), the **Android Studio / IntelliJ
  project icon** shown in the IDE project switcher (a dark `#2e3943` circle with a light
  "み"). Not the app launcher.

### Separate in-app brand marks (not the launcher)

`app/src/main/res/drawable/ic_mihon.xml` (256dp, `viewport 148×141`) and
`ic_mihon_splash.xml` drive in-app branding / the splash screen. These are tracked as a
deferred rename in `CLAUDE.md` (Phase B) and are **out of scope** for the launcher-icon
swap, but should eventually be replaced too for a full rebrand.

### Summary of what a full icon replacement must cover

1. `drawable/ic_launcher_foreground.xml` (+ debug variant)
2. `drawable/ic_launcher_background.xml` (+ debug variant)
3. `drawable/ic_launcher_monochrome.xml`
4. `.github/assets/logo.png` (repo/README logo, 512×512)
5. `.idea/icon.svg` (IDE project icon — cosmetic, optional)
6. *(new)* a store/F-Droid metadata icon, if distribution there is ever set up
7. *(separately, deferred)* `ic_mihon.xml` / `ic_mihon_splash.xml` in-app marks

There are **no** legacy density PNGs to regenerate (adaptive-icon-only, `minSdk 26`).

---

## Logo concepts & image-generation prompts

The direction here follows **Mihon's own mark**: Mihon's icon is a single **hiragana "み"**
(the first kana of みほん / *Mihon*) — a Japanese-character logomark, not a picture of a book.
Tankobon should do the same: a **single bold Japanese character** for 単行本 (*tankōbon*). This
keeps the visual lineage of the fork, reads unmistakably as *manga* (not a Western novel), and
scales to a launcher icon far better than any illustrated object.

> **On the "no lettering" rule:** the earlier guidance to avoid embedded text was about *words
> and taglines*. A **single glyph that IS the logo** — exactly as Mihon uses "み" — is the
> intended exception. Still: no words, no Latin letters, no tagline.

> **⚠️ Reliability note — read before generating.** AI image tools frequently **distort or
> invent** kanji/kana strokes. For a single-character mark the **most reliable route is not image
> generation at all**: set the exact Unicode character below in a **bold Japanese typeface**
> (a heavy Gothic/ゴシック like *Noto Sans JP Black*, or a brush/mincho face for a calligraphic
> look), export at high resolution, then vectorize/clean in VectorMagic. Use the image prompts
> below only for a *stylized/brush* starting point — and **verify the character is exactly right
> and undistorted** before committing (compare against the character shown here).

**Shared guidance (same rationale as before):** 1024×1024 1:1; flat, high-contrast, bold strokes
(thin strokes vanish at 48dp); the glyph **centered with ~20% margin** so an icon mask
(circle/squircle) never clips it; a single flat or transparent background to split into
foreground/background layers afterward. Palette continuity with today's icon: deep navy `#031019`
/ accent blue `#0058A0` / off-white `#FAFAFA` — either a dark glyph on a light field, or (like the
IDE icon) a light glyph on a navy/blue circle.

### Concept A — 「本」 (*hon*, "book") — recommended

The kanji **本** means "book / volume" and is the **final character of 単行本**. It's the most
meaningful choice — it literally names what the app is for — and one of the cleanest, best-balanced
kanji: a few thick strokes with a distinctive wide horizontal base. It parallels Mihon's
single-glyph mark, but carries *meaning*, not just sound.

- **Small-size hold-up:** **Excellent.** Few, thick strokes; the base bar and central vertical stay
  legible at 48dp. Use a heavy weight so nothing thins out.

> **Prompt:** A minimalist flat app icon whose only element is the single Japanese kanji
> character 本 (meaning "book"), centered as a bold logomark in a clean brush-inspired style.
> Render the character accurately and undistorted with thick, high-contrast strokes. No other
> text, no Latin letters, no fine detail, no gradients. An off-white character on a solid
> deep-navy rounded-square field with a subtle accent-blue edge. The character is centered with
> generous margin so it is never clipped by a circular or squircle mask. Flat, iconic, modern,
> a manga-app feel. Square 1:1, 1024×1024, high resolution.

### Concept B — 「た」 (hiragana *ta*) — closest Mihon parallel

The first hiragana of **たんこうぼん**. This is the nearest structural echo of Mihon's soft, rounded
「み」 — a single friendly, approachable hiragana glyph, phonetically tied to the name.

- **Small-size hold-up:** **Very good.** One flowing stroke group; keep it bold and keep the small
  internal loop/gap open so it doesn't fill in when shrunk.

> **Prompt:** A minimalist flat app icon whose only element is the single Japanese hiragana
> character た, centered as a bold logomark in a clean brush-inspired style. Render the character
> accurately and undistorted, thick high-contrast strokes, keeping its open counter clear. No other
> text, no Latin letters, no fine detail, no gradients. A deep-navy character on a flat off-white
> background (or inverted: off-white on navy). Centered with generous margin so it is not clipped by
> an icon mask. Flat, iconic, friendly, modern. Square 1:1, 1024×1024, high resolution.

### Concept C — 「タ」 (katakana *ta*) — modern / angular

Katakana **タ** — the same sound as た, but angular and geometric. Katakana carries a punchy,
stylized, contemporary feel (think manga titling and sound effects), and its straight strokes are
extremely clean at tiny sizes.

- **Small-size hold-up:** **Excellent.** Just two or three angled strokes — about as legible as a
  glyph gets when shrunk.

> **Prompt:** A minimalist flat app icon whose only element is the single Japanese katakana
> character タ, centered as a bold geometric logomark. Render the character accurately and
> undistorted, thick high-contrast straight strokes, sharp clean corners. No other text, no Latin
> letters, no fine detail, no gradients. A bright accent-blue character on a flat off-white
> background, or off-white on deep navy. Centered with generous margin so it is not clipped by a
> circular or squircle mask. Flat, modern, punchy, iconic. Square 1:1, 1024×1024, high resolution.

### Concept D — 「単」 (kanji *tan*) — distinctive but busier

The **first kanji of 単行本**. More formal and distinctive, but it has more strokes (a boxed top with
internal bars over a vertical), so it's the busiest option.

- **Small-size hold-up:** **Moderate.** The stacked internal horizontals can merge below ~48dp. Use
  a very heavy weight, and prefer this only if the icon is usually seen larger.

> **Prompt:** A minimalist flat app icon whose only element is the single Japanese kanji character
> 単, centered as a bold logomark in a clean heavy style. Render the character accurately and
> undistorted with thick, high-contrast strokes and clear spacing between the internal horizontal
> strokes so they don't merge. No other text, no Latin letters, no gradients. An off-white character
> on a solid deep-navy rounded-square field. Centered with generous margin so it is not clipped by
> an icon mask. Flat, iconic, modern. Square 1:1, 1024×1024, high resolution.

### Picking

**本 (A)** is the strongest recommendation — it *means* "book/volume," is part of the name, and is
clean at any size. **た (B)** is the truest parallel to Mihon's kana mark; **タ (C)** if you want a
sharper, more modern feel; **単 (D)** only if you accept more detail. Whichever you choose, prefer
the **font-typeset → VectorMagic** route over raw image generation for a crisp, correct glyph.

---

## Asset swap procedure

> Nothing here is done yet — this section documents *how* to swap in the final mark once it
> exists. The current Mihon-derived icons are **left untouched** until real Tankobon art is ready.

### 0. Get a clean master image

1. Generate the mark in ChatGPT using one of the prompts above (1024×1024).
2. Clean it up and vectorize it with **[VectorMagic](https://vectormagic.com/)** — trace the
   raster into a crisp **SVG**. Flatten to a small number of solid colors; remove stray specks.
3. If the concept has a distinct subject and backdrop, keep them on **separate layers** (or
   export two SVGs: a transparent-background **foreground** and a **background** fill/graphic),
   since the adaptive icon needs them split.

### 1. Regenerate the launcher icon with Image Asset Studio (easiest path)

The **easiest** way to produce every icon slot is Android Studio's **Image Asset Studio** —
it regenerates all buckets from one high-resolution source, so you don't hand-produce anything:

1. In Android Studio: right-click the `app/src/main/res` folder → **New → Image Asset**.
2. Icon type **Launcher Icons (Adaptive and Legacy)**.
3. **Foreground layer:** select your foreground SVG/PNG; nudge scale so the subject sits
   inside the safe zone (the tool shows the circle/squircle mask previews live).
4. **Background layer:** pick your background SVG, or a solid color (today's is off-white
   `#FAFAFA` with an accent, baked into `ic_launcher_background.xml`).
5. **Monochrome / Legacy tabs:** provide the monochrome silhouette (Android 13+ themed icons)
   — this project **has** a monochrome layer, so keep one.
6. Finish. It overwrites `ic_launcher_foreground.xml`, `ic_launcher_background.xml`,
   `ic_launcher_monochrome.xml`, and `mipmap/ic_launcher.xml` as needed.

> Keep every layer on the **108dp × 108dp** canvas (`viewport 432×432`) with content in the
> central **~66dp safe zone** — matching the existing files documented above.

### 2. Files to replace (checklist)

If editing by hand instead of Image Asset Studio, these are the exact slots — see the
[inventory](#current-icon-assets-inventory) for full paths:

- [ ] `app/src/main/res/drawable/ic_launcher_foreground.xml` — adaptive foreground
- [ ] `app/src/main/res/drawable/ic_launcher_background.xml` — adaptive background
- [ ] `app/src/main/res/drawable/ic_launcher_monochrome.xml` — themed-icon monochrome
- [ ] `app/src/debug/res/drawable/ic_launcher_foreground.xml` — debug-variant foreground
- [ ] `app/src/debug/res/drawable/ic_launcher_background.xml` — debug-variant background
      (keep the debug palette distinct from release so debug installs stay recognizable)
- [ ] `.github/assets/logo.png` — repo / README logo (512×512)
- [ ] `.idea/icon.svg` — IDE project icon (cosmetic; optional)
- [ ] *(create if publishing)* store / F-Droid metadata icon (512×512) + feature graphic
      (1024×500), e.g. under `fastlane/metadata/android/en-US/images/`
- [ ] *(deferred, separate rebrand step)* the in-app marks `ic_mihon.xml` /
      `ic_mihon_splash.xml` and their references

**No legacy density PNGs** need regenerating — the app is adaptive-icon-only (`minSdk 26`).

### 3. Verify

- Build and install; check the launcher icon on the home screen and in the app switcher.
- Toggle a **themed (Material You)** icon on Android 13+ to confirm the monochrome layer.
- Confirm the subject isn't clipped under **circle**, **squircle**, and **rounded-square**
  masks (Image Asset Studio previews these; a device/emulator confirms).
- Confirm the **debug** build shows its distinct variant.
</content>
