# FINTRACK CURRENT CONTEXT

> Compact operational context for continuing FinTrack development.
> Last verified: 2026-08-31
> Git baseline: `aed996f`
> Previous baseline: `14f5338` / `0da6b96` / `4ed7894` / `1ed28ec` / `37155bc` / `32fc27b` / `a739400` / `baf2f70`

## 1. ROLE OF THIS FILE

This is the short context that should be supplied to a new AI coding session.

It is intentionally NOT the complete project history.

For detailed history, decisions and evidence use:
- `FINTRACK_PROJECT_MEMORY.md`
- GitHub history
- ChatGPT project transcripts
- Antigravity history

## 2. CURRENT REPOSITORY STATE

```text
Branch:       main
HEAD:         aed996f
origin/main:  aed996f
Remote:       https://github.com/free2rhime/Fintrack.git
```

At the time this context was verified, the repository working tree was clean.

Current commit:

`aed996f ci: enable Firebase-configured online APK builds`

Previous functional baseline:

`14f5338` (ci: remove redundant GitHub workflows) / `0da6b96` (docs: update project memory after step 12.1) / `4ed7894` (fix: allow cross-user transaction editing) / `1ed28ec` / `37155bc` / `32fc27b` / `a739400` / `baf2f70`

**GitHub/current repository is the implementation source of truth.**

Do not assume an older handoff/checkpoint is current.

## 3. PROJECT

FinTrack is a shared household personal-finance Android application.

Household model:

```text
Household
├── Members
├── Shared Transactions
├── Shared Categories
│   └── Shared Subcategories
└── Shared Analytics
```

Different households must remain isolated.

Within one household, financial/category data is shared according to RBAC.

## 4. STACK

- Kotlin
- Android
- Jetpack Compose / Material 3
- Room
- Firebase Authentication
- Firestore
- Google Sign-In
- Kotlin Coroutines / Flow
- Gradle
- Unit tests / Robolectric
- Firestore Emulator / Rules tests
- GitHub Actions

Environments:
- Android Studio = local development
- Google AI Studio = online development/inspection
- Antigravity = alternative engineering environment
- GitHub = shared repository and implementation source of truth

## 5. CURRENT VERIFIED STATE

### Authentication
- Google Sign-In works in the APK.
- Firebase Authentication is integrated.

### Household
- Household resolution must be explicit.
- Synthetic/fallback household creation was removed.
- Different households must remain isolated.
- Household management is intentionally an online-only control-plane operation.

### Firestore Security Hardening (Step 8 — baf2f70)
- **P0-1 Household Self-Join: RESOLVED.** Member creation requires an atomic invitation binding (`inviteId`, matching household, status `PENDING`, invitee email match, active owner inviter).
- **Invitation Replay Protection: VERIFIED.** Rules evaluate `status == 'PENDING'` during pre-commit transaction evaluation; after acceptance the invitation transitions to `ACCEPTED`, preventing replay.
- **P0-2 Privilege Escalation: RESOLVED.** Member `role` and `status` mutations and deletions are restricted to `isHouseholdOwner()`; non-owners cannot escalate or delete owners. Kotlin domain `ADMIN` support is retained.
- **P1 Financial Validation: RESOLVED.** Strict numeric validation enforced (`amountRon > 0`, `amountEur >= 0`, `exchangeRate >= 0`, string `transactionDate` and `description`).
- **Emulator Verification: 92/92 PASS.** All rules unit tests pass against the Firestore Local Emulator.

### Android Test Suite & Migration Stabilization (Step 9 — a739400)
- **Step 9.1A Room Migration Tests: RESOLVED.** Resolved Room schema JSON availability for Robolectric unit tests via Gradle `debug` sourceSet asset configuration. `RoomMigrationTest` 4/4 PASS, `Stage1BMigrationStateTest` 4/4 PASS. No production migration SQL redesign.
- **Step 9.1B Stage 2B Preflight: RESOLVED.** Initialized valid backup fixture in `Stage2BPreflightTest` test setup, resolving preflight fast-fail and verifying all downstream conflict assertions (`ACTIVE_MIGRATION_IN_PROGRESS`, `EXISTING_REMOTE_DATA_DETECTED`, security contract). 9/9 PASS. Original assertions preserved.
- **Step 9.1C Compose / Robolectric UI Tests: RESOLVED.** Disambiguated `preview_household_id` test tag in `Stage3BUiTest` (7/7 PASS); configured coroutine test dispatching (`Dispatchers.setMain(testDispatcher)` / `resetMain()`), scrolling semantics (`performScrollTo()`), and Compose state dialog remounting in `Stage7Step4PreviewSafetyTest` (3/3 PASS).
- **MainViewModel Production Enhancement:** One low-risk, non-destructive production enhancement was introduced in `startMigrationPreflight()` to improve human-readable household-name resolution from `householdRepository` when preflight is initiated for a target household before real-time sync is actively populated.
- **Android Test Baseline: 330/330 PASS (0 failed, 0 skipped, 0 new regressions).**

