# Life Tracker (Android)

A native Android wrapper around the Life Tracker web app: income, expenses,
tasks and life events, tracked fully offline. All data lives in the app's
local storage on the device — nothing is ever sent over the network, which
is why this app asks for **zero permissions** (not even Internet).

The UI itself (`app/src/main/assets/www/index.html`) is one self-contained
HTML/CSS/JS file running inside a plain `WebView`. Two small pieces of
Kotlin (`MainActivity.kt`) connect it to real Android behaviour:

- **Save backup** → the page hands the app a JSON string, which is written
  wherever the user picks via Android's own "Save file" dialog.
- **Restore backup** → the page's existing file picker opens Android's
  document picker directly.

## Get a working .apk without installing anything

This repo already includes a GitHub Actions workflow
(`.github/workflows/build-apk.yml`) that builds a debug APK on GitHub's own
servers — you don't need Android Studio, the Android SDK, or Gradle on your
own computer.

1. Create a new **public or private** repository on GitHub.
2. Upload every file in this folder to it (drag-and-drop on github.com
   works, or `git push` if you're comfortable with git).
3. Open the **Actions** tab of the repository. A run named "Build APK"
   starts automatically; if it doesn't, click **Run workflow**.
4. When the run finishes (a couple of minutes), open it and scroll to
   **Artifacts** → download `LifeTracker-debug-apk`. It's a zip containing
   `app-debug.apk`.
5. Copy that `.apk` to your phone (email, Drive, USB — any way you like),
   open it, and allow "install unknown apps" for whichever app you opened
   it with. It installs and runs like any other app.

Every push to the repo rebuilds the APK automatically, so if you ask for
changes later, re-uploading the updated files gets you a fresh one the same
way.

## Building locally instead (optional)

If you do have Android Studio installed:

1. Open this folder as a project (**File → Open**).
2. Let it sync (first sync downloads the Android Gradle Plugin and the
   Kotlin plugin — needs internet once).
3. Run ▶ on a device or emulator, or **Build → Build Bundle(s) / APK(s) →
   Build APK(s)** to get a file under `app/build/outputs/apk/debug/`.

## Project layout

```
app/
  build.gradle.kts               module config (compileSdk 35, minSdk 26)
  src/main/
    AndroidManifest.xml          single activity, no permissions
    assets/www/index.html        the whole app: UI, logic, local storage
    java/.../MainActivity.kt     WebView host + backup save/restore bridge
    res/                         launcher icon, theme, colors, app name
.github/workflows/build-apk.yml  builds + uploads the APK on every push
```

## Notes

- **Package name**: `com.lifetracker.mobile`, set in
  `app/build.gradle.kts`. Fine as-is for personal use; if you ever publish
  this to the Play Store you'll want to change it to something under a
  domain you control, since it has to be globally unique.
- **minSdk 26** (Android 8.0, 2017+) — this covers effectively every phone
  in active use today, and lets the launcher icon be a plain vector
  drawable with no separate image files to manage.
- **Data**: stored in the WebView's local storage on the device. Use the
  in-app **Backup & data** screen regularly, especially before switching
  phones or reinstalling — that's the supported way to move your data.
