# T Minus

[![CI](https://github.com/alex-straw/t-minus/actions/workflows/ci.yml/badge.svg)](https://github.com/alex-straw/t-minus/actions/workflows/ci.yml)

A date to count towards.

`T-364`

T Minus puts a minimal countdown to a meaningful future date on your Android lock screen. It uses local calendar days, updates shortly after midnight, and stops automatically if you replace the wallpaper.

The default target is 8 August 2027. It is a personal default, not a claim of scientific consensus.

## Install

1. Download [`T.apk`](https://github.com/alex-straw/t-minus/releases/latest/download/T.apk) and [`T.apk.sha256`](https://github.com/alex-straw/t-minus/releases/latest/download/T.apk.sha256).
2. Verify the checksum.
3. Open `T.apk` on an Android device and allow installation from the browser or file manager when Android asks.
4. Open T Minus, choose a date, and select **Set as lock screen**.

PowerShell checksum verification:

```powershell
(Get-FileHash .\T.apk -Algorithm SHA256).Hash.ToLowerInvariant()
Get-Content .\T.apk.sha256
```

macOS or Linux checksum verification:

```sh
sha256sum -c T.apk.sha256
```

Android may warn that the app came from outside an app store. Only install release files downloaded from this repository.

## Privacy

T Minus has no network access, analytics, accounts, advertising, or data collection. It requests no `INTERNET` or runtime permission. Its only app-facing capability is `SET_WALLPAPER`; WorkManager adds normal wake-lock and reboot permissions so daily updates survive process death and device restarts. The selected date and wallpaper state stay in private storage on the device.

## Build

Requirements:

- JDK 17
- Android SDK Platform 36 and Build Tools 36.0.0

Run:

```powershell
.\gradlew.bat test lint assembleDebug
```

The debug APK is written beneath `app/build/outputs/apk/debug/`.

## Release signing

Create one dedicated signing key before the first public release. Keep it private and back it up: future versions must use the same key or Android will not install them as updates.

```powershell
keytool -genkeypair -v -keystore release/T-release.jks -alias t-release -keyalg RSA -keysize 4096 -validity 10000
Copy-Item keystore.properties.example keystore.properties
```

Edit the ignored `keystore.properties` with the keystore location and credentials. Never commit the keystore or that properties file.

Produce the signed APK and checksum with:

```powershell
.\scripts\prepare-release.ps1
```

Upload `release/T.apk` and `release/T.apk.sha256` to a GitHub Release manually.

## Scope

Version 0.1 intentionally has no notifications, widgets, live wallpaper, themes, accounts, backend, analytics, or ads. See [project_plan.MD](project_plan.MD) for the specification and acceptance checklist.