### Outbox Reliability & Reconnection Polish (Step 10 — 32fc27b)
- **Foreground Reconnection Recovery: RESOLVED.** `FirestoreSyncRepository.checkHandshakeAndUpdateState()` wakes the outbound queue upon inbound snapshot handshake completion / reconnection; `OutboundSyncEngine.start()` safely forwards to `notifyPending()` if already started rather than silently returning.
- **Exponential Retry Backoff: RESOLVED.** Implemented deterministic, cancellable exponential retry backoff based on `retryCount` (base delay = 1000ms, max delay = 30000ms, formula `delay = base * 2^(retryCount - 1)`, overflow protected). Unit-test mode uses zero delay for instantaneous tests.
- **Maximum Retry Threshold: RESOLVED.** Implemented `MAX_RETRIES = 5` threshold for transient/unknown errors (`UNAUTHENTICATED`, `TIMEOUT`, `UNAVAILABLE`, `UNKNOWN_ERROR`). When threshold is reached, item transitions from `PENDING` to `FAILED` with descriptive error message, unblocking FIFO queue processing and informing `SyncStatus`. `PERMISSION_DENIED` immediately transitions to `FAILED` on attempt 1.
- **Coroutine Lifecycle & Concurrency: VERIFIED.** `activeJob` ownership preserved, cancellable delay, `CancellationException` preserved (reverting `IN_PROGRESS` to `PENDING`), single worker guarded by `processMutex` and atomic wakeup signal.
- **Household Isolation & FIFO: PRESERVED.** Sequential FIFO processing maintained; strict household validation before remote dispatch preserved.
- **Test Baseline: 335/335 Android Unit Tests PASS (27/27 focused Outbox tests), 92/92 Firestore Emulator Tests PASS, 0 new regressions.**

### Architecture Cleanup (Step 11 — 37155bc)
- **HistoricalRateRepairCoordinator (Step 11.1): EXTRACTED.** Separated BNR historical rate discrepancy detection, EUR impact calculation, CSV backup file creation & six-point validation, and repair preparation from `MainViewModel` into dedicated coordinator. (3/3 unit tests PASS).
- **CsvImportOrchestrator (Step 11.2): EXTRACTED.** Separated ContentResolver/URI input-stream resolution, CSV header/row parsing & validation, duplicate mode handling (`SKIP_EXISTING`, `UPDATE_EXISTING`), pre-import backup generation, and atomic repository import delegation from `MainViewModel`. (4/4 unit tests PASS).
- **MigrationPreflightHelper (Step 11.3): EXTRACTED.** Separated mandatory preflight backup bundle creation, manifest timestamp extraction, `PreflightValidationResult.Ready` to `MigrationPreviewState` data mapping, and UI error message sanitization from `MainViewModel`. (3/3 unit tests PASS).
- **Preserved Boundaries:** `MainViewModel` retains full UI state ownership (`MigrationUiState`, `MainUiState`), all coroutine lifecycle ownership (`viewModelScope`), migration state machine transitions, `confirmAndExecuteMigration()` progress callback, auth/household resolution, and public ViewModel API.
- **Combined Test Baseline: 345/345 Android Unit Tests PASS, 92/92 Firestore Emulator Tests PASS, assembleDebug PASS, 0 new regressions.**

### Cross-User Transaction Permission & Data Integrity (Step 12.1 — 4ed7894)
- **Cross-User Transaction Editing & Deletion: RESOLVED.** All active household members are permitted to create, edit, and delete transactions belonging to their household (`firestore.rules`). Creator identity is decoupled from mutation authorization.
- **Immutable createdByUid Preservation: VERIFIED.** `TransactionEntity.toFirestoreMap()` preserves original `createdByUid` on edits across multiple users, satisfying Firestore immutability rules (`FirestoreDtos.kt`, `FirestoreDtoTest`).
- **FAILED Outbox Shielding: RESOLVED.** `SyncOutboxDao.getActiveEntityIdsByType` includes `FAILED` outbox entries, shielding local un-synced or failed Room mutations from destructive overwrite by stale inbound remote snapshots (`SyncOutboxDao.kt`, `Stage9OutboxShieldTest`).
- **Active Household Preservation: RESOLVED.** `MainViewModel.activeHouseholdId` preserves resolved household context during `SyncStatus.PermissionDenied` and `SyncStatus.Offline` states without falsely masking errors or collapsing the query scope (`MainViewModel.kt`, `CategoryPermissionsTest`).
- **Security Boundaries Preserved:** Category/subcategory mutations remain OWNER/ADMIN-only. Household member management and invitation administration remain OWNER-only. Cross-household isolation strictly enforced.
- **Test Baseline: 343/343 Android Unit Tests PASS, 95/95 Firestore Rules test cases preserved in `tests/firestore.rules.test.ts`, assembleDebug PASS, 0 new regressions.**

