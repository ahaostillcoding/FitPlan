# FitPlan Beta Feedback Log

Use this log for Stage 9 internal testing. Keep one row per issue so fixes can be traced to commits and verification results.

## Priority Rules

- P0: Crash, data loss, app cannot start, or core offline flow is blocked.
- P1: Major flow friction, confusing confirmation, backup/AI failure handling, or serious layout issue on real devices.
- P2: Copy polish, minor visual alignment, nice-to-have accessibility or documentation improvement.

## Feedback Items

| ID | Priority | Area | Status | Summary | Repro Steps | Owner / Fix Commit | Verification |
| --- | --- | --- | --- | --- | --- | --- | --- |
| BETA-001 | P1 | Release | Fixed | Stage 9 needs a single place to track feedback and RC readiness. | Review repository docs after Stage 8. | Stage 9 docs commit | Confirm this file and `docs/1.0-rc-notes.md` exist. |
| BETA-002 | P1 | Backup | Fixed | Import result should say how many plans/records were added and skipped. | Import a backup JSON from Settings. | Backup summary commit | `SettingsViewModelTest.importBackup_setsResultSummaryWithSkippedCounts` passed. |
| BETA-003 | P1 | Workout | Fixed | Old workout draft should be called out before restoring. | Start a workout, leave draft for a long time, reopen same day. | Draft expiry commit | `WorkoutSessionViewModelTest.expiredDraftClearsSavedDraftAndStartsFresh` passed. |
| BETA-004 | P2 | UI | Planned | Stage 9 HTML prototype should mention RC readiness and beta fixes. | Open `ui-preview/index.html`. | Pending | Verify title and sidebar content. |

## Verification Notes

- Run `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleRelease`, and `:app:lintDebug` before marking Stage 9 complete.
- Install the debug APK on a real Android device and complete the first-run sample plan flow.
- Keep release signing credentials local. Do not paste API Keys, keystore passwords, or personal data into this log.
