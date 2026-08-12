# FinTrack Agent Instructions

## Purpose
These instructions apply to all AI-assisted work in this repository. Optimize every task for the Android AI Studio free tier while preserving correctness, existing behavior, migration safety, and auditability.

## Core Operating Rules

- Use Kotlin and the existing FinTrack architecture and naming conventions.
- Inspect the current repository state before editing: `git status`, `git diff --stat`, and targeted `git diff` output.
- Continue from existing work. Never discard, regenerate, or repeat completed changes unless a verified defect requires it.
- Read only files directly relevant to the requested stage. Avoid broad repository scans after the initial audit.
- Do not modify unrelated files or perform opportunistic refactors.
- Never begin the next numbered prompt automatically.
- Never use `git reset --hard`, `git restore .`, `git clean -fd`, or destructive Room migration fallback.
- Preserve stable entity IDs, historical transaction data, CSV compatibility, BNR behavior, and existing repository boundaries unless the current prompt explicitly requires a change.
- Do not activate Firebase Authentication, Firestore, remote synchronization, or new dependencies unless explicitly required by the current prompt.
- Treat entity `syncStatus` fields as metadata, not as the durable queue. The Room `sync_outbox` table is the durable local queue.

## Quota and Token Economy

- Work in small dependency-aware stages rather than one large autonomous run.
- At the start of a stage, inspect only the files identified by the preceding audit.
- Do not repeatedly summarize the project or re-read unchanged files.
- Do not produce long progress narratives. Return concise checkpoints.
- Do not run the full test suite after every edit.
- During implementation, run only the smallest targeted test class or test method covering the changed behavior.
- Run the complete unit suite and APK build once, after all stages of the current numbered prompt are finished.
- Preserve Gradle caches. Do not run `clean` unless stale generated output is proven to be the cause of a failure.
- If a task approaches a timeout, stop safely and report a checkpoint instead of starting another broad build/fix loop.

## Required Workflow

### Stage 0 — Resume and Audit

Before changing files:

1. Run `git status`.
2. Run `git diff --stat`.
3. Inspect targeted `git diff` output.
4. Compare the repository with every requirement in the current numbered prompt.
5. Return only:
   - completed requirements;
   - missing requirements;
   - exact files requiring changes;
   - proposed stages;
   - targeted test for each stage.
6. Do not edit files or run Gradle during this audit stage.

### Implementation Stages

For each approved stage:

1. Implement only the requested stage.
2. Read only directly relevant files.
3. Preserve existing compatible work.
4. Add or update focused tests.
5. Run one targeted test first.
6. Fix only failures caused by or blocking the current stage.
7. Stop and report:
   - files modified;
   - behavior implemented;
   - targeted command;
   - result;
   - exact remaining stage.

Do not automatically continue into the next stage.

### Final Verification

After all implementation stages are complete, run exactly once:

```bash
gradle :app:testDebugUnitTest --no-daemon
gradle assembleDebug --no-daemon
```

Both commands must end with `BUILD SUCCESSFUL`. Confirm the APK exists at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

If a final command fails, diagnose the smallest relevant failure. Do not restart the implementation or modify unrelated code.

## Room and Data Safety

- Use explicit, non-destructive migrations for every schema-version increase.
- Export every new Room schema into `app/schemas/...` and copy required schemas to configured test/main asset locations.
- Test migration from the immediately preceding production schema using `MigrationTestHelper`.
- Preserve all unaffected columns and stable IDs.
- Couple each local mutation and its durable outbox mutation in the same Room transaction when synchronization tracking applies.
- Test rollback atomicity: neither the entity change nor outbox entry may survive if either write fails.
- Do not infer successful remote synchronization from entity metadata alone.

## Testing Strategy

Use this order:

1. Compile only if needed to expose syntax/type errors.
2. Run the new or modified targeted test class.
3. Run adjacent regression tests only when the changed boundary affects them.
4. Run the complete unit suite once at final verification.
5. Build the debug APK once at final verification.

Prefer a targeted command like:

```bash
gradle :app:testDebugUnitTest --tests "com.example.SpecificTest" --no-daemon
```

## Timeout or Quota Interruption

If interrupted by timeout or quota:

- Do not revert changes.
- Do not restart the numbered prompt.
- Leave the repository in its current state.
- Provide a checkpoint containing:
  - current `git status` summary;
  - modified files;
  - completed requirements;
  - last command and result;
  - exact next smallest action.

On the next session, resume from Stage 0 using the existing repository state and the full original numbered prompt.

## Required Final Report Format

Return a concise report with:

1. Exact files inspected.
2. Exact files modified.
3. Work already present at session start.
4. Work completed in this session.
5. Requirement-by-requirement mapping.
6. Tests added or changed.
7. Exact targeted test commands and results.
8. Exact final Gradle commands and results.
9. Total tests passed and zero failures.
10. Migration details and data-preservation mapping, when applicable.
11. Confirmation that unrelated behavior was preserved.
12. APK path: `app/build/outputs/apk/debug/app-debug.apk`.
13. Remaining risks or explicitly deferred work.

Do not claim a test, build, migration, or APK result unless the corresponding command actually completed successfully.
