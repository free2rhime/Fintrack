# FINTRACK PROJECT MEMORY v2

> Canonical compact operational memory for the FinTrack project.
> Last reconciled: 2026-08-31
> Current verified Git checkpoint: `7a8b6bf`
> Previous functional baseline: `aed996f` / `14f5338` / `0da6b96` / `4ed7894` / `1ed28ec` / `37155bc` / `32fc27b` / `a739400` / `baf2f70`

---

## 0. PURPOSE

This document exists to prevent loss of FinTrack context across:

- ChatGPT project chats
- Google AI Studio
- Android Studio
- Antigravity
- GitHub

It is **not** the complete project history.

The rule is:

> Keep operational memory compact; keep detailed history in separate evidence/checkpoint documents.

Every important statement should be classified as:

- **VERIFIED** — supported by current repository/tests or converging sources.
- **HISTORICAL** — true for an earlier checkpoint but not necessarily current.
- **OPEN** — known unfinished work.
- **UNKNOWN** — not sufficiently demonstrated.
- **CONFLICT** — sources disagree and verification is required.

---

# 1. SOURCE AUTHORITY

When sources disagree:

1. **Current GitHub/local repository** → source of truth for implementation and current code.
2. **ChatGPT project history** → source of truth for requirements, decisions, rationale and user intent.
3. **Antigravity history** → source of truth for its investigations, experiments and observations.
4. **Old handoff/context files** → historical context only; must be verified against current repository.

Never silently turn historical information into current state.

---

# 2. CURRENT CHECKPOINT — VERIFIED

Repository state verified on 2026-08-31:

```text
Branch:       main
HEAD:         7a8b6bf
origin/main:  7a8b6bf
Working tree: clean
Remote:       https://github.com/free2rhime/Fintrack.git
```

Commit:

`7a8b6bf docs: update project memory after step 12.1I`

The repository contains a chronological Git history from the initial commit through `873017a`, `baf2f70`, `a739400`, `32fc27b`, `37155bc`, `1ed28ec`, `4ed7894`, `0da6b96`, `14f5338`, `aed996f` to `7a8b6bf`.

### Relationship to Previous Baseline:

```text
baf2f70
fix: harden firestore security rules

        ↓

a739400
test: stabilize Android migration and UI tests

        ↓

32fc27b
fix: improve outbox reliability and reconnection recovery

        ↓

37155bc
refactor: extract domain logic from MainViewModel

        ↓

1ed28ec
docs: update project memory after step 11

        ↓

4ed7894
fix: allow cross-user transaction editing (Step 12.1)

        ↓

0da6b96
docs: update project memory after step 12.1

        ↓

14f5338
ci: remove redundant GitHub workflows (Step 12.1G)

        ↓

aed996f
ci: enable Firebase-configured online APK builds (Step 12.1I)

        ↓

7a8b6bf
docs: update project memory after step 12.1I (Step 12.1J)
```

---

# 3. HISTORICAL DEVELOPMENT ARC — VERIFIED FROM GIT

The commit history reconstructs the major implementation phases.

### Foundation
- Initial Android project structure and build configuration.
- GitHub Actions debug APK build.
- Jetpack Compose UI foundation.
- BNR exchange-rate synchronization.
- Transaction filtering/suggestions and optional destination.
- Room schema/test setup.

### Data architecture
- Room migration 1 → 2.
- Repository abstraction.
- Cloud-sync-ready entities.
- Room-backed sync outbox.

### Authentication and security
- Firebase Authentication.
- Google Sign-In integration.
- Firestore Security Rules.
- Household protection.
- Collection-group membership query protection.
- Default-deny and household-scoped authorization.

### Firestore synchronization
`cc0c42d` introduced the main Firestore synchronization implementation, including:

- `FirestoreDtos`
- `FirestoreSyncRepository`
- sync integration in the application
- lifecycle tests
- synchronization tests

### Coroutine lifecycle
`3729dbf` addressed synchronization coroutine lifecycle behavior.

The change included:

- tracking repository sync jobs;
- cancelling tracked jobs;
- replacing the OutboundSyncEngine observer-job handling with lifecycle-job ownership;
- cancelling/restarting the lifecycle job in a controlled manner.

This history is important because `UncompletedCoroutinesError` was a real project investigation area and must not be "fixed" by hiding the lifecycle problem in tests.

### Household resolution
Several commits subsequently removed synthetic/fallback household behavior and enforced explicit household resolution.

### Multi-device synchronization
`cde50c7` completed stages 3–10 and finalized the MVP multi-device synchronization milestone.

### Current category architecture
Subsequent commits implemented and stabilized:

- household-scoped categories;
- category RBAC;
- Firestore rules;
- duplicate default-category prevention;
- duplicate reconciliation;
- final category synchronization architecture.

### Current transaction architecture
Subsequent commits enforced transaction household scoping, corrected null `migrationId` serialization, and stabilized transaction outbound synchronization.

