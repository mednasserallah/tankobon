<div align="center">

<img src="docs/assets/logo.png" alt="Tankobon" width="120" />

# Tankobon

### A local-first, volume-based manga reader for Android

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-0877d2)](/LICENSE)
[![Latest release](https://img.shields.io/github/v/release/mednasserallah/tankobon)](https://github.com/mednasserallah/tankobon/releases/latest)

</div>

Tankobon is an **unofficial fork** of [Mihon](https://github.com/mihonapp/mihon) (successor to
Tachiyomi) that reads manga you already own as local files, organized by **volume** instead of
chapter. No remote sources, no extensions — just your on-device library.

## Features

- **Local files only** — reads `.cbz` / `.zip` / `.cbr` / `.7z` / folders / EPUB from your device.
- **Volume-based** — one archive or folder is one bound volume, the way physical manga is shelved.
- **Smart file-name parsing** — understands volume numbers, publication years, editions, and
  omnibus ranges from a simple naming convention.
- **Per-volume covers** — a grid view with each volume's own cover, extracted from the archive.
- **On-device text detection & translation** — detect English text on a page and translate it to
  Arabic, fully offline and free.

## Fork attribution

Tankobon is built on the **Mihon** and **Tachiyomi** projects and is licensed under **Apache-2.0**.
It is an independent, unofficial fork and is **not** affiliated with or endorsed by the Mihon
project. The application hosts zero content and has no affiliation with any content providers.

## Documentation

| Guide | What's inside |
| --- | --- |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Module map, what the fork removed vs. added, roadmap status |
| [docs/LOCAL_LIBRARY_FORMAT.md](docs/LOCAL_LIBRARY_FORMAT.md) | The file-naming convention: volumes, editions, omnibus ranges, sidecars — with examples |
| [docs/COVERS.md](docs/COVERS.md) | Series vs. per-volume covers and how thumbnails are chosen/cached |
| [docs/TEXT_DETECTION.md](docs/TEXT_DETECTION.md) | On-device OCR + English→Arabic translation, and its known limits |
| [docs/BUILDING.md](docs/BUILDING.md) | Dev environment and how to build from source |
| [docs/RELEASING.md](docs/RELEASING.md) | The release process (signing, tagging, publishing) |
| [docs/BRANDING.md](docs/BRANDING.md) | App-icon assets and how to swap in a new logo |

## Building

Standard Android/Gradle project — see **[docs/BUILDING.md](docs/BUILDING.md)** to build from source.

## Contributing

Pull requests are welcome; for major changes, open an issue first. See
[CONTRIBUTING.md](CONTRIBUTING.md) and the [Code of Conduct](CODE_OF_CONDUCT.md).

## License

Tankobon builds on the work of the Mihon and Tachiyomi projects and remains licensed under the
Apache License, Version 2.0.

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
