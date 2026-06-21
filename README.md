# WiPear 🍐

(A vibe coding project)

A swipe-to-sort photo cleaner for Android. Go through your camera roll one card at a time — swipe **left** to mark for trash, swipe **right** to keep — then permanently delete the whole trash pile in one tap.

The name is a pun on *swipe* + *pear* (and yes, the pear branding is on purpose).

## Features

- **Three sources, one screen** — a toggle switches the swipe deck between **🖼 Photos**, **🎬 Videos**, and **📄 PDFs** without leaving the main screen.
- **Swipe deck** — items appear one at a time as cards. Sensitive, short-distance swipes.
- **Trash pile** — left-swipes pile up in a grid you can review before deleting (PDF cards show a rendered first-page thumbnail).
- **One-tap bulk delete** — photos/videos use Android's native `MediaStore` delete request (scoped-storage safe); PDFs are deleted directly via all-files access.
- **Date filter** — show only items inside a date range (works for all three sources).
- **Album / Folder filter** — multi-select which photo/video albums or PDF folders to include.
- **Sound effects** — short beeps for keep / trash / delete, with a persisted mute toggle.
- **Open** — jump straight to the current photo, video, or PDF in the relevant app.
- **Custom adaptive icon + splash screen.**
- **Green Material theme** end-to-end.

## Tech

- Java, Android View system (no Compose), Material Components
- AGP 8.13.2, Gradle KTS, version catalog
- `minSdk` 24, `targetSdk` 36
- Glide for image loading
- `androidx.core:core-splashscreen` for the splash
- `MediaStore.createDeleteRequest` (API 30+) / `RecoverableSecurityException` (API 29) / direct delete (older) for cross-version deletion

## Build

```bash
./gradlew :app:assembleDebug
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

To install on a connected device:

```bash
./gradlew :app:installDebug
```

## Permissions

- `READ_MEDIA_IMAGES` (Android 13+) / `READ_EXTERNAL_STORAGE` (older) — to list photos.
- `WRITE_EXTERNAL_STORAGE` (maxSdk 28) — for direct deletion on older Android.

Deletion on modern Android goes through the system delete-confirmation dialog; the app never deletes silently.

## License

No license set — all rights reserved by default. Add a `LICENSE` file if you want to open it up.