### SyncStatus accuracy and outbox alignment (Step 7.9 — 873017a)
`873017a` aligned `SyncStatus` semantics with actual outbox state:
- `SyncStatus.Synced` represents completed inbound handshake (transactions + categories) with a valid active household, no inbound errors, and zero unresolved outbound entries (0 PENDING, 0 IN_PROGRESS, 0 FAILED).
- `SyncStatus.Connecting` (`"Syncing..."`) represents inbound connection in progress OR active unresolved outbound work.
- Outbox failures map to `SyncStatus.PermissionDenied` (for `PERMISSION_DENIED`) or `SyncStatus.Offline`.
- `OutboundSyncEngine` processing is decoupled from public `SyncStatus`, allowing queue draining while status is `Connecting`.
- Infinite Outbox Flow collectors were removed in favor of direct/finite Room queries (`getActiveCountSync`, `getFirstFailedEntrySync`) and event-driven notifications (`onOutboxStateChanged`).
- Lifecycle `UncompletedCoroutinesError` in `TestScope` was resolved without sleeps, delays, or detached coroutines.

### Firestore Security Hardening (Step 8 — baf2f70)
`baf2f70` closed all three critical Firestore security vulnerabilities:
- **P0-1 Self-Join Closure:** Member creation now requires an atomic invitation binding (`inviteId`, matching household, status `PENDING`, invitee email match, active owner inviter).
- **Invitation Replay Protection:** Rules evaluate pre-commit `PENDING` status during atomic transaction; upon commit, the invitation becomes `ACCEPTED`, preventing reuse.
- **P0-2 Privilege Escalation Prevention:** Restricted member `role` and `status` updates and member deletions strictly to `isHouseholdOwner()`; non-owners cannot escalate to owner or delete owners. Kotlin domain `ADMIN` support is retained.
- **P1 Financial Validation:** Enforced strictly positive `amountRon > 0`, non-negative `amountEur >= 0` and `exchangeRate >= 0`, and mandatory string `transactionDate` and `description`.
- **DTO & Serialization:** Added nullable `inviteId` to `HouseholdMemberDto` with null-safe `toMap()` / `fromMap()`, populated during `acceptInvite()`.
- **Verification:** 92/92 Firestore rules emulator tests passing (0 failures).

### Android Test & Migration Stabilization (Step 9 — a739400)
`a739400` stabilized the full Android unit/Robolectric test suite, bringing the baseline to 330/330 PASS:
- **Step 9.1A Room Migration Tests:** Configured `debug` and `test` sourceSet assets in `app/build.gradle.kts` to expose canonical generated schemas (`app/schemas`) to Robolectric unit test execution. Resolved `FileNotFoundException` in `RoomMigrationTest` (4/4 PASS) and `Stage1BMigrationStateTest` (4/4 PASS) without modifying production migration SQL.
- **Step 9.1B Stage 2B Preflight:** Initialized valid default backup fixture in `Stage2BPreflightTest` setup, resolving preflight fast-fail and verifying all downstream conflict assertions (`ACTIVE_MIGRATION_IN_PROGRESS`, `EXISTING_REMOTE_DATA_DETECTED`, security contract). 9/9 PASS. Original assertions preserved.
- **Step 9.1C Compose / Robolectric UI Tests:** Disambiguated `preview_household_id` test tag in `Stage3BUiTest` (7/7 PASS); configured coroutine test dispatching (`Dispatchers.setMain(testDispatcher)` / `resetMain()`), scrolling semantics (`performScrollTo()`), and Compose state dialog remounting in `Stage7Step4PreviewSafetyTest` (3/3 PASS).
- **MainViewModel Production Enhancement:** Introduced one low-risk, non-destructive production enhancement in `MainViewModel.startMigrationPreflight()` to resolve the human-readable household name from `householdRepository` when preflight is initiated for a target household before real-time sync is actively populated.
- **Verification:** 330/330 Android unit tests PASS, 92/92 Firestore emulator tests PASS, 0 regressions.

### Outbox Reliability & Reconnection Polish (Step 10 — 32fc27b)
`32fc27b` resolved outbound synchronization reliability gaps and established the 335/335 PASS test baseline:
- **Foreground Reconnection Recovery:** `FirestoreSyncRepository.checkHandshakeAndUpdateState()` explicitly wakes the outbound queue via `outboundSyncEngine.notifyPending()` upon inbound handshake/reconnection; `OutboundSyncEngine.start()` safely forwards to `notifyPending()` if already started rather than silently returning.
- **Exponential Retry Backoff:** Implemented deterministic, cancellable exponential retry backoff based on `retryCount` (base delay = 1000ms, max delay = 30000ms, formula `delay = base * 2^(retryCount - 1)`, overflow protected). Unit-test interceptor mode uses zero delay for instantaneous tests.
- **Maximum Retry Threshold:** Implemented `MAX_RETRIES = 5` threshold for transient/unknown errors (`UNAUTHENTICATED`, `TIMEOUT`, `UNAVAILABLE`, `UNKNOWN_ERROR`). When threshold is reached, item transitions from `PENDING` to `FAILED` with descriptive error message, unblocking FIFO queue processing and informing `SyncStatus`. `PERMISSION_DENIED` immediately transitions to `FAILED` on attempt 1.
- **Coroutine Lifecycle & Concurrency:** `activeJob` ownership preserved, cancellable delay, `CancellationException` preserved (reverting `IN_PROGRESS` to `PENDING`), single worker guarded by `processMutex` and atomic wakeup signal.
- **Household Isolation & FIFO:** Sequential FIFO processing maintained; strict household validation before remote dispatch preserved.
- **Verification:** 335/335 Android unit tests PASS (27/27 focused Outbox tests), 92/92 Firestore emulator tests PASS, 0 new regressions.

