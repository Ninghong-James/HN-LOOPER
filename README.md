# HN LOOPER

**A lightweight Android player for locally stored MP3 and MP4 files, with looping and playlists.**

HN LOOPER is a native Android project built with **Kotlin**, **Jetpack Compose**, and **AndroidX Media3 / ExoPlayer**. It focuses on convenient repeat playback, simple media management, and an offline-first experience.

> **Project status:** v1.0 milestone — first functional Android build, tested on an Android emulator and a physical Vivo X300 on **15 October 2026**. The current source still contains some previous “HN Loop Player” UI and Android display-name strings; the repository has been renamed to HN LOOPER, and an in-app branding update is pending.

## Features

- **MP3 and MP4 playback:** Select files using Android’s system document picker.
- **Repeat modes:** Off, repeat one, and repeat all.
- **Playback controls:** Play, pause, stop, skip, seek, adjust playback speed, and change volume.
- **Playlists:** Create, rename, select and delete playlists; add, remove and reorder tracks.
- **Background audio:** Media3 `MediaSessionService` provides playback beyond the app screen and integrates with system media controls.
- **Persistent preferences:** Save playlists, selected playlist, last track, repeat mode, playback speed and volume on the device.
- **Dark interface:** Jetpack Compose with a custom dark colour scheme.
- **Offline-first:** The Android manifest declares **no internet permission**; the app has no account, advertising or analytics integration.

### Media access and privacy

HN LOOPER uses Android's **Storage Access Framework** to let you select individual documents. It requests continued read access to selected files so that saved playlists can work after restarting the app. It does **not** request broad access to your device storage.

Playlist names, selected document URIs and media titles are stored in app-private Android preferences. The manifest disables Android's application backup for this app. HN LOOPER does not include network-transfer code or an internet permission. However, the Android system document picker may offer files from third-party or cloud-backed providers; choose on-device files for completely offline media access.

## Tech stack

| Component | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose / Material 3 |
| Media engine | AndroidX Media3 / ExoPlayer |
| Media controls / background service | Media3 `MediaSession` and `MediaSessionService` |
| File selection | Android Storage Access Framework |
| Playlist persistence | Android `SharedPreferences` |
| Build system | Android Gradle Plugin 8.13.2, Gradle 8.13, JDK 17 |
| Minimum Android version | Android 8.0 (API 26) |

## Build from source

1. Install **Android Studio**, **JDK 17**, Android SDK Platform **36.1**, and the SDK build tools required by your Android Studio setup.
2. Open the project folder that contains `settings.gradle.kts` in Android Studio.
3. Allow Android Studio to sync the project and resolve dependencies. The project specifies **Gradle 8.13** in `gradle/wrapper/gradle-wrapper.properties`.
4. Select **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. After a successful build, find the debug APK under `app/build/outputs/apk/debug/`.

**Note:** The initial repository contains `gradle-wrapper.properties`, but **does not contain `gradlew`, `gradlew.bat`, or `gradle-wrapper.jar`**. If you want to build from a terminal, you need a compatible local Gradle 8.13 installation or must generate and commit a standard Gradle wrapper first.

With Gradle 8.13 installed locally:

```sh
gradle :app:assembleDebug
```

### Install on Android

Transfer the generated debug APK to a phone you control and open it using the Files app, or install it using Android Debug Bridge:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

USB debugging is needed for the `adb` method, **not** for installing by opening a copied APK in the Files app. Install only APKs from sources you trust.

The debug APK is intended for development and personal testing. A publicly distributed release should use appropriate production signing and additional testing.

## Development milestone

### First functional build — 15 October 2026

- Created the Android project with AI-assisted development using OpenAI Codex.
- Resolved Android Studio / Gradle configuration and resource-compilation issues.
- Successfully built and launched the app in an Android emulator.
- Installed the APK on a Vivo X300; the developer reported that it worked as intended during initial use.
- Placed the project under Git version control and created its first repository.

This is the **first working milestone**, not a claim of comprehensive automated or security testing. The audited initial repository does not include automated tests.

## Roadmap

These are ideas for future releases, not features available in the current build:

- [ ] Finish in-app rebranding from “HN Loop Player” to **HN LOOPER**.
- [ ] A–B repeat between chosen timestamps.
- [ ] Sleep timer.
- [ ] Picture-in-picture video playback.
- [ ] Broader device testing and automated playback tests.
- [ ] Review and restrict external media-controller access where appropriate.

## Development and contributions

This project was developed using **AI-assisted code generation**, with human-led planning, Android Studio setup, debugging, testing and iteration.

The repository's licensing terms have **not yet been selected**. Public visibility by itself does not make the code open-source under a permissive licence. Please do not assume reuse rights unless a licence is added.

---

*HN LOOPER — Your media, on repeat.*
