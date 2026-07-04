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

Four distinct directions for a **Tankobon-original** mark. "Tankobon" (単行本) is the
Japanese term for a single **bound, collected volume** of manga — so every concept leans
on the physical *volume* as the subject, which also matches the app's volume-based model.

Each concept below has a ready-to-paste prompt for ChatGPT's image tool (you run it — I
can't generate images). After generating, separate the result into adaptive-icon
foreground/background layers (e.g. via VectorMagic to vectorize, then split), then follow
[Asset swap procedure](#asset-swap-procedure).

**Shared guidance baked into every prompt (why):**
- **1024 × 1024, 1:1** — high-res source downscales cleanly to 48dp; the reverse never works.
- **Flat, high-contrast, minimal** — launcher icons render as small as **48dp**. Big flat
  shapes survive shrinking; gradients-as-the-only-cue, thin linework, and fine detail vanish.
- **No text or lettering** — embedded text doesn't scale and can't be localized.
- **Centered subject with ~20% margin** — Android masks the icon to a circle/squircle/etc.
  per device; keeping the subject in the central safe zone prevents clipping.
- **Clean flat or transparent background** — you'll split foreground from background afterward.
- Optional palette continuity with today's icon: deep navy `#031019` / accent blue `#0058A0`
  on off-white `#FAFAFA`. Prompts mention it as a suggestion — swap freely.

### Concept A — "Volume Stack"

A small, tidy **stack of three book volumes** seen straight-on (a short shelf run or a neat
pile). It reads immediately as *a collection of volumes* — the heart of a volume-based
library. Distinct blocks with clear separation give a strong, recognizable silhouette.

- **Small-size hold-up:** **Excellent.** Three solid blocks, high contrast, no fine detail.
  Cap at three books and avoid drawing spine text or thin page lines — those disappear at 48dp.

> **Prompt:** A minimalist flat vector app icon of a small neat stack of three bound
> manga book volumes viewed straight on, slightly offset like a tidy pile. Bold simple
> rounded-rectangle shapes, high contrast, no gradients, no text or lettering, no fine
> detail. Deep navy and a single accent blue on a flat off-white background. The stack is
> centered with generous empty margin around all edges so nothing is clipped. Clean, iconic,
> modern, geometric. Square 1:1 composition, 1024×1024, high resolution.

### Concept B — "Bound Volume"

A **single closed tankobon** shown at a subtle 3/4 angle (or pure side profile) that reveals
the rounded **bound spine** and the page block. This is the literal object the app is named
for — one iconic, confident shape rather than a scene.

- **Small-size hold-up:** **Very good**, if reduced to 2–3 flat planes (cover face, spine,
  page-edge). Keep the page block as one solid shape — no individual page striations.

> **Prompt:** A minimalist flat vector app icon of a single closed manga volume (a
> Japanese tankobon) shown at a gentle three-quarter angle, revealing the rounded bound
> spine and a solid page-edge block. Two or three bold flat color planes only — cover,
> spine, pages — high contrast, no gradients, no text or lettering, no fine line detail.
> Deep navy cover with an accent-blue spine on a flat off-white background. The book is
> centered with generous margin so it is never clipped by a circular or rounded mask.
> Clean, iconic, geometric. Square 1:1 composition, 1024×1024, high resolution.

### Concept C — "Bookmark Ribbon"

A **single volume with a bookmark ribbon** tab dropping from the top edge. It fuses the two
things the app is about — *collecting* volumes and *reading* them — and the ribbon gives a
natural spot for a bold accent color and an asymmetric, memorable focal point.

- **Small-size hold-up:** **Good.** The book + ribbon are two bold shapes. Make the ribbon
  wide and short (not a thin string) so it stays legible at 48dp; a forked/notched ribbon
  end is fine if kept chunky.

> **Prompt:** A minimalist flat vector app icon of a single bound manga volume shown
> straight on as a rounded rectangle, with a bold chunky bookmark ribbon hanging from its
> top edge. Two simple flat shapes only — book and ribbon — high contrast, no gradients, no
> text or lettering, no fine detail. Deep navy book with a bright accent-blue ribbon on a
> flat off-white background. Composition centered with generous margin on all sides so it is
> not clipped by an icon mask. Clean, modern, iconic, geometric. Square 1:1, 1024×1024, high
> resolution.

### Concept D — "Covers Grid"

An abstract **2×2 grid of rounded squares** — echoing Tankobon's signature **per-volume
covers grid view**. It's the most "app-native" and modern of the four, and instantly
distinct from Mihon's glyph mark. One tile can carry the accent color to create a focal point.

- **Small-size hold-up:** **Excellent.** Evenly weighted geometry with generous gutters
  reads cleanly at any size — just keep the gaps between tiles thick so they don't merge when
  shrunk.

> **Prompt:** A minimalist flat vector app icon of an abstract 2 by 2 grid of four rounded
> squares with thick even gaps between them, evoking a grid of manga volume covers. Bold flat
> shapes, high contrast, no gradients, no text or lettering, no fine detail. Three tiles in
> deep navy and one tile in a bright accent blue, on a flat off-white background. The grid is
> centered with generous margin around all edges so it is not clipped by a circular or
> squircle icon mask. Clean, modern, geometric, app-like. Square 1:1, 1024×1024, high
> resolution.

### Picking / blending

Run whichever resonates. **A** and **D** are the safest bets for small-size legibility;
**B** is the most literal to the name; **C** is the most distinctive focal mark. Elements
combine well — e.g. a **Bound Volume (B)** with a **Bookmark Ribbon (C)**, or a **Volume
Stack (A)** rendered as grid tiles (**D**). Ask if you'd like a blended prompt.

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