### Architecture Cleanup (Step 11 — 37155bc)
`37155bc` extracted domain and infrastructure responsibilities from `MainViewModel` into dedicated, stateless coordinators without expanding scope or breaking existing test contracts:
- **Step 11.1 Historical Rate Repair Coordinator:** Extracted BNR rate discrepancy auditing, EUR variance calculation, CSV backup creation, six-point backup validation, and repair preparation into `HistoricalRateRepairCoordinator`. (3/3 unit tests PASS).
- **Step 11.2 CSV Import Orchestrator:** Extracted ContentResolver/URI input-stream management, CSV parsing & header validation, duplicate mode resolution (`SKIP_EXISTING`, `UPDATE_EXISTING`), pre-import backup creation, and atomic repository import invocation into `CsvImportOrchestrator`. (4/4 unit tests PASS).
- **Step 11.3 Migration Preflight Helper:** Extracted mandatory preflight backup bundle creation, manifest timestamp parsing, `PreflightValidationResult.Ready` to `MigrationPreviewState` data mapping, and UI error sanitization into `MigrationPreflightHelper`. (3/3 unit tests PASS).
- **Step 11.4 Regression & Commit Readiness Audit:** Confirmed all 25 focused migration tests, 34 outbox regression tests, 19 historical repair tests, 13 CSV import tests, and 18 auth/lifecycle tests pass with zero test integrity compromises.
- **Preserved Boundaries:** `MainViewModel` retains full UI state ownership (`MigrationUiState`, `MainUiState`), all coroutine lifecycle ownership (`viewModelScope`), migration state machine transitions, `confirmAndExecuteMigration()` progress callback, auth/household resolution, and public ViewModel API.
- **Verification:** 345/345 Android unit tests PASS (0 failed, 0 skipped), 92/92 Firestore emulator tests PASS, assembleDebug PASS, 0 new regressions.

### Cross-User Transaction Permission & Data Integrity (Step 12.1 — 4ed7894)
`4ed7894` resolved the cross-user transaction editing and deletion synchronization blocker while safeguarding local data integrity:
- **Step 12.1A Forensic Audit:** Diagnosed root causes of multi-device permission failures: Firestore update/delete rules strictly bound transaction mutations to creator UID, while `TransactionEntity.toFirestoreMap()` replaced `createdByUid` with the editor's UID, causing `PERMISSION_DENIED` and cascading data loss.
- **Step 12.1B Implementation:**
  - *Cross-User Update Authorization:* Rules permit any active household member to update transactions within the household, preserving the immutability of `transactionId`, `householdId`, and `createdByUid` (`firestore.rules`).
  - *Cross-User Delete Authorization:* Rules permit any active household member to delete transactions within the household (`allow delete: if isHouseholdMember(householdId)` in `firestore.rules`).
  - *Immutable createdByUid Serialization:* `TransactionEntity.toFirestoreMap()` preserves original `createdByUid`, falling back to `effectiveUid` only for new creations (`FirestoreDtos.kt`).
  - *FAILED Outbox Shielding:* `SyncOutboxDao.getActiveEntityIdsByType()` includes `FAILED` status, preventing stale inbound snapshots from destructively overwriting local un-synced or failed edits (`SyncOutboxDao.kt`).
  - *Active Household Preservation:* `MainViewModel.activeHouseholdId` preserves resolved household context during `SyncStatus.PermissionDenied` and `SyncStatus.Offline` states without falsely masking error statuses (`MainViewModel.kt`).
- **Step 12.1C Regression & Commit Readiness Audit:** Confirmed 0 regressions across 48 focused tests and 95 Firestore emulator rules tests.
- **Step 12.1D Commit & Push:** Committed and pushed `4ed7894` (`fix: allow cross-user transaction editing`).
- **Verification:** 340/340 Android unit tests PASS, 95/95 Firestore emulator rules tests PASS, assembleDebug PASS, 0 new regressions.

### CI Baseline Cleanup & Firebase-Configured Online APK (Steps 12.1G–12.1I — 14f5338 / aed996f)
`14f5338` streamlined CI automation by removing redundant workflows, and `aed996f` enabled online Firebase-configured APK builds while preserving credentials outside the repository:
- **Removed Workflows:** `.github/workflows/build-debug-apk.yml`, `.github/workflows/unit-tests.yml`, and `.github/workflows/firestore-rules-tests.yml`.
- **Retained Workflow:** `.github/workflows/build-apk.yml` ("Build Debug APK").
- **Firebase Secret Injection (Step 12.1I — aed996f):** Temporarily reconstructs `app/google-services.json` from `secrets.GOOGLE_SERVICES_JSON` during workflow execution; validates JSON structure safely without secret logging; cleans up the file in an `always()` post-step. `google-services.json` remains strictly outside the Git repository.
- **Online APK Status:** assembleDebug PASS; Google Services integration PASS; successfully verified on physical devices (Step 12.2).
- **Testing & Security Preserved:** `tests/firestore.rules.test.ts` preserved in codebase; 343/343 Android unit tests PASS.

