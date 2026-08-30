# T v0.1 Project Plan

## Locked identity

- Product/display name: `T`
- Repository: `alex-straw/t-minus`
- Application ID/package: `io.github.alexstraw.tminus`
- Version: `0.1.0` (`versionCode` 1)
- License: MIT
- Default target: 08 August 2027
- Language: English only in v0.1
- Launcher mark: the supplied white `T-` symbol on pure black, implemented as vector resources

Explanatory copy:

> I think AGI could make the world unrecognisable within a year. Nobody knows what comes next. T is a reminder to live intentionally until then.

The GitHub username contains a hyphen, which is invalid in a Kotlin package segment, so `alexstraw` is the normalized package owner.

## Build foundation

1. Create a single-module Android project in the existing repository.
2. Use Kotlin, Kotlin DSL, Android Views/XML, and JDK 17.
3. Pin AGP 9.3.0, Gradle 9.5.0, and WorkManager 2.11.2.
4. Use AGP 9 built-in Kotlin support; do not apply `org.jetbrains.kotlin.android`.
5. Configure `minSdk 24`, `compileSdk 36`, and `targetSdk 36`.
6. Enable Java API desugaring so countdown logic can use `LocalDate`.
7. Enable R8 and resource shrinking for release builds.
8. Ignore Android Studio, Gradle, local SDK, signing, APK, and build output files.

## Application structure

Keep the implementation small:

```text
MainActivity.kt
Countdown.kt
TApplication.kt
TPreferences.kt
WallpaperRenderer.kt
WallpaperPreviewView.kt
WallpaperController.kt
UpdateScheduler.kt
WallpaperUpdateWorker.kt
res/layout/activity_main.xml
```

Do not introduce Compose, fragments, ViewModels, dependency injection, repositories, navigation, DataStore, or a database.

## Countdown rules

Implement one pure function taking `today: LocalDate` and `target: LocalDate`:

- Future: `T-N`
- Today: `T`
- Past: `T+N`
- Calendar days only, calculated with `ChronoUnit.DAYS`
- No time-of-day, hours, minutes, or seconds

Do not read the clock inside this function.

## Persisted state

Store exactly these values in private `SharedPreferences`:

- `targetEpochDay`
- `active`
- `lastWallpaperId`
- `lastRenderedEpochDay`

A date selected in the picker remains an in-memory draft, restored through `savedInstanceState`. Persist it only after the user successfully applies the wallpaper.

`lastRenderedEpochDay` prevents a full-resolution bitmap render and wallpaper write when the active wallpaper is already correct for the current local date.

WorkManager uses on-demand initialization. Opening the app on a current day performs only the wallpaper-ID/date check; WorkManager initializes when applying, cancelling, or executing persistent work.

## Wallpaper rendering

- Background: `#000000`
- Text: `#F2F2F2`, anti-aliased
- Typeface: system monospace, normal weight
- Text size: `bitmapWidth * 0.03f`
- Horizontal centre: `bitmapWidth / 2`
- Visual text centre: `bitmapHeight * 0.40f`, adjusted using font metrics
- Bitmap dimensions: at least both the display and `WallpaperManager` desired minimum dimensions
- Apply using `setBitmap(bitmap, null, false, FLAG_LOCK)`
- Treat a returned wallpaper ID of zero as failure
- Recycle obsolete preview and render bitmaps

The preview must use the same renderer and aspect ratio as the applied bitmap.

## One-screen UI

Visual system: pure black throughout, system monospace text, off-white primary text, muted grey secondary text, and restrained terminal green for actions. Controls use thin square-edged outlines with no shadows or bundled fonts.

Inactive state:

- `T`
- `A date to count towards.`
- Formatted target date and `Change`
- Wallpaper preview
- `Set as lock screen`
- The agreed explanatory copy

Active, unchanged state:

- Title, date, preview
- `Change date`
- `Stop updating`
- Keep the explanatory copy visible below the controls on the same scrollable page

Active with a draft date:

- Show `Update lock screen`
- Keep `Stop updating`

Use the platform `DatePickerDialog`. While applying, disable controls and show an indeterminate progress indicator. Perform wallpaper I/O off the main thread.

## State transitions

- Successful Apply: set wallpaper, persist target/ID/active, then schedule work.
- Failed initial Apply: persist nothing and show the agreed error.
- Stop: set inactive, cancel all T work, and leave the current wallpaper untouched.
- App opened while active: compare wallpaper IDs first. If they match, refresh it; if not, deactivate and never write.
- Manual date changes affect only the preview until Apply succeeds.
- Unsupported or policy-blocked devices show `Couldn't change the lock screen wallpaper on this device.`
- Other failures show `Couldn't update T.`

