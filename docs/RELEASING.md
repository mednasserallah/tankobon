# Releasing

The runbook for cutting a Tankobon release, so future releases don't have to be re-derived.
This process produced **v0.1.0** (the first release).

## Branch model

- `master` — release branch.
- `develop` — integration branch (branches off `master`).
- `feature/*` — feature branches (branch off `develop`).

Feature work merges into `develop`; releases merge `develop` into `master`.

## 1. Merge feature work into `develop`

```bash
git checkout develop && git pull
git merge --no-ff feature/<name>
./gradlew assembleDebug        # must build clean
git push origin develop
```

Delete each merged feature branch (`git branch -d …`, and the remote copy if it was pushed).

## 2. Bump the version

In `app/build.gradle.kts` (`defaultConfig`), set `versionCode` and `versionName`, then commit and
push on `develop`.

## 3. Merge `develop` into `master`

```bash
git checkout master && git pull
git merge --no-ff develop
./gradlew assembleDebug        # must build clean
git push origin master
```

Confirm `git diff --stat develop master` is empty (identical trees).

## 4. Signing keystore (one-time, permanent)

The release signing key is created **once, ever**. Losing it means no future build can update over
an existing install.

- Generate an RSA keystore (`tankobon-release.keystore`) at the repo root with `keytool`.
- Modern keystores are **PKCS12**, where the **key password is forced to equal the store
  password** — so there's effectively one password. Store it in a password manager; it is not
  recoverable.
- Create a git-ignored `keystore.properties` at the repo root:

  ```properties
  storeFile=../tankobon-release.keystore
  storePassword=<password>
  keyAlias=tankobon
  keyPassword=<password>          # same as storePassword for PKCS12
  ```

  `storeFile` is relative to the `app/` module, so `../tankobon-release.keystore` resolves to the
  repo root.
- **Both `tankobon-release.keystore` and `keystore.properties` are git-ignored — never commit
  them.** Confirm with `git check-ignore` and that they don't appear in `git status`.

`app/build.gradle.kts` already reads `keystore.properties` into the release signing config, so no
build-config change is needed — the presence of those two files is enough.

## 5. Build the signed release

```bash
./gradlew assembleRelease
```

Outputs are **ABI-split** APKs under `app/build/outputs/apk/release/` — there is **no** single
`app-release.apk`:

- `app-universal-release.apk` — works on any device (largest)
- `app-arm64-v8a-release.apk` — modern phones
- `app-armeabi-v7a-release.apk` — older 32-bit phones
- `app-x86-release.apk`, `app-x86_64-release.apk` — emulators

Verify the signature with `apksigner verify --print-certs <apk>` (the cert DN should be
`CN=Tankobon`).

## 6. Tag

```bash
git tag -a vX.Y.Z -m "Tankobon vX.Y.Z — …"
git push origin vX.Y.Z
```

> Heads-up: the repo inherits **old upstream tags** (e.g. a 2016 `v0.1.0` from Tachiyomi). If a tag
> name collides, delete the stale tag locally and on origin, then recreate it on the release commit.

## 7. Changelog + GitHub Release

- Add a version section to `CHANGELOG.md` (keep the inherited Mihon history below it).
- Publish the release (attach at least the universal + arm64 APKs). A concise notes file reads
  better than pasting the whole changelog:

  ```bash
  gh release create vX.Y.Z \
    app/build/outputs/apk/release/app-universal-release.apk \
    app/build/outputs/apk/release/app-arm64-v8a-release.apk \
    --title "Tankobon vX.Y.Z" --notes-file <notes.md> --latest
  ```

  (`gh auth login` first if the GitHub CLI isn't authenticated.)

## 8. Install / smoke-test

Install a signed APK on a device/emulator and confirm it launches:

```bash
adb install app/build/outputs/apk/release/app-arm64-v8a-release.apk
```

Because Tankobon's `applicationId` is `app.tankobon` (distinct from `app.mihon`), it installs
alongside other manga readers without conflict.