### Two-Device Physical Beta Smoke Test Regression (Step 12.2 — COMPLETE)
Step 12.2 was successfully executed on two physical devices (Device A and Device B) using the GitHub Actions-generated release APK with real Firebase services:
- **Physical Device Authentication:** Google Sign-In verified PASS on Device A and Device B against production Firebase Authentication.
- **Household & Invitation Flow:** Household creation, invitation issuance, atomic token acceptance, and membership resolution verified PASS on physical devices. Existing OWNER/MEMBER security model strictly preserved.
- **Cross-User Transaction Mutation:**
  - Verified User 1 creating Transaction A and User 2 creating Transaction B.
  - Verified User 2 editing Transaction A and User 1 editing Transaction B.
  - Verified User 1 deleting User 2's transaction and User 2 deleting User 1's transaction.
  - Bidirectional sync verified PASS in real-time. No transaction disappears from Room or UI. `SyncStatus` does not drop into `PermissionDenied`. `createdByUid` remains immutable.
  - The historical defect (where saving cross-user edits triggered `PermissionDenied` and made transactions disappear) is confirmed resolved on physical hardware.
- **Category Synchronization & Authorization:** Real-time category synchronization verified PASS; OWNER/ADMIN category hierarchy management verified PASS; unauthorized MEMBER mutation restrictions verified PASS.
- **Outbox & Offline Reconnection:** Inbound, outbound, and bidirectional sync verified PASS; local persistence, outbox queueing, network reconnection, pending mutation recovery, and foreground reconnection verified PASS.
- **App Lifecycle & Sessions:** App restart, FirebaseAuth session restoration, sync recovery after restart, and sign-out/sign-in cycles verified PASS.
- **Verification Layers:** Automated test suite (343/343 Android tests PASS, 95/95 Firestore emulator rules tests preserved) and Physical device suite (Device A & Device B PASS) are both green and complementary.

---

# 4. CURRENT ARCHITECTURE — VERIFIED

## 4.1 Application

FinTrack is a shared household personal-finance Android application.

Stack:

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

Development environments:

```text
Android Studio  → local development
Google AI Studio → online development / inspection
Antigravity     → alternative engineering environment
GitHub          → shared repository / source of truth
```

---

## 4.2 Household

The application is household-centric:

```text
Household
├── Members
├── Shared Transactions
├── Shared Categories
│   └── Shared Subcategories
└── Shared Analytics
```

Rules:

- different households are isolated;
- household resolution must be explicit;
- no synthetic/fallback household may be created merely because resolution failed;
- within a household, shared financial/category data follows RBAC.

---

## 4.3 Authentication

Firebase Authentication is integrated with Google Sign-In.

Google Sign-In was verified functional in the APK.

Authentication and household resolution must be treated as separate synchronization layers.

---

## 4.4 Room

Room is the local client-side persistence layer for:

- transactions;
- categories;
- migration state;
- exchange rates;
- synchronization outbox.

Room/outbox is part of the synchronization architecture and must not be bypassed casually.

---

## 4.5 Firestore

Firestore is the remote synchronization backend.

Security principles:

- default deny;
- household-scoped access;
- cross-user transaction mutations: all active household members may create, update, and delete transactions in their household;
- immutable transaction audit fields (`createdByUid`, `transactionId`, `householdId` are strictly immutable on update);
- OWNER/MEMBER/ADMIN RBAC (with owner-only privilege for role/status mutations, member deletions, and invitation management);
- OWNER-only category and subcategory creation, update, and deletion (ADMIN per model; MEMBER cannot mutate);
- invitation binding for member self-creation (`inviteId`, matching household, status `PENDING`, matching email, active owner inviter);
- invitation replay protection (server-side transition from `PENDING` to `ACCEPTED` upon transaction commit);
- collection-group access only where explicitly authorized (`members` queries constrained by `uid` and `ACTIVE` status);
- strict financial schema validation (`amountRon > 0`, `amountEur >= 0`, `exchangeRate >= 0`, string `transactionDate` and `description`);
- no authorization weakening as a workaround for synchronization failures.

---

## 4.6 Categories

Current category architecture:

- household-scoped;
- stable UUID identity;
- shared across household members;
- OWNER-managed (MEMBER consumption only);
- initial default seeding only on explicit household creation;
- no repeated startup seeding;
- no deterministic category-ID hashing;
- duplicate default creation was prevented;
- existing duplicates were reconciled;
- category deletion must not invalidate historical transaction integrity.

This architecture should be treated as stable unless a reproducible regression or explicit product decision requires change.

---

## 4.7 Transactions

Transactions are:

- household-scoped;
- editable and deletable by any active household member;
- tagged with an immutable `createdByUid` representing original creator audit identity;
- synchronized bidirectionally;
- subject to deletion propagation;
- categorized using the household category model;
- capable of Expense and Income semantics;
- multi-currency, including RON/EUR.

Firestore serialization must omit `migrationId` when it is null.

---

## 4.8 Synchronization

The synchronization architecture contains:

```text
Room
  ↓
Outbox (PENDING / IN_PROGRESS / FAILED)
  ↓
OutboundSyncEngine
  ↓
Firestore

Firestore
  ↓
snapshot/listener
  ↓
FirestoreSyncRepository (with FAILED/active outbox shield)
  ↓
Room
```