### CI Baseline Cleanup & Firebase-Configured Online APK (Steps 12.1G–12.1I — 14f5338 / aed996f)
- **Redundant Workflows Removed:** `.github/workflows/build-debug-apk.yml`, `.github/workflows/unit-tests.yml`, and `.github/workflows/firestore-rules-tests.yml` were cleanly removed.
- **Retained Workflow:** `.github/workflows/build-apk.yml` ("Build Debug APK") actively maintained for APK release artifact generation.
- **Firebase Secret Injection (Step 12.1I — aed996f):** Temporarily reconstructs `app/google-services.json` from `secrets.GOOGLE_SERVICES_JSON` during workflow execution; validates JSON structure safely without secret logging; cleans up the file in an `always()` post-step. `google-services.json` remains strictly outside the Git repository.
- **Online APK Status:** assembleDebug PASS; Google Services integration PASS; Real-device Google Sign-In requires physical two-device testing (Step 12.2).
- **Firebase & Credentials:** `google-services.json` strategy preserved outside Git repository for local development; Firestore security rules and test suite preserved.

### Categories
- Categories are household-scoped.
- Stable UUIDs are used as identity.
- Categories are shared by household members.
- OWNER manages category hierarchy (ADMIN permissions per model; MEMBER cannot mutate).
- Initial default-category seeding occurs only on explicit household creation.
- Startup/repeated category seeding loops were removed.
- Deterministic category-ID hashing is not used.
- Duplicate default-category creation/reconciliation was addressed.
- Historical transactions must remain protected when categories are deleted.

### Transactions
- Transactions are household-scoped.
- All active household members may create, update, and delete transactions.
- `createdByUid` remains immutable as original creator audit identity.
- Bidirectional synchronization exists.
- Transaction deletion propagation exists.
- `migrationId` is omitted from Firestore payloads when null.
- Expense vs Income semantics must be preserved.
- Multi-currency support includes RON/EUR.

### Synchronization
The architecture contains:
- inbound Firestore snapshot listeners;
- outbound Room-backed outbox;
- synchronization status;
- coroutine/job lifecycle management.

The historical outbound coroutine lifecycle issue was addressed before the current checkpoint.

### SyncStatus
`SyncStatus.Synced` represents:
- completed inbound snapshot handshake (transactions + categories);
- valid active household;
- absence of active inbound error;
- zero `PENDING`, zero `IN_PROGRESS`, and zero `FAILED` outbox records.

`SyncStatus.Connecting` / `"Syncing..."` represents inbound handshake in progress OR unresolved active outbound work (`PENDING` or `IN_PROGRESS`).

Outbox failures map to `SyncStatus.PermissionDenied` (for `PERMISSION_DENIED`) or `SyncStatus.Offline` (for other fatal errors).

`OutboundSyncEngine` processing is decoupled from public `SyncStatus`, enabling outbound processing whenever inbound sync is healthy.

## 6. OPEN WORK & DEVELOPMENT PRIORITIES

### Development Sequence:
1. **Phase 1 — COMPLETED:** Firestore Security Hardening (`baf2f70`).
2. **Phase 2 — COMPLETED:** Android Test Suite & Migration Stabilization (`a739400`).
3. **Phase 3 — COMPLETED:** Outbox Reliability & Recovery Polish (`32fc27b`).
4. **Phase 4 — COMPLETED:** Architecture Cleanup (`37155bc`).
5. **Phase 5 (Step 12.1) — COMPLETED:** Cross-User Transaction Permission, Data Integrity & CI Baseline Synchronization (`4ed7894` / `14f5338` / `aed996f`).
   - *12.1A:* Cross-User Transaction Permission & Data Integrity Audit
   - *12.1B:* Cross-User Transaction Permission & Data Integrity Implementation
   - *12.1C:* Cross-User Transaction Regression & Commit Readiness Audit
   - *12.1D:* Cross-User Transaction Commit & Push
   - *12.1E:* Project Memory Commit & Push
   - *12.1F:* GitHub Firestore Action Removal & CI Baseline Cleanup
   - *12.1G:* GitHub Actions Redundancy Cleanup
   - *12.1H:* Project Memory Update & CI Baseline Synchronization
   - *12.1I:* GitHub Online APK & Firebase Configuration (`aed996f`)
   - *12.1J:* Project Memory Update & Online APK Baseline Synchronization