## Daily updating

Schedule a one-time worker for the next local midnight plus five minutes.

- Give all T jobs one common tag.
- Give each scheduled date a unique work name to prevent duplicates.
- Check `active` and the wallpaper ID before rendering.
- An ID mismatch deactivates T and finishes without writing.
- Recalculate using the actual local date on every run.
- Persist the new wallpaper ID after success.
- Schedule the next local midnight only after success.
- Retry transient rendering or I/O failures.
- Do not add exact alarms, boot receivers, foreground services, or network constraints.

## Automated verification

Add unit tests for:

- Future, tomorrow, today, yesterday, and historical targets
- Leap days and month/year boundaries
- DST scheduling in `Europe/London`
- Matching wallpaper ID permits an update
- Mismatched wallpaper ID deactivates without applying
- Failed Apply does not activate or replace the saved target

Run `assembleDebug`, unit tests, lint, and `assembleRelease`.

## Samsung A16 acceptance

1. Fresh install uses the default date.
2. Preview matches the lock screen.
3. The home-screen wallpaper remains unchanged.
4. The date changes after midnight.
5. Reboot preserves scheduled updates.
6. Opening the app corrects a stale countdown.
7. Replacing the wallpaper disables T without overwriting it.
8. Stop cancels updates but leaves the wallpaper visible.
9. A new target is not applied before confirmation.
10. There are no runtime permissions and no `INTERNET` permission.

Only after these pass should the 3% text size and 40% vertical position be tuned, with any change recorded as constants.

## Release

Configure release signing through an ignored `keystore.properties`, but do not create or commit a signing key or password. The owner creates and backs up the release keystore separately.

After signing is configured:

1. Build `T.apk`.
2. Generate `T.apk.sha256`.
3. Verify a higher-version APK installs over the first build.
4. Upload both files manually to GitHub Releases.

Add GitHub Actions CI that runs unit tests, Android lint, and a debug build on pushes and pull requests. The README must contain the product copy, direct APK link, checksum verification, sideload instructions, privacy statement, build instructions, and signing-key continuity warning.

## Definition of done

- Fresh install works and uses 08 August 2027.
- Countdown semantics and boundary cases are tested.
- Preview and lock-screen rendering agree.
- Daily work survives reboot and recalculates from the current local date.
- A user-selected replacement wallpaper is never overwritten.
- Changing the target and stopping updates work as specified.
- The source manifest requests only the `SET_WALLPAPER` app capability. The merged APK has no `INTERNET` or runtime permissions; only WorkManager's normal wake-lock and reboot permissions may be added for persistent work.
- Release shrinking is enabled.
- A signed update installs over the previous signed version.
- README installation and release instructions are complete.

## Execution status

Completed on 09 August 2026:

- Android project, wrapper, application code, resources, tests, release configuration, and documentation implemented.
- 17 unit tests pass with no failures.
- Android lint passes with zero errors. Its three remaining warnings are the deliberately locked API 36 target/compile SDK and Gradle 9.5.0 version.
- Debug APK and unsigned, R8/resource-shrunk release APK build successfully.
- Merged APK manifest verified to contain no `INTERNET`, network-state, foreground-service, or runtime permission.
- WorkManager's required normal `WAKE_LOCK` and `RECEIVE_BOOT_COMPLETED` permissions remain for persistent work.

Pending owner/device gates:

- Complete the remaining Samsung A16 acceptance checks: replacement detection, Stop, reboot persistence, and an unconfirmed date change.
- Create and back up the owner-controlled release keystore.
- Build the signed `T.apk` and checksum.
- Verify a signed higher-version APK installs over the first signed build.
- Publish the GitHub Release manually.

Samsung A16 results recorded on 09 August 2026:

- Debug APK installed and cold-launched successfully on an SM-A165F.
- Default target and `T-364` preview were correct.
- Lock-screen wallpaper applied with the intended text size and 40% vertical position.
- Home-screen wallpaper remained unchanged.
- App-open self-correction refreshed the wallpaper ID and restored the midnight schedule.
- The background worker was forced through JobScheduler and completed successfully without a crash.
- WorkManager scheduled the next run for approximately 00:05 local time.
- The active screen keeps the introductory copy reachable below the controls with no separate page or hidden state.
- The preview draws directly into its small view instead of allocating a phone-resolution bitmap.
- Same-day app opens validate the wallpaper ID/date without rendering or rewriting the wallpaper.
- Representative same-day cold starts measured 1.63s, 1.00s, and 0.96s on the A16 after installation settled.
- Steady debug-process PSS measured about 78 MB, down from the 149 MB pre-optimization baseline.