Relevant synchronization layers:

1. authentication;
2. household resolution;
3. Firestore authorization;
4. inbound sync;
5. outbound sync;
6. SyncStatus;
7. listeners;
8. Room/outbox;
9. coroutine lifecycle.

These layers must be diagnosed separately.

---

# 5. SYNCHRONIZATION / COROUTINE HISTORY

A major investigation occurred around coroutine lifecycle and tests.

The historical failure class included `UncompletedCoroutinesError`.

The correct debugging rule is:

1. identify the surviving Job/coroutine;
2. identify its owner;
3. determine how it starts;
4. determine how it is cancelled;
5. reproduce the failure;
6. only then modify lifecycle code or tests.

Never use:

- arbitrary sleeps;
- `Thread.sleep`;
- artificial delays;
- arbitrary timeout extensions;
- test-only cancellation hacks

to hide an actual lifecycle defect.

The current repository contains lifecycle stabilization changes, including the `951b4aa` transaction outbound-sync stabilization. The full regression state still requires explicit verification.

---

# 6. SYNCSTATUS — VERIFIED SEMANTICS

`SyncStatus.Synced` represents:
- completed inbound transaction snapshot handshake;
- completed inbound category snapshot handshake;
- valid active household;
- absence of active inbound error;
- zero `PENDING` outbox entries;
- zero `IN_PROGRESS` outbox entries;
- zero `FAILED` outbox entries.

`SyncStatus.Connecting` (`"Syncing..."`) represents:
- inbound snapshot handshake in progress; OR
- active unresolved outbound work (`PENDING` or `IN_PROGRESS`).

`FAILED` outbox entries map to:
- `SyncStatus.PermissionDenied` (when `errorCode == "PERMISSION_DENIED"`);
- `SyncStatus.Offline` (for other fatal outbound errors).

`OutboundSyncEngine` processing is decoupled from public `SyncStatus`, allowing mutations to be uploaded whenever inbound synchronization is healthy, even while the user-facing status is `Connecting`/`Syncing`.

Outbox state is evaluated via direct/finite Room queries (`getActiveCountSync`, `getFirstFailedEntrySync`) and event-driven notifications (`onOutboxStateChanged`), avoiding unbounded background collectors.

---

# 7. CURRENT VERIFIED / RESOLVED ITEMS

At checkpoint `baf2f70` the following have been addressed:

- Google Sign-In configuration and authentication.
- Explicit household resolution.
- Removal of synthetic/fallback household logic.
- Firestore household-scoped authorization.
- Default-deny security principles.
- OWNER/MEMBER category RBAC.
- Household-scoped categories.
- Duplicate default-category prevention.
- Duplicate-category reconciliation.
- Final category synchronization architecture.
- Transaction household scoping.
- Null `migrationId` omission in Firestore payloads.
- Transaction outbound synchronization stabilization.
- Historical coroutine lifecycle stabilization work.
- SyncStatus accuracy alignment with outbox state (0 PENDING, 0 IN_PROGRESS, 0 FAILED) (Step 7.9 — `873017a`).
- Decoupled outbound sync engine processing gate (Step 7.9 — `873017a`).
- Direct/finite outbox evaluation and event-driven state recomputation (Step 7.9 — `873017a`).
- Elimination of `UncompletedCoroutinesError` lifecycle defects without artificial sleeps or delays (Step 7.9 — `873017a`).
- Firestore Security Hardening (Step 8 — `baf2f70`):
  - P0-1 Household Self-Join closed via atomic invitation binding (`inviteId`, `PENDING` state, email match, owner inviter check).
  - Invitation replay protection verified against pre-commit transaction semantics.
  - P0-2 Privilege escalation closed (admin/member self-escalation denied, admin owner deletion denied, owner self-deletion denied).
  - P1 Financial validation enforced (`amountRon > 0`, `amountEur >= 0`, `exchangeRate >= 0`, string dates/descriptions).
  - `HouseholdMemberDto.inviteId` serialization/deserialization and client propagation in `acceptInvite()`.
  - 92/92 Firestore rules emulator tests passing (0 failures).
- Android Test Suite & Migration Stabilization (Step 9 — `a739400`):
  - Room migration schema asset resolution (`RoomMigrationTest` 4/4 PASS, `Stage1BMigrationStateTest` 4/4 PASS) via Gradle `debug` sourceSet asset configuration.
  - Stage 2B Preflight test fixture backup initialization resolving `BACKUP_INVALID` fast-fail and verifying all downstream conflict states (`Stage2BPreflightTest` 9/9 PASS).
  - Compose UI test selector disambiguation and scroll/lifecycle stabilization (`Stage3BUiTest` 7/7 PASS, `Stage7Step4PreviewSafetyTest` 3/3 PASS).
  - Non-destructive `MainViewModel.startMigrationPreflight()` household-name resolution enhancement.
  - Full Android regression baseline established at 330/330 PASS (0 failures, 0 skipped, 0 new regressions).
