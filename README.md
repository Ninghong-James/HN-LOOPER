# HN Loop Player

HN Loop Player is a native, offline Android app for MP3 audio and MP4 video. It uses Kotlin, Jetpack Compose, Media3 ExoPlayer, and a `MediaSessionService` for background playback and system media controls.

## Features

- Select multiple files through Android's system document picker. The app keeps read access to selected files, so playlists work after a restart.
- Create, rename, choose, and delete playlists; add, remove, and reorder their tracks.
- Play, pause, stop, skip, seek, set volume and speed, and cycle repeat off → one → all.
- Continue MP3 playback in the background with lock-screen and headset controls. ExoPlayer handles audio focus and headphones being unplugged.
- Save playlists, the selected playlist, last track, repeat mode, volume, and speed on the device.
- Show a useful error if a document has moved, its permission is gone, or its codec cannot be played. Remove that item and select it again.

No account, internet permission, analytics, ads, or network code is included. The system picker may show files from installed document providers; choose files stored on the device for fully offline playback.

## Build in Android Studio

1. Install Android Studio **Narwhal 3 Feature Drop (2025.1.3)** or newer, JDK 17, Android SDK Platform **36.1**, and SDK Build Tools **35.0.0** or newer. Gradle **8.13** is required by the pinned Android Gradle Plugin **8.13.2**.
2. Open this `HN-Loop-Player-source` directory as a project. Let Android Studio install missing SDK components and sync Gradle. The project includes the existing `gradle/wrapper/gradle-wrapper.properties` pinned to Gradle 8.13. If Android Studio opens it as a plain Java project, select **File → Open** and choose `settings.gradle.kts` to import it as a Gradle project. If it asks for a local Gradle installation, select Gradle 8.13. This environment did not have Gradle, so wrapper scripts and the wrapper JAR are not included.
3. Choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**. The installable debug APK will be at `app/build/outputs/apk/debug/app-debug.apk` after a successful build.

From a terminal with JDK 17, Android SDK, and Gradle 8.13 configured:

```sh
gradle :app:assembleDebug
```

Once a local Gradle 8.13 installation is available, you may generate standard wrapper scripts and the wrapper JAR with `gradle wrapper --gradle-version 8.13` and then use `./gradlew :app:assembleDebug`.

## Install on a physical Android device

Use a device running Android 8.0 (API 26) or later. Enable **Developer options → USB debugging**, connect the device, accept its authorization prompt, then run:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Alternatively, copy `app-debug.apk` to the device, tap it in the Files app, and allow that Files app to **Install unknown apps** when Android asks. Open HN Loop Player, tap **Add MP3 or MP4 files**, and choose locally stored documents.

## Notes

- Playback support for a particular MP4 codec depends on the device. The app accepts MP4 containers and reports playback errors when the device cannot decode one.
- Removing an entry from a playlist never deletes the original file.
- The app uses Android's Storage Access Framework. It does not request broad storage access.
