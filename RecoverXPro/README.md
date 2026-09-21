# RecoverX Pro

RecoverX is a privacy-first Android photo recovery application built around **real, verifiable operations**. It does not fabricate scan percentages, fake recovered files, or claim that Android allows access it does not actually provide.

## Real capabilities in v1.0.0

- MediaStore image discovery.
- Android system-trash discovery where the MediaStore provider exposes trashed images (Android 11+).
- User-selected folder scanning through Storage Access Framework.
- Export recovered/accessible images to a user-selected destination.
- Restore MediaStore trash items through the Android system consent dialog.
- Deep forensic carving from a user-selected storage image/dump file: JPEG and PNG signatures are extracted into real files.
- Arabic RTL and English LTR UI.
- Dark/light Material 3 interface.
- Local-first privacy design.
- GitHub Actions build workflow for debug APK and release AAB.

## Important technical boundary

A normal Android application cannot simply read raw deleted blocks from every phone. Modern Android storage isolation, filesystem behavior, encryption, and device-specific implementations prevent a universal "recover everything" feature. RecoverX therefore reports only files it can actually access, and its deep-carving feature operates on a storage image/dump the user explicitly provides.

## Build

### GitHub Actions

Push the repository to GitHub. The workflow at `.github/workflows/android.yml` builds `app-debug.apk` and a release bundle artifact.

### Termux

On a device with JDK 17 and Gradle 9.6 available:

```bash
git clone <your-repo-url>
cd RecoverXPro
gradle assembleDebug
```

The recommended publishing artifact is an **Android App Bundle (`.aab`)**. Use a private release keystore; never commit signing keys or passwords.

## Store preparation

Before production release, complete:

- Real-device QA across representative Android 6–16+ devices.
- Privacy-policy hosting.
- Google Play Data safety form.
- App content declarations.
- Signed release AAB.
- Store icon, screenshots, feature graphic, and localized listing text.
- Verification of every permission declared in the Play Console.

## Permission model

- Android 13+: `READ_MEDIA_IMAGES` and selected-photo support on Android 14+.
- Android 12 and lower: `READ_EXTERNAL_STORAGE`.
- Folder scanning uses SAF so the user explicitly chooses the directory.

## License

Copyright © 2026 RecoverX. The project is prepared as a source distribution. Add your preferred open-source or proprietary license before public repository publication.