- Outbox Reliability & Reconnection Polish (Step 10 — `32fc27b`):
  - Foreground reconnection recovery: `FirestoreSyncRepository.checkHandshakeAndUpdateState()` wakes the outbound queue upon inbound snapshot handshake completion / reconnection; `OutboundSyncEngine.start()` safely forwards to `notifyPending()` if already started rather than silently returning.
  - Exponential retry backoff: deterministic, cancellable exponential retry backoff based on `retryCount` (base delay = 1000ms, max delay = 30000ms, formula `delay = base * 2^(retryCount - 1)`, overflow protected). Unit-test interceptor mode uses zero delay for instantaneous tests.
  - Maximum retry threshold: `MAX_RETRIES = 5` threshold for transient/unknown errors (`UNAUTHENTICATED`, `TIMEOUT`, `UNAVAILABLE`, `UNKNOWN_ERROR`). When threshold is reached, item transitions from `PENDING` to `FAILED` with descriptive error message, unblocking FIFO queue processing and informing `SyncStatus`. `PERMISSION_DENIED` immediately transitions to `FAILED` on attempt 1.
  - Coroutine lifecycle & concurrency safety: `activeJob` ownership preserved, cancellable delay, `CancellationException` preserved (reverting `IN_PROGRESS` to `PENDING`), single worker guarded by `processMutex` and atomic wakeup signal.
  - Household isolation & FIFO preserved: sequential FIFO processing maintained; strict household validation before remote dispatch preserved.
  - Full Android regression baseline established at 335/335 PASS (27/27 focused Outbox tests, 0 failures, 0 skipped, 0 new regressions).
- Architecture Cleanup (Step 11 — `37155bc`):
  - Domain responsibilities extracted from `MainViewModel` into `HistoricalRateRepairCoordinator`, `CsvImportOrchestrator`, and `MigrationPreflightHelper`.
  - UI state ownership, coroutines, and public APIs preserved.
  - 345/345 Android unit tests passing (0 failures, 0 skipped).
- Cross-User Transaction Permission, Data Integrity & CI Baseline Synchronization (Step 12.1 — `4ed7894` / `14f5338` / `aed996f`):
  - Cross-user transaction editing and deletion authorization in Firestore rules for all active household members (`allow update`, `allow delete` in `firestore.rules`).
  - Creator UID immutability in `FirestoreDtos.kt` (`createdByUid` preserved across cross-user edits).
  - Local transaction shielding in `SyncOutboxDao.kt` (`FAILED` outbox status included in active entity ID queries to prevent overwrite by stale inbound snapshots).
  - Active household ID preservation in `MainViewModel.kt` during `PermissionDenied` and `Offline` sync states.
  - CI redundancy cleanup removing `build-debug-apk.yml`, `unit-tests.yml`, `firestore-rules-tests.yml`, and retaining `build-apk.yml`.
  - Online APK build enabled with safe temporary injection and validation of `app/google-services.json` from `GOOGLE_SERVICES_JSON` secret (`.github/workflows/build-apk.yml`).
  - 343/343 Android unit tests PASS (0 failures, 0 skipped), 95/95 Firestore rules test cases preserved in `tests/firestore.rules.test.ts`.
- Two-Device Physical Beta Smoke Test Regression (Step 12.2 — COMPLETE):
  - Real-device Google Sign-In PASS on Device A and Device B against production Firebase.
  - Household creation, invitation issuance, atomic acceptance, and membership resolution PASS.
  - Cross-user transaction creation, bidirectional syncing, updating, and deletion PASS on both physical devices with immutable `createdByUid`. Historical bug (saving cross-user edits causing `PermissionDenied` and disappearing transactions) confirmed resolved.
  - Category synchronization, OWNER/ADMIN management, and MEMBER restrictions PASS.
  - Inbound, outbound, and bidirectional sync PASS with no `PermissionDenied` errors during valid operations; local persistence, outbox queueing, network reconnection, pending mutation recovery, and foreground reconnection PASS.
  - App restart, FirebaseAuth session restoration, sync recovery after restart, and sign-out/sign-in PASS.

These statements mean the corresponding implementation work exists at the current checkpoint. They do **not** imply that every possible runtime edge case has been exhaustively verified.

---

# 8. OPEN WORK

## Current Development Priorities:
1. **Phase 1 — COMPLETED:** Firestore Security Hardening (`baf2f70`).
2. **Phase 2 — COMPLETED:** Android Test Suite & Migration Stabilization (`a739400`).
3. **Phase 3 — COMPLETED:** Outbox Reliability & Recovery Polish (`32fc27b`).
4. **Phase 4 — COMPLETED:** Incremental Architecture Cleanup (`37155bc`).
5. **Phase 5 (Step 12.1) — COMPLETED:** Cross-User Transaction Permission, Data Integrity & CI Baseline Synchronization (`4ed7894` / `14f5338` / `aed996f`).
6. **Phase 6 (Step 12.2) — COMPLETED:** Two-Device Beta Smoke Test Regression on Physical Hardware.
   - *12.2:* Physical Two-Device Smoke Test Execution (PASS on Device A & Device B)
   - *12.2B:* Project Memory Update + Commit + Push
7. **Phase 7 — NEXT:** Beta Release Polish & Housekeeping (Pending Roadmap Review).

## Verification Layer Policy:
- **Automated Verification:** PASS (343/343 Android unit/Robolectric tests, 95/95 Firestore rules test cases preserved, assembleDebug PASS).
- **Physical Device Verification:** PASS (Physical two-device smoke testing on Device A & Device B completed with 0 errors across Google Sign-In, household sync, cross-user transactions, offline recovery, and app restart).

