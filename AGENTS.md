## FinTrack Agent Instructions

### Purpose

These instructions apply to all AI-assisted work in this repository. Optimize every task for the Android AI Studio free tier while preserving correctness, existing behavior, migration safety, security boundaries, repository history, and auditability.

### Core Operating Rules

- Use Kotlin and the existing FinTrack architecture and naming conventions.
- Inspect the current repository state before editing: working directory, Git root, branch, HEAD, `git status`, `git diff --stat`, and targeted diff output.
- Continue from existing work. Never discard, regenerate, or repeat completed changes unless a verified defect requires it.
- Read only files directly relevant to the requested stage. Avoid broad repository scans after the initial audit.
- Do not modify unrelated files or perform opportunistic refactors.
- Never begin the next task automatically.
- Never use `git reset --hard`, `git restore .`, `git clean -fd`, or a destructive Room migration fallback.
- Preserve stable entity IDs, historical transaction data, stored RON/EUR values, conversion statuses, BNR audit metadata, CSV compatibility, Room data, and existing repository boundaries unless the current task explicitly requires a compatible change.
- Firebase Authentication and household-scoped inbound Firestore synchronization are established features. Preserve their existing authorization, validation, listener-lifecycle, and local-data behavior.
- Do not broaden Firestore access, introduce general outbound synchronization, or add new cloud dependencies unless explicitly required by the current task.
- Treat entity `syncStatus` fields as metadata, not as proof of successful remote synchronization.
- The Room `sync_outbox` table is reserved for a future explicitly authorized outbound-synchronization design. Do not read, write, process, enqueue, clear, or infer migration completion from it during the controlled Room-to-Firestore migration.

### Quota and Token Economy

- Work in small, dependency-aware stages rather than one large autonomous run.
- Give or execute only one smallest action at a time unless the user explicitly requests the full plan.
- At the start of a stage, inspect only the files identified by the preceding audit or checkpoint.
- Do not repeatedly summarize the project or reread unchanged files.
- Do not produce long progress narratives. Return concise checkpoints.
- Do not run the full test suite after every edit.
- During implementation, run only the smallest targeted test class or method covering the changed behavior.
- Reuse valid focused-test evidence when the tested source and relevant configuration have not changed.
- Do not rerun every completed stage merely to reproduce historical results.
- Run the complete unit suite and debug APK build once after all stages of the current task are finished.
- Preserve Gradle caches. Do not run `clean` unless stale generated output is proven to cause the failure.
- If a task approaches a timeout, stop safely and report a checkpoint instead of starting another broad build or fix loop.

### Repository Identity and Publication Safety

Before any commit, merge, cherry-pick, push, or publication:

- Verify the current working directory and Git root.
- Verify the current branch, HEAD, commit history, and configured remote.
- Verify that `AGENTS.md` is tracked and preserved.
- Treat Android AI Studio applet workspaces as potentially isolated or incomplete snapshots.
- Never configure a remote or publish from a snapshot that lacks the authoritative project history.
- Do not commit, push, fetch, reset, merge, rebase, or cherry-pick until repository identity and history are established when any discrepancy exists.
- Do not claim that a commit is published merely because it is the local HEAD or appears in `FETCH_HEAD`.

### Required Workflow

#### Stage 0 — Resume and Audit

Before changing files:

- Run the minimum read-only Git identity and status checks required for the task.
- Inspect targeted Git differences.
- Compare the repository with every requirement in the current task specification.
- Distinguish current source evidence from historical handoff or AI summary claims.
- Return only:
  - completed requirements;
  - missing requirements;
  - exact files requiring changes;
  - proposed small stages;
  - targeted test for each stage;
  - the single smallest next action.
- Do not edit files or run Gradle during the audit stage.

#### Implementation Stages

For each requested stage:

- Implement only that stage.
- Read only directly relevant files.
- Preserve existing compatible work.
- Add or update focused tests.
- Run one targeted test first.
- Fix only failures caused by or blocking the current stage.
- Stop and report:
  - files modified;
  - behavior implemented;
  - targeted command;
  - passed, failed, and skipped counts;
  - background-task status;
  - exact remaining action.
- Do not automatically continue into the next stage.

#### Final Verification

After all implementation stages are complete, run exactly once:

```bash
gradle :app:testDebugUnitTest --no-daemon
gradle :app:assembleDebug --no-daemon
```

Target result:

- Both commands end with `BUILD SUCCESSFUL`.
- Confirm the APK exists at `app/build/outputs/apk/debug/app-debug.apk`.

If a final command fails or is interrupted:

- Diagnose the smallest relevant failure without restarting implementation.
- Do not repeatedly rerun the complete suite or build.
- Inspect existing authoritative XML/test outputs and background-task status.
- Determine whether the cause is a test defect, leaked resource, build defect, or environment timeout.
- If the environment execution ceiling is insufficient, record the incomplete result accurately and move the single final verification run to the authoritative Codespace or CI environment.
- Never claim that the complete suite passed when only partial results exist.
- Never claim `assembleDebug` succeeded unless that Gradle task completed successfully or equivalent authoritative task evidence is available.
- An internal `compile_applet` action and APK existence alone do not prove that the requested Gradle command completed.

