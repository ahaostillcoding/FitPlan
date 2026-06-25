# FitPlan Phase 8 Internal Test Guide

Use this guide when sharing a debug APK with internal testers.

## First Run

- Fresh install the app.
- Verify the home empty state offers manual creation, AI generation, and sample plan creation.
- Create the sample plan and confirm it becomes the active plan.
- Start the first workout day from the home page.

## Core Flow

- Create a manual plan with at least two workout days.
- Add, delete, move up, and move down exercises.
- Try saving invalid fields and confirm the message points to the specific day or exercise.
- Start a workout, leave the app, return, and verify draft recovery.
- Try finishing with unfinished exercises and confirm the warning appears.
- Save the workout and verify history shows an exercise snapshot.

## AI And Settings

- Open AI without an API Key and confirm the error is clear.
- Save an API Key in Settings and verify the status text updates.
- Generate an AI plan, edit the preview, save it, and confirm it appears as a new plan.
- Clear the API Key and confirm AI generation becomes unavailable.

## Backup And Release Checks

- Export backup JSON and confirm version/export time are shown.
- Preview import and verify it states that import only adds data.
- Import backup and verify existing plans are not overwritten.
- Run `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleRelease`, and `:app:lintDebug`.