## Reconciled Open Items:

### OPEN-1 — Offline/recovery synchronization (RESOLVED in Step 10 — 32fc27b)
Resolved: Foreground reconnection recovery wakes the outbound queue automatically upon inbound snapshot handshake completion. Verified by `OutboundSyncEngineReliabilityTest`.

### OPEN-2 — Outbox failure/retry/recovery semantics (RESOLVED in Step 10 — 32fc27b)
Resolved: Implemented deterministic exponential retry backoff and max retry threshold (`MAX_RETRIES = 5`) for transient/unknown errors, preventing unbounded retries and queue starvation. Verified by `OutboundSyncEngineReliabilityTest`.

### OPEN-3 — Concurrent household edits
Need defined conflict behavior when multiple household members modify shared data concurrently.

### OPEN-4 — Room ↔ Firestore mirror integrity
Need explicit end-to-end verification that local and remote representations remain consistent.

### OPEN-5 — End-to-end migration verification
Migration state exists, but complete end-to-end execution needs verification.

### OPEN-6 — Full regression suite (RESOLVED in Step 9 — a739400)
Resolved: Stabilized Room schema test asset resolution, preflight backup fixtures, and Compose UI test lifecycle/scrolling semantics. Verified with 330/330 passing Android unit tests and 0 failures.

### OPEN-7 — SyncStatus outbound-health semantics (RESOLVED in Step 7.9 — 873017a)
Resolved: `SyncStatus.Synced` requires both inbound handshake readiness and zero unresolved outbox work (0 PENDING, 0 IN_PROGRESS, 0 FAILED). Outbound engine is decoupled from public UI status.

### OPEN-8 — Firestore Rules Security (RESOLVED in Step 8 — baf2f70)
Resolved: Closed self-join bypass, role escalation, and invalid financial values. Verified with 92/92 Firestore emulator tests and TypeScript compilation.

### OPEN-9 — Household Offline-First (INVALID / DESIGN DECISION)
Household creation and invitation management are intentionally online-only control-plane operations.

### OPEN-10 — Transaction Hard-Delete Mirror (CAN DEFER / P3)
Normal application operations use the verified soft-delete model. Physical deletion mirroring can be evaluated as a future enhancement.

### OPEN-11 — Periodic Outbox Completed Records Cleanup (CAN DEFER / P2)
Periodic background deletion of `SUCCESS` outbox records older than 24-48 hours. Basic manual cleanup queries exist in DAO.

---

# 9. UNKNOWN

Currently not fully demonstrated:

- prolonged offline/high-latency behavior on physical devices;
- complete runtime behavior under degraded network recovery;
- complete multi-device concurrent conflict resolution under active physical load;
- whether all historical Antigravity experiments remain represented in current memory.

UNKNOWN does not mean broken.

---

# 10. NON-REGRESSION RULES

Do not reintroduce:

1. synthetic/fallback household resolution;
2. ambiguous household selection;
3. weakened Firestore authorization;
4. deterministic category-ID hashing;
5. startup/repeated category seeding;
6. null `migrationId` serialization where rules reject it;
7. per-user isolation inside a shared household;
8. creator-only edit/delete restrictions on shared household transactions;
9. overwriting `createdByUid` with editor's UID during transaction edits;
10. artificial delays to mask coroutine lifecycle defects;
11. arbitrary coroutine cancellation/restart logic;
12. broad rewrites without reproducible evidence.

Preserve:

- household isolation;
- default-deny security;
- OWNER/MEMBER RBAC (OWNER-only member management and category administration);
- shared household financial data;
- cross-user transaction editing and deletion within household;
- immutable `createdByUid` preservation;
- Room/outbox architecture;
- FAILED outbox shielding from stale inbound snapshots;
- active household ID preservation during non-fatal sync errors;
- bidirectional synchronization;
- deletion propagation;
- Expense vs Income semantics;
- RON/EUR support;
- visible synchronization status;
- active household context;
- five-tab navigation and current FinTrack visual direction.

---

# 11. SOURCE RECONSTRUCTION STATUS

The project memory was reconstructed from three intended historical sources:

### Source #1 — GitHub
Used for:

- chronological implementation history;
- commit-level changes;
- architectural milestones;
- current repository checkpoint.

### Source #2 — ChatGPT project history
Used for:

- user requirements;
- decisions;
- problem descriptions;
- development rationale;
- project workflow.

### Source #3 — Antigravity
Used for:

- investigations;
- experiments;
- diagnostics;
- alternative-agent observations.

These sources should not be treated as equally authoritative for every fact.

Implementation truth → current Git repository.

Requirements/intent → ChatGPT project history.

Antigravity investigation results → Antigravity evidence.

Conflicts must be surfaced and verified.

---

# 12. MEMORY OPERATING MODEL

The project memory system consists of:

```text
FINTRACK_MEMORY_BOOTSTRAP.md
        ↓
How an AI agent should work

FINTRACK_CURRENT_CONTEXT.md
        ↓
What is true / relevant right now

FINTRACK_PROJECT_MEMORY.md
        ↓
Durable architecture + decisions + historical reconciliation

Git / ChatGPT / Antigravity
        ↓
Detailed evidence when needed
```

