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
</content>
