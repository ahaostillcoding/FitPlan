# FitPlan Release Checklist

Use this checklist before sharing a debug or release candidate APK with internal testers.

## Build Outputs

- Debug APK: run `:app:assembleDebug`.
- Unsigned release APK: run `:app:assembleRelease` without `signing.properties`.
- Signed release APK: copy `signing.properties.example` to `signing.properties`, fill local keystore values, then run `:app:assembleRelease`.
- Never commit `signing.properties`, `.jks`, or `.keystore` files.

## Required Verification

- Run `:app:testDebugUnitTest`.
- Run `:app:lintDebug`.
- Install the debug APK on at least one real Android device.
- Launch the app after a fresh install and after an app process restart.

## Manual Regression

- Create a plan with at least two workout days and multiple exercises.
- Reorder workout days and exercises, save, and reopen the detail page.
- Select a workout day on the home page and confirm the source text updates.
- Start a workout, mark exercises complete, start/stop rest, leave and return to verify draft recovery.
- Discard a recovered draft and verify the session resets.
- Finish a workout and confirm history uses exercise snapshots.
- Filter history by all, this week, and this month.
- Export backup JSON, preview import, and confirm import only adds data.
- Configure DeepSeek API Key, test connection, generate a plan, edit preview, and save as a new plan.
- Disable network and confirm manual plans, workout execution, history, and backup still work.

## Known Limits

- No login or cloud sync.
- API Key is stored locally with DataStore, not encrypted storage.
- Release signing credentials are local-only and not part of the repository.
- History filters are local time based and intentionally simple.
