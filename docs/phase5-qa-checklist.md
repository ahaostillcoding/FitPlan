# FitPlan Phase 5 QA Checklist

Use this checklist before sharing a debug or internal-test build.

## Local Plan Flow

- Create a new plan with at least two workout days.
- Add, edit, duplicate, and delete exercises.
- Reorder workout days and exercises, then reopen the plan detail page.
- Set a plan as current and confirm the home page updates.
- Delete a plan and verify the confirmation dialog appears first.

## Workout Flow

- Start a workout from the home page.
- Mark some exercises complete and leave at least one incomplete.
- Use the rest timer once.
- Leave the workout screen and return to confirm draft recovery.
- Finish the workout and verify the history record contains exercise snapshots.

## History Flow

- Confirm records are sorted by newest date first.
- Open a record detail page and verify completed/incomplete exercises.
- Delete a record and verify the confirmation dialog and success message.

## AI Flow

- Open AI page without an API key and verify the missing-key error.
- Save a DeepSeek API key in settings.
- Run the connection test.
- Generate an AI plan, edit the preview, save it, and verify it creates a new plan.
- Trigger a network or invalid-key failure and verify the App does not crash.

## Backup Flow

- Export backup JSON from local data.
- Import the backup into a fresh database or test install.
- Preview import data before saving.
- Verify importing does not overwrite existing plans unless explicitly saved as new records.

## Offline Checks

- Disable network.
- Create/edit plans.
- Start and finish a workout.
- View history records.
- Confirm only AI generation and connection test require network.
