# RecoverX Store Checklist

1. Replace placeholder publisher contact information in `PRIVACY.md`.
2. Generate a signed release AAB from the release build.
3. Test install/uninstall/upgrade on Android 6, 10, 13, 14, 15 and 16-class devices.
4. Validate media permissions and selected-photo behavior on Android 14+.
5. Test system-trash restore on at least one Android 11+ device with images deliberately moved to trash.
6. Test SAF folder scan with internal storage and an SD card provider where available.
7. Test forensic carving using synthetic disk images containing JPEG/PNG files and false positives.
8. Fill Google Play Data Safety and permission declarations from the final release configuration.
9. Provide Arabic and English screenshots and store listing copy.
10. Never publish the app as claiming universal recovery of permanently deleted files.