6. **Phase 6 (Step 12.2) — NEXT (P1):** Two-Device Beta Smoke Test Regression.

### Automated vs. Real-Device Verification:
- **Automated Verification:** PASS (343/343 Android unit/Robolectric tests, 95/95 Firestore rules test cases preserved).
- **Real-Device Verification:** STILL REQUIRED (Physical two-device smoke testing under real multi-device network conditions).

### Critical Two-Device Real-Device Test Scenario (Step 12.2):
1. **Device A (User 1):** Create Transaction A.
2. **Device B (User 2):** Create Transaction B.
3. **Device B (User 2):** Edit Transaction A.
4. **Device A (User 1):** Edit Transaction B.
5. **Verify:**
   - Both transactions remain visible on both devices.
   - Edits propagate bidirectionally.
   - No transaction disappears from Room or UI.
   - `SyncStatus` does NOT transition to `PermissionDenied`.
   - Firestore contains updated documents with original `createdByUid` intact.
   - Sign-out / Sign-in retains data and resolves household correctly.
6. **Cross-User Deletion:**
   - User 1 deletes User 2's transaction; User 2 deletes User 1's transaction.
   - Verify deletions propagate correctly to both devices.

### Open Areas:
1. Two-Device physical smoke verification under active concurrent use (Step 12.2).
2. Degraded-network and prolonged-offline real runtime behavior.
3. Periodic cleanup of old `SUCCESS` outbox records (P2 housekeeping).

## 7. UNKNOWN / NOT FULLY VERIFIED

- Degraded-network and prolonged-offline runtime behavior on physical devices.
- Complete multi-device concurrent conflict resolution under active physical load.
- Some deep historical rationale contained only in ChatGPT/Antigravity transcripts or historical evidence files.

## 8. NON-REGRESSION RULES

Do not reintroduce:

- synthetic or fallback households;
- ambiguous household resolution;
- deterministic category-ID hashing;
- startup category-seeding loops;
- serialized `migrationId: null` Firestore fields;
- weakened Firestore authorization;
- artificial sleeps/delays used to mask lifecycle defects;
- arbitrary coroutine cancellation/restart logic;
- per-user financial/category isolation inside a shared household;
- creator-only edit/delete restrictions on shared household transactions;
- overwriting `createdByUid` with editor's UID during transaction edits.

Preserve:

- strict household isolation;
- default-deny Firestore security;
- OWNER/MEMBER RBAC (OWNER-only member management and category administration);
- Room/outbox synchronization;
- FAILED outbox shielding from stale inbound snapshots;
- active household preservation during non-fatal sync errors;
- shared household financial/category model;
- Expense vs Income semantics;
- RON/EUR support;
- visible synchronization status;
- active household context;
- five-tab navigation and FinTrack visual identity.

## 9. DEVELOPMENT RULE

Before any code change:

1. Inspect current Git state.
2. Analyze the problem.
3. Form a hypothesis.
4. Verify it against the current repository.
5. Identify exact files/classes/methods.
6. Propose the minimal reversible change.

After a change:

1. Compile.
2. Run relevant targeted tests.
3. Run broader tests when appropriate.
4. Inspect the Git diff.
5. Commit and push only when the change is validated.
6. Update project memory when durable project state changes.

Compilation alone is not proof that a problem is resolved.

## 10. SYNCHRONIZATION DEBUGGING

For synchronization problems, analyze separately:

1. Authentication
2. Household resolution
3. Firestore authorization/rules
4. Inbound sync
5. Outbound sync
6. SyncStatus
7. Firestore listeners
8. Room/outbox
9. Coroutine lifecycle

For `UncompletedCoroutinesError` or hanging tests, identify the surviving Job/coroutine and its owner before changing tests or production lifecycle code.

## 11. CURRENT NEXT-STEP POLICY

Do not automatically select an item from OPEN work.

First identify the explicitly selected user task, then follow:

```text
ANALYZE
→ HYPOTHESIS
→ VERIFY
→ MINIMAL CHANGE
→ COMPILE
→ TARGETED TESTS
→ INTERPRET
→ DIFF
→ COMMIT/PUSH
→ MEMORY UPDATE
```