### Controlled Firestore Migration Safety

The Prompt 8 Room-to-Firestore migration is a controlled, one-time migration, not general outbound synchronization.

- Migration must remain explicitly user-initiated and must never start on application startup, authentication change, network change, or a background schedule.
- Require an active household OWNER or ADMIN.
- Never assume or infer that `householdId` equals `userUid`.
- Require a complete, validated backup before any remote payload write.
- Never overwrite an existing conflicting remote document.
- Preserve stable IDs, timestamps, stored RON/EUR values, conversion statuses, exchange rates, effective dates, sources, and BNR audit metadata exactly.
- Never recalculate migrated financial values or upgrade questionable data to `OFFICIAL`.
- Never clear Room or automatically delete local records after migration.
- Keep `sync_outbox` unused throughout migration.
- Do not add WorkManager, recurring upload jobs, background services, alarms, or general outbound synchronization loops.
- Suppress inbound listeners only for the controlled migration window and restore them safely afterward.
- Sanitize every user-facing migration error. Never expose raw exceptions, stack traces, document internals, credentials, or personal financial data.

### Room and Data Safety

- Use explicit, non-destructive migrations for every schema-version increase.
- Export every new Room schema into `app/schemas/...` and copy required schemas to configured test or main asset locations.
- Test migration from the immediately preceding production schema using `MigrationTestHelper` when schema behavior changes.
- Preserve all unaffected columns and stable IDs.
- Couple a local entity mutation with a durable outbox mutation in the same Room transaction only when an explicitly authorized future synchronization design requires outbox tracking.
- The controlled Firestore migration is exempt from outbox mutation and must leave `sync_outbox` untouched.
- Test rollback atomicity where transactional local mutation and outbox behavior apply.
- Do not infer successful remote synchronization from entity metadata alone.

### BNR, Currency, and CSV Safety

- Preserve the working official BNR conversion endpoints and historical-date selection behavior.
- Never fabricate, estimate, interpolate, or hardcode fallback exchange rates.
- Never recalculate stored audited values during export or migration.
- Never upgrade missing, malformed, pending, failed, or unverified conversion data to `OFFICIAL`.
- Keep file and network work outside Room write transactions.
- Preserve existing CSV import/export headers, validation, preview, backup, confirmation, and atomic-write behavior unless an explicitly requested compatible migration changes them.

### Testing Strategy

Use this order:

1. Compile only if needed to expose syntax or type errors.
2. Run the new or modified targeted test class.
3. Run adjacent regression tests only when the changed boundary affects them.
4. Run the complete unit suite once at final verification.
5. Build the debug APK once at final verification.

Preferred targeted command:

```bash
gradle :app:testDebugUnitTest --tests "com.example.SpecificTest" --no-daemon
```

- Record exact counts only from completed authoritative test results.
- State explicitly whether the requested class, complete suite, and build finished.
- Do not report zero failures as proof of full-suite success when execution was interrupted or only partial XML results exist.

### Timeout or Quota Interruption

If interrupted by timeout or quota:

- Do not press Continue for implementation or other broad work.
- Continue at most once only when one already-started focused test is the sole remaining action.
- Do not revert changes or restart the task.
- Do not start another Gradle command while a previous task may still be running.
- Leave the repository in its current state.
- Provide a checkpoint containing:
  - current Git status summary;
  - files modified;
  - completed requirements;
  - incomplete requirements;
  - last command and result;
  - passed, failed, and skipped counts;
  - background-task status;
  - exact smallest unfinished action.
- In the next session, resume from the existing repository state and checkpoint. Never restart a partially completed stage from scratch.

### Repository and Artifact Safety

- Keep `AGENTS.md` tracked and preserve it across publications.
- Never open, encode, expose, stage, commit, publish, or include in source exports:
  - `debug.keystore.base64`;
  - keystores or signing credentials;
  - `google-services.json`;
  - `local.properties`;
  - `.env` files;
  - personal financial CSV or database exports;
  - generated APK files.
- Keep `.build-outputs/`, `app/build/`, Firebase debug output, local audit bundles, and other generated artifacts excluded from publication.
- Verify generated and sensitive files with metadata/status checks only; do not inspect their contents.

### Required Final Report Format

Return a concise report containing:

- Exact files inspected.
- Exact files modified.
- Work already present at session start.
- Work completed in this session.
- Requirement-by-requirement mapping.
- Tests added or changed.
- Exact targeted test commands and completed results.
- Exact final Gradle commands and completed results.
- Exact passed, failed, and skipped counts from authoritative outputs.
- Whether the complete suite finished.
- Migration and data-preservation details when applicable.
- Confirmation that unrelated behavior was preserved.
- APK path and whether `:app:assembleDebug` conclusively completed.
- Git identity, branch, HEAD, working-tree status, and publication status when publication is requested.
- Confirmation that `AGENTS.md` remains tracked.
- Confirmation that generated and sensitive artifacts are not tracked or staged.
- Remaining risks, environment limitations, or explicitly deferred work.

Do not claim a test, build, migration, APK, commit, or publication result unless the corresponding authoritative command or evidence completed successfully.
