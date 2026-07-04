<div align="center">

# Tankobon

### A local-first, volume-based manga reader for Android

Tankobon is an **unofficial fork** of [Mihon](https://github.com/mihonapp/mihon) (itself the successor to Tachiyomi) focused on reading manga you already own as local files, organized by **volume** rather than chapter.

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-0877d2)](/LICENSE)

</div>

## About this fork

Tankobon deliberately narrows Mihon's scope. Instead of a general-purpose reader with remote sources and installable extensions, Tankobon aims to be a focused reader for a personal, on-device manga library.

> **Work in progress.** This fork is under active development and is **not** feature-complete. Expect breaking changes. Some Mihon features are still present but are being removed or reworked (see the roadmap below).

## Roadmap

The direction of this fork differs from upstream Mihon:

- **Local files only.** The extension system (remote sources, extension repos, installer/updater) is being **removed** entirely. Tankobon reads content stored on your device.
- **Volume-based organization.** The local parser is being reworked so that each **volume** is the atomic reading unit, using an opinionated file-naming convention:

  ```
  Series Name/
    ├─ Series Name - Volume 01 (Year).cbz
    ├─ Series Name - Volume 02 (Year).cbz
    ├─ cover.jpg        (optional, ignored)
    └─ details.json     (optional, ignored)
  ```

  Single-volume/one-shot titles (e.g. `Boy Meets Maria (2021).cbz`) and series names that themselves contain parentheses (e.g. `BLAME! (Master Edition)`) are handled as special cases.

Neither of these is complete yet.

## Features (current)

Inherited from Mihon and still available while the rework is in progress:

* Local reading of content from `.cbz`/archive files.
* A configurable reader with multiple viewers, reading directions and other settings.
* Categories to organize your library.
* Light and dark themes.
* Create and restore local backups.

## Building

Standard Android/Gradle project. To build a debug APK:

```
./gradlew assembleDebug
```

Requires a JDK matching `.github/.java-version` and Android SDK. Android Studio is recommended. See [CONTRIBUTING.md](./CONTRIBUTING.md) for details.

## Contributing

[Code of conduct](./CODE_OF_CONDUCT.md) · [Contributing guide](./CONTRIBUTING.md)

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## Disclaimer

The developer(s) of this application do not have any affiliation with any content providers, and this application hosts zero content. Tankobon is an independent, unofficial fork and is **not** affiliated with or endorsed by the Mihon project.

## License

Tankobon builds on the work of the Mihon and Tachiyomi projects and remains licensed under the Apache License, Version 2.0.

<pre>
Copyright © 2015 Javier Tomás
Copyright © 2024 Mihon Open Source Project
Copyright © 2026 Tankobon Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
</pre>
</content>
</invoke>