The objective is not to preserve every message.

The objective is to preserve enough **verified operational context** that a new AI session can continue safely.

---

# 13. MEMORY MAINTENANCE

Update `FINTRACK_CURRENT_CONTEXT.md` when:

- checkpoint changes;
- major issue becomes verified/resolved;
- important issue becomes open;
- next task changes;
- non-regression rule changes.

Update `FINTRACK_PROJECT_MEMORY.md` when:

- architecture changes;
- durable design decision is made;
- major historical reconciliation is completed;
- new non-regression rule is established.

Do not copy full transcripts into operational memory.

Detailed evidence may remain in separate archive/checkpoint files.

---

# 14. DEVELOPMENT WORKFLOW

The project workflow is:

```text
1. ANALYZE
2. FORM HYPOTHESIS
3. VERIFY AGAINST CURRENT REPOSITORY
4. IDENTIFY EXACT FILES / METHODS
5. PROPOSE MINIMAL CHANGE
6. MODIFY
7. COMPILE
8. RUN TARGETED TESTS
9. RUN BROADER TESTS WHEN APPROPRIATE
10. INSPECT DIFF
11. COMMIT
12. PUSH
13. VERIFY REMOTE STATE
14. UPDATE MEMORY IF REQUIRED
```

Never claim resolution solely because compilation succeeds.

Use:

- **implemented** = code was changed;
- **compiles** = build succeeded;
- **targeted test passes** = specific behavior passed;
- **verified** = evidence supports the claim;
- **resolved** = original reproducible problem is demonstrated as fixed.

---

# 15. CURRENT PROJECT POSITION

The application has progressed from initial Android scaffolding through:

```text
Foundation
→ local data architecture
→ Firebase authentication
→ Firestore security
→ household model
→ synchronization
→ multi-device synchronization
→ category RBAC
→ category reconciliation
→ transaction household scoping
→ transaction outbound stabilization
→ SyncStatus outbox alignment (Step 7.9 — 873017a)
→ Firestore Security Hardening (Step 8 — baf2f70)
→ Android Test & Migration Stabilization (Step 9 — a739400)
→ Outbox Reliability & Reconnection Polish (Step 10 — 32fc27b)
→ Architecture Cleanup (Step 11 — 37155bc)
→ Cross-User Transaction Permission & Data Integrity (Step 12.1 — 4ed7894)
→ CI Baseline Cleanup (Step 12.1G — 14f5338)
→ GitHub Online APK & Firebase Configuration (Step 12.1I — aed996f)
→ Two-Device Beta Smoke Test Regression (Step 12.2)
```

The current baseline is:

```text
7a8b6bf
docs: update project memory after step 12.1I
```

Previous functional baseline: `aed996f` / `14f5338` / `0da6b96` / `4ed7894` / `1ed28ec` / `37155bc` / `32fc27b` / `a739400` / `baf2f70`.

Step 12.2 (Two-Device Beta Smoke Test Regression) has been verified and completed on physical hardware. The next development task is Beta Release Polish & Housekeeping (Pending Roadmap Review).

---

# 16. FINAL RULE

When starting a new FinTrack session:

1. Read `FINTRACK_CURRENT_CONTEXT.md`.
2. Verify Git state.
3. Read `FINTRACK_PROJECT_MEMORY.md` if deeper context is needed.
4. Identify the explicitly selected task.
5. State cause/hypothesis before modifying code.
6. Make the smallest justified change.
7. Verify with compilation and relevant tests.
8. Update memory when durable state changes.

The goal is **continuity without unnecessary context bloat**.

## OPTIONAL DEVELOPMENT SKILLS

The adopted skill set is intentionally minimal:

* `research` → investigate
* `debug` → diagnose
* `architecture` → design
* `implement` → change
* `verify` → prove

Skills are optional tools, not mandatory workflow stages. They must not alter the established FinTrack workflow or override project constraints.

---

# 17. BETA READINESS ASSESSMENT

| Gate | Status | Evidence / Details |
|---|---|---|
| Security Gate | **PASS** | Firestore security rules hardened; P0-1, P0-2, P1 resolved; cross-user transaction edit/delete verified; 95/95 emulator rules test cases preserved |
| Inbound/Outbound Sync Gate | **PASS** | Handshake, outbox draining, foreground reconnection recovery, exponential retry backoff, max retries threshold, FAILED outbox shielding, and SyncStatus alignment verified |
| Full Android Regression Gate | **PASS** | 343/343 unit/Robolectric tests passing (0 failures, 0 skipped) |
| Migration Verification Gate | **PASS** | Migration state, schema assets, and preview dialog safety verified |
| Online Firebase APK Gate | **PASS** | assembleDebug PASS; safe secret injection and JSON validation in CI; Google Services integration PASS |
| Multi-Device Production Smoke | **PASS** | Physical Device A & Device B smoke regression verified with GitHub release APK, real Firebase auth, cross-user mutations, category sync, offline recovery, and app restarts (Step 12.2) |

**Overall Beta Status: READY FOR BETA RELEASE / BETA SMOKE TEST COMPLETE**
*Details:* All core functional, security, automated testing, and physical multi-device smoke verification gates have passed. Ready for roadmap transition / release polish.
