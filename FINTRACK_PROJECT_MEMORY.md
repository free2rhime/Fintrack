# FINTRACK PROJECT MEMORY v2

> Canonical compact operational memory for the FinTrack project.
> Last reconciled: 2026-09-05
> Current verified Git checkpoint: Phase 13 Final Visual QA Clean Completion Checkpoint (Phases 1–13 Complete — GO: PHASE 13 CLEAN)
> Previous functional baseline: Phase 12 / Phase 11 / Transactions Search Bug Fix Checkpoint (`fix(fintrack): restore Transactions search filtering` - 2026-09-03) / Step 12.3Z Real Database Import & Full CSV Pipeline Verification Checkpoint / Step 12.3Y / Step 12.3X / Step 12.3W / Step 12.3V / Step 12.3U / Step 12.3T / Step 12.3S / Step 12.3M / Step 12.3L / `1bef33f` / `7a8b6bf` / `aed996f` / `14f5338` / `0da6b96` / `4ed7894` / `1ed28ec` / `37155bc` / `32fc27b` / `a739400` / `baf2f70`

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

Repository state verified on 2026-09-05:

```text
Branch:       main
Remote:       https://github.com/free2rhime/Fintrack.git
```

Current implementation baseline:

Phase 13 Final Visual QA Clean Completion Checkpoint (`GO — PHASE 13 CLEAN` - 2026-09-05)
FinTrack Design System v1 verified across full presentation layer (Phases 1–13 complete).

Previous UI/Functional baselines:

Phase 12 Accessibility + Responsive QA (2026-09-05) / Phase 11 Motion Foundation Clean Completion (2026-09-05) / Phase 10 Empty/Loading/Error States (2026-09-05) / Phase 9 Dialogs & Forms Polish (2026-09-05) / Phase 8 Settings & Household Visual Overhaul (2026-09-05) / Phase 7 Categories Visual Overhaul (2026-09-05) / Transactions Search Bug Fix Checkpoint (`fix(fintrack): restore Transactions search filtering` - 2026-09-03)

Historical backend / data baselines:

Step 12.3Z (`docs: update project memory for real database import and full pipeline verification`) / Step 12.3Y (`test: verify real-device 33-row CSV import and historical transaction period filter visibility`) / Step 12.3X (`feat: implement "Tichete de masa" UI display label for Account while preserving internal "Meal Tickets" value`) / Step 12.3W / Step 12.3V / Step 12.3U / Step 12.3T / Step 12.3S / Step 12.3M (`fix: deduplicate CSV categories and subcategories`) / Step 12.3L / `1bef33f` (feat: automate BNR EUR conversion for CSV imports) / `7a8b6bf` (docs: update project memory after step 12.1J) / `aed996f` (ci: enable Firebase-configured online APK builds) / `14f5338` (ci: remove redundant GitHub workflows) / `0da6b96` / `4ed7894` (fix: allow cross-user transaction editing) / `1ed28ec` / `37155bc` / `32fc27b` / `a739400` / `baf2f70`

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

        ↓

1bef33f
feat: automate BNR EUR conversion for CSV imports (Step 12.3)

        ↓

Step 12.3G
fix: propagate authenticated context for CSV imports

        ↓

Step 12.3L / Step 12.3M
fix: deduplicate CSV categories and subcategories

        ↓

Step 12.3S / Step 12.3T / Step 12.3U / Step 12.3V
feat: permanent Firestore hard deletion for transactions and categories + inbound REMOVED sync

        ↓

Step 12.3W / Step 12.3X
feat: implement "Tichete de masa" UI display label for Account while preserving internal "Meal Tickets" value

        ↓

Step 12.3Y
test: verify real-device 33-row CSV import and historical transaction period filter visibility

        ↓

Step 12.3Z
docs: complete real historical database import & full pipeline verification checkpoint
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
- subject to permanent hard-delete propagation (outbound `document.delete().await()` in Firestore and local Room deletion);
- capable of receiving remote deletions via inbound `DocumentChange.Type.REMOVED` processing (with active outbox shielding);
- categorized using the household category model (category deletion leaves transaction category/subcategory strings intact; no cascade delete);
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
Firestore (Create / Update / Hard Delete)

Firestore
  ↓
snapshot/listener (ADDED / MODIFIED / REMOVED)
  ↓
FirestoreSyncRepository (with FAILED/active outbox shield & conflict diagnostics)
  ↓
Room (Insert / Update / Delete)
```

Relevant synchronization layers:

1. authentication;
2. household resolution;
3. Firestore authorization;
4. inbound sync (including `DocumentChange.Type.REMOVED` processing);
5. outbound sync (including hard-delete `document.delete().await()`);
6. SyncStatus;
7. listeners;
8. Room/outbox (with mutation shielding during inbound deletions);
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
- CSV Import & Automatic BNR EUR Conversion + Context Propagation (Step 12.3 — `1bef33f` / Step 12.3G):
  - Automatic RON → EUR conversion during CSV preview and import via `CsvImportOrchestrator`.
  - Historical BNR rate resolution using transaction date via `TransactionRepository.getOfficialRate()` with distinct-date caching and reuse.
  - Weekend and public holiday fallback to the preceding publishing day via existing BNR effective-date rules.
  - Existing `ExchangeRateService.calculateAmountEUR` reused for calculation and rounding consistency.
  - Conversion metadata populated (`exchangeRate`, `exchangeRateDate`, `exchangeRateSource = "BNR"`, `conversionStatus = ConversionStatus.AUTO_CONVERTED`).
  - Safe offline/unavailable fallback: `conversionStatus = ConversionStatus.PENDING`, `amountEUR = 0.0`, `exchangeRate = 0.0`, `exchangeRateSource = "NONE"`.
  - Authenticated context propagation (Step 12.3G): resolved the CSV import PermissionDenied defect by passing authenticated user UID and active household ID from `MainViewModel` through `CsvImportOrchestrator` to `CsvImporter` and `RoomTransactionRepository`. Transactions explicitly receive `householdId = activeHouseholdId`, `userId = authenticatedUserUid`, and `createdByUid = authenticatedUserUid`.
  - Elimination of fallback identities: authenticated imports no longer default to `householdId = null`, `userId = "local_user"`, or serialize `"remote_user"` to Firestore.
  - Local resilience & atomic commit: Room persistence commits atomically; transactions remain valid and queryable locally even if outbound network sync encounters errors.
  - Zero rules weakening: `firestore.rules` remain strictly enforced without modification.
  - Preview and persisted values remain strictly identical across `SKIP_EXISTING` and `UPDATE_EXISTING` modes.
  - Real-device physical verification: 33/33 transactions imported, official BNR EUR rates converted, visible in UI, queryable in export, outbox processed to Firestore, SyncStatus = `Synced`. Repeated import verified PASS.
  - 360/360 Android unit tests PASS (21/21 focused orchestrator/importer tests), 95/95 Firestore rules test cases preserved, assembleDebug PASS.

- CSV Category & SubCategory Deduplication (Step 12.3L / Step 12.3M — COMPLETE):
  - Root cause diagnosed and resolved: `CsvImportOrchestrator` now passes the active `householdId` when querying existing categories via `CategoryRepository.getAllCategoriesList(householdId)`.
  - Existing category and subcategory reuse: `CsvImporter.parseAndValidate()` identifies existing household categories and subcategories, reusing their stable UUIDs instead of flagging them as missing and generating new random UUIDs.
  - Duplicate creation prevention: Prevents creating duplicate `CategoryEntity` instances and outbox mutations on repeated CSV imports.
  - Strict isolation and scoping: Preserves household isolation (categories in household A cannot be reused in household B), Expense vs. Income separation (same-name categories with different types remain distinct), and parent category hierarchy separation.
  - Step 12.3G protection: Authenticated UID propagation, active `householdId`, immutable `createdByUid`, Room persistence, outbox queueing, and Firestore synchronization remain strictly preserved.
  - Existing duplicates status: Duplicate creation is prevented going forward during imports; existing historical duplicate records in the database are not altered/deleted (tracked for separate cleanup audit).
  - 76/76 Android unit/Robolectric test suites PASS (12/12 focused deduplication tests, 18/18 orchestrator tests, 4/4 importer tests, 33-transaction real-world regression PASS).

- Transaction & Category Firestore Hard Deletion (Step 12.3S / Step 12.3T / Step 12.3U / Step 12.3V — COMPLETE):
  - **Transaction Hard Delete:** Changed outbound deletion from Firestore soft-delete (`isDeleted = true`) to permanent hard deletion via `document.delete().await()` in `DefaultFirestoreSnapshotSource.deleteTransaction()` at `/households/{householdId}/transactions/{transactionId}`.
  - **Category Hard Delete:** Preserved direct permanent removal via `DefaultFirestoreSnapshotSource.deleteCategory()` (`document.delete().await()`) at `/households/{householdId}/categories/{categoryId}`.
  - **Inbound REMOVED Change Processing:** `FirestoreSnapshotSource.listenToTransactions()` and `DefaultFirestoreSnapshotSource.listenToTransactions()` track `DocumentChange.Type.REMOVED`. `FirestoreSyncRepository.processTransactionSnapshot()` permanently deletes corresponding local Room entities.
  - **Outbox Shielding on Remote Deletion:** `processTransactionSnapshot()` inspects `activeOutboxIds` prior to local entity deletion; if an active local mutation is pending in the outbox, the local Room entity is protected from deletion, a conflict event (`UPDATE_VS_DELETE`) is recorded, and `onConflictDetected` is invoked.
  - **Category Mirror Reconciliation & Data Safety:** Category mirror sync via `processCategorySnapshot()` and `deleteCategoriesNotIn()` deletes removed categories locally without foreign-key cascade; existing transaction category and subcategory string attributes remain intact.
  - **Backward Compatibility for Legacy Soft-Deletes:** Inbound documents with `isDeleted == true` continue to be handled safely and remove local Room entities.
  - **Zero Firestore Rules Modifications:** `firestore.rules` already permitted permanent hard deletions for active household members.
  - **Regression Verification (Step 12.3T):** Comprehensive audit verified 0 regressions across CSV import, BNR EUR conversion, security rules, and sync lifecycle. 380/380 full Android JVM/Robolectric tests PASS, 100/100 Firestore tests PASS.
  - **Physical Device Verification (Step 12.3U):** Verified hard-delete behavior on physical hardware. Verified transactions and categories are permanently deleted from Firestore without leaving soft-delete tombstones; verified local Room rows are removed and SyncStatus remains `SYNCED`.
  - **Historical Firestore Data:** Existing historical documents with `isDeleted == true` created prior to Step 12.3S were not deleted and remain untouched in Firestore, requiring separate administrative cleanup.

- Account / Payment Method UI Label Localization (Step 12.3W / Step 12.3X — COMPLETE):
  - **Internal Account Value vs UI Display Label:** Maintained strict architectural decoupling between internal domain enum/string value `"Meal Tickets"` and localized Romanian UI display label `"Tichete de masa"`.
  - **Data Model Compatibility:** Internal value `"Meal Tickets"` remains unchanged across Room, `TransactionEntity`, `TransactionDao`, Firestore DTOs, Firestore security rules, and CSV import format.
  - **UI Localization:** UI components (selection dropdowns, filter chips, transaction cards, creation forms) display `"Tichete de masa"` for the Meal Tickets payment method. Card remains `"Card"`, Cash remains `"Cash"`.
  - **No Database / Firestore Migration:** No database migration or Firestore schema modification was performed; the change is strictly an architectural UI layer mapping.
  - **Category vs Account Distinction:** Preserved the distinct category rename (`💳 Meal Tickets` → `💳 Tichete de masa`), clarifying that category identity is separate from the Account internal value.
  - **Test Baseline:** 8/8 targeted UI label tests PASS, full Android JVM/Robolectric test suite PASS.

- Complete Real Database Import & Full Pipeline Verification (Step 12.3Z — COMPLETE):
  - **Complete Real Historical Database Import:** The user's complete personal historical transaction database was imported into the FinTrack application using the CSV import functionality on real physical hardware:
    - Complete historical database: IMPORTED.
    - Room persistence: PASS (all transactions persisted atomically to Room).
    - Outbox processing: PASS (mutations queued and processed sequentially).
    - Firestore synchronization: PASS (all records synchronized to production Firestore without errors).
    - SyncStatus: SYNCED.
    - Production Firestore Console verification: VERIFIED (imported transaction data independently inspected and verified in Firebase Firestore Console).
    - Real-Database / Real-Device Verification: This checkpoint reflects real-device and real-database execution against production Firestore, beyond automated unit/emulator testing.
  - **End-to-End CSV Pipeline Final Verification:**
    - CSV parsing: PASS
    - CSV validation: PASS
    - Authenticated context propagation (`authenticatedUserUid` / `activeHouseholdId`): PASS
    - `householdId` propagation: PASS
    - `createdByUid` propagation: PASS
    - Room persistence: PASS
    - Household visibility: PASS
    - Category / SubCategory resolution: PASS
    - Category / SubCategory deduplication: PASS
    - Outbox generation: PASS
    - Firestore synchronization: PASS
    - BNR EUR conversion: PASS (historical BNR rate resolution, distinct-date caching, weekend fallback, holiday fallback)
    - PermissionDenied defect: RESOLVED (authenticated UID & active household propagated without weakening Firestore security rules)
    - SyncStatus: SYNCED
  - **33-Transaction Regression Baseline Preserved:**
    - 33 test transactions: PASS (imported and synchronized cleanly).
    - 0 duplicate categories or subcategories created.
    - Historical visibility resolution confirmed: initial apparent disappearance was caused by the selected date period filter in the UI defaulting to "Last Month", not a Room/sync defect.
  - **Category & SubCategory Deduplication (Step 12.3L) Re-Verified:**
    - Existing categories and subcategories belonging to the active household are accurately matched and reused by logical identity.
    - Household scoping remains strictly enforced (no cross-household category reuse).
    - Expense vs. Income separation preserved.
    - Parent category hierarchy preserved.
    - Complete database import produced zero unexpected duplicate categories and zero unexpected duplicate subcategories.
    - Existing historical duplicates remain preserved for separate cleanup audit.
  - **Hard Delete Architecture Preserved:**
    - Transaction hard delete: permanent deletion in Firestore via `document.delete().await()` and local Room deletion.
    - Category hard delete: permanent deletion in Firestore via `document.delete().await()` and local Room deletion.
    - Inbound `REMOVED` change processing: remote deletions propagate to Room; active outbox mutations shielded (`UPDATE_VS_DELETE` conflict handling).
    - Legacy `isDeleted == true` tombstone backward compatibility preserved.
    - Historical legacy soft-deleted documents remain separate administrative cleanup data.
  - **Account / Payment Method UI Label Preserved:**
    - Internal Account value: `"Meal Tickets"` across Room, DTOs, security rules, and CSV parser.
    - UI display label: `"Tichete de masa"` across dropdowns, filters, cards, and forms.
    - Zero database or Firestore schema migrations required.
  - **Data Integrity Summary:**
    - Complete historical transaction dataset: IMPORTED.
    - Room: CONSISTENT.
    - Firestore: CONSISTENT.
    - Household isolation: PRESERVED.
    - Pipeline verified: `CSV → CsvImporter → CsvImportOrchestrator → Room → Outbox → OutboundSyncEngine → Firestore`.

- Real CSV Import & Historical Transaction Visibility Resolution (Step 12.3Y — COMPLETE):
  - **Real-Device 33-Row CSV Import:** Verified successful import of a 33-row historical CSV dataset on physical hardware:
    - 33 transactions parsed, BNR exchange rates calculated/assigned, and persisted atomically to Room.
    - Outbox synchronized 33 documents to Firestore successfully (SyncStatus = `Synced`).
    - Zero duplicate categories or subcategories created; existing category hierarchy accurately reused.
    - Account value `"Meal Tickets"` in CSV accepted cleanly and rendered as `"Tichete de masa"` in UI.
  - **12.3Y Visibility Finding & Period Filter Diagnosis:**
    - The 33 imported historical transactions initially appeared missing from the Transactions screen tab.
    - Comprehensive forensic analysis confirmed Room persistence, Firestore synchronization, authenticated UID context, and Category/SubCategory UUID links were 100% healthy and intact (`isDeleted = false`, valid `householdId`).
    - Root cause was the user-facing period filter defaulting to `"Last Month"`, which excluded historical records outside the date window.
    - Switching the period filter to an appropriate historical/all-time range immediately made all 33 imported transactions visible.
    - No defects existed in `FinancialAnalyticsEngine` or CSV import pipeline; no unnecessary production or CSV engine modifications introduced.
  - **CSV Data Integrity Baseline:**
    - 33 historical transactions: SUCCESSFULLY IMPORTED & PERSISTED.
    - Category / SubCategory deduplication: CONFIRMED (0 duplicates created).
    - Manual category/subcategory UUIDs in CSV: NOT REQUIRED (identity accurately resolved via household-scoped lookup).
    - BNR RON → EUR conversion: PASS (historical rates, weekend/holiday fallback, distinct-date caching, PENDING fallback).
    - PermissionDenied: RESOLVED (authenticated UID & active household propagated).

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
7. **Phase 7 (Step 12.3) — COMPLETED:** CSV Import & Automatic BNR EUR Conversion + Context Propagation + Category Deduplication + Hard Delete Migration + Account UI Localization + Real CSV Verification + Real Database Import (`1bef33f` / Step 12.3G / Step 12.3L / Step 12.3S / Step 12.3U / Step 12.3X / Step 12.3Y / Step 12.3Z).
   - *12.3A:* CSV Automatic BNR EUR Conversion Architectural Audit
   - *12.3B:* CSV Automatic BNR EUR Conversion Implementation & Tests
   - *12.3C:* Regression & Commit Readiness Audit
   - *12.3D:* Commit & Push (`1bef33f`)
   - *12.3E:* Project Memory Update + Commit + Push
   - *12.3F:* CSV Import PermissionDenied Root-Cause Audit
   - *12.3G:* CSV Import Context Propagation & PermissionDenied Fix (360/360 Android tests PASS, 21/21 focused CSV tests PASS)
   - *12.3H:* CSV Import Regression & Commit Readiness Audit (READY)
   - *12.3I:* CSV Import Fix Commit & Push
   - *12.3J:* Project Memory Update & CSV Import Fix Baseline Synchronization
   - *12.3K:* CSV Category & SubCategory Matching/Deduplication Audit (COMPLETE — Root cause confirmed: `CsvImportOrchestrator` omitted `householdId` parameter in category lookup)
   - *12.3L:* CSV Category & SubCategory Deduplication Implementation (COMPLETE — Household-scoped lookup & reuse, stable IDs preserved, duplicate creation prevented)
   - *12.3M:* CSV Category Deduplication Commit & Push (`fix: deduplicate CSV categories and subcategories`)
   - *12.3N:* Project Memory Update & Deduplication Baseline Synchronization
   - *12.3R:* Transaction Hard-Delete Architecture & Risk Audit (COMPLETE)
   - *12.3S:* Transaction Hard-Delete & Inbound REMOVED Implementation (COMPLETE)
   - *12.3T:* Hard-Delete Regression & Commit Readiness Audit (COMPLETE — 380/380 tests PASS)
   - *12.3U:* Hard-Delete Real-Device Verification (PASS — Real hardware verification on Device A & Firestore)
   - *12.3V:* Project Memory Update — Hard Delete Checkpoint (COMPLETE)
   - *12.3W:* Account "Meal Tickets" UI Label Audit (COMPLETE — Distinction between internal "Meal Tickets" value and "Tichete de masa" UI display label established)
   - *12.3X:* Account UI Label Implementation (COMPLETE — "Tichete de masa" UI display label implemented; internal value preserved; 8/8 targeted UI label tests PASS)
   - *12.3Y:* Real CSV Import & Historical Transaction Visibility Verification (COMPLETE — 33-row historical CSV imported successfully on real physical hardware; 33 Firestore documents present; SyncStatus = Synced; 0 category/subcategory duplicates; historical transaction visibility confirmed as user-side period filter configuration)
   - *12.3Z:* Complete Real Database Import & Full Pipeline Verification Checkpoint (COMPLETE — User's complete historical database imported, Room persisted, Outbox processed, Firestore synchronized, SyncStatus = Synced, independently verified in Firebase Firestore Console; 0 unexpected category/subcategory duplicates; hard delete & account label preserved; Project Memory updated)
   - *Physical Device Verification:* 33/33 test transactions imported cleanly, full real historical personal transaction database imported and verified in production Firestore Console, BNR EUR rates converted, Room UI visible, export visible, outbox synced; physical hard deletion for transactions and categories verified with permanent Firestore document removal and clean Synced status; period filter visibility confirmed.
8. **Phase 8 — Beta Release Polish & Housekeeping:**
   - *Next Roadmap Item:* Continue FinTrack development from the verified real-database baseline / Beta Release Polish & Housekeeping.
   - *Open Administrative Cleanup Task:* Administrative cleanup script for legacy Firestore documents with `isDeleted == true` created prior to Step 12.3S (separate execution).
   - *Open Data-Integrity Item (Separate Task):* Audit and cleanup of historical duplicate Category/SubCategory records that already exist in the database from earlier imports prior to Step 12.3L (duplicate creation is prevented going forward, but existing duplicates remain preserved until an explicit cleanup task is conducted).

## Verification Layer Policy:
- **Automated Verification:** PASS (380/380 full Android JVM/Robolectric test cases passing, 100/100 Firestore test cases preserved, 8/8 targeted Account UI label tests PASS, assembleDebug PASS).
- **Physical Device Verification:** PASS (Physical two-device smoke testing on Device A & Device B completed; physical real-device 33-transaction CSV import completed with 33 synced documents; physical real-device transaction & category hard deletion verified with permanent Firestore document removal and clean Synced status; period filter visibility confirmed; complete real historical personal transaction database imported and verified in production Firestore Console).

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

### OPEN-10 — Transaction Hard-Delete Mirror (RESOLVED in Step 12.3S / Step 12.3T / Step 12.3U)
Resolved: Transaction and Category outbound deletions permanently delete Firestore documents via `document.delete().await()`. Inbound deletions are processed via `DocumentChange.Type.REMOVED` with active outbox mutation shielding. Legacy tombstones (`isDeleted == true`) remain supported. Verified on physical devices. Existing historical tombstones require separate administrative cleanup.

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
→ CSV Import & Automatic BNR EUR Conversion (Step 12.3 — 1bef33f)
→ CSV Import Context Propagation & PermissionDenied Fix (Step 12.3G)
→ CSV Category & SubCategory Deduplication (Step 12.3L / Step 12.3M)
→ Transaction & Category Firestore Hard Deletion + Inbound REMOVED Sync (Step 12.3S / Step 12.3T / Step 12.3U / Step 12.3V)
→ Account UI Label Localization (Step 12.3W / Step 12.3X)
→ Real CSV 33-Row Import & Historical Transaction Period Filter Visibility (Step 12.3Y)
→ Complete Real Historical Database Import & Production Firestore Console Verification Checkpoint (Step 12.3Z)
→ Transactions Search Bug Fix & IME Handling Checkpoint (2026-09-03)
```

The current baseline is:

Transactions Search Bug Fix Checkpoint (`fix(fintrack): restore Transactions search filtering` - 2026-09-03)

Previous functional baseline: Step 12.3Z / Step 12.3Y / Step 12.3X / Step 12.3W / Step 12.3V / Step 12.3U / Step 12.3T / Step 12.3S / Step 12.3M / Step 12.3L / `1bef33f` / `7a8b6bf` / `aed996f` / `14f5338` / `0da6b96` / `4ed7894` / `1ed28ec` / `37155bc` / `32fc27b` / `a739400` / `baf2f70`.

Step 12.3 (CSV Import & Automatic BNR EUR Conversion + Context Propagation + Category Deduplication + Hard Deletion Migration + Account UI Label Localization + Real CSV Verification + Real Historical Database Import Checkpoint) has been verified. The next development phase is Phase 8 — Beta Release Polish & Housekeeping, alongside separate tasks for legacy Firestore tombstone cleanup and historical duplicate category/subcategory record cleanup.

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
| Security Gate | **PASS** | Firestore security rules hardened; P0-1, P0-2, P1 resolved; cross-user transaction edit/delete verified; 100/100 emulator rules test cases preserved |
| Inbound/Outbound Sync Gate | **PASS** | Handshake, outbox draining, foreground reconnection recovery, exponential retry backoff, max retries threshold, FAILED outbox shielding, and SyncStatus alignment verified |
| Full Android Regression Gate | **PASS** | 380/380 full Android JVM/Robolectric test cases passing (0 failures, 0 skipped; 31/31 focused hard-delete/sync tests; 8/8 targeted Account UI label tests) |
| Migration Verification Gate | **PASS** | Migration state, schema assets, and preview dialog safety verified |
| Online Firebase APK Gate | **PASS** | assembleDebug PASS; safe secret injection and JSON validation in CI; Google Services integration PASS |
| Multi-Device Production Smoke | **PASS** | Physical Device A & Device B smoke regression verified with GitHub release APK, real Firebase auth, cross-user mutations, category sync, offline recovery, and app restarts (Step 12.2) |
| CSV BNR & Sync Gate | **PASS** | Automatic RON → EUR conversion during CSV preview/import with historical BNR rate resolution, distinct-date caching, authenticated context propagation, and real-device physical verification (Step 12.3 — 1bef33f / Step 12.3G) |
| CSV Category Deduplication Gate | **PASS** | Household-scoped category lookup & reuse, stable UUID preservation, duplicate entity prevention during import, and 12/12 dedicated tests passing (Step 12.3L / Step 12.3M) |
| Hard Delete Inbound/Outbound Gate | **PASS** | Permanent Firestore document deletion (`document.delete().await()`), inbound `REMOVED` change handling with outbox mutation shielding, legacy tombstone backward compatibility, and physical real-device verification PASS (Step 12.3S / 12.3T / 12.3U) |
| Account UI Label Localization Gate | **PASS** | Clean separation of internal `"Meal Tickets"` data contract and `"Tichete de masa"` UI display label; 8/8 targeted tests PASS (Step 12.3W / 12.3X) |
| Real CSV 33-Row Import & Visibility Gate | **PASS** | 33-row historical CSV imported on physical device; 33 Firestore documents present; SyncStatus = Synced; 0 category duplicates; historical transaction visibility confirmed as user period filter configuration (Step 12.3Y) |
| Real Database Import & Production Firestore Gate | **PASS** | User's complete historical personal transaction database imported via CSV on real device; atomic Room persistence PASS; sequential outbox sync PASS; production Firestore Console verified; SyncStatus = Synced; 0 unexpected category duplicates (Step 12.3Z) |
| Transactions Search & IME Gate | **PASS** | MainViewModel in-memory search query state integrated with filterSettings; OutlinedTextField explicit IME search handling added; case-insensitive multi-attribute search verified; Stage3AViewModelTest, FinancialAnalyticsEngineTest, and GreetingScreenshotTest PASS (2026-09-03) |

**Overall Beta Status: READY FOR BETA RELEASE / BETA SMOKE TEST COMPLETE**
*Details:* All core functional, security, automated testing, physical multi-device smoke, CSV BNR conversion + sync context propagation, category deduplication, hard deletion inbound/outbound sync, Account UI label localization, real-device 33-row CSV import, complete real historical database production Firestore verification gates, and Transactions Search filtering bug fix have passed. Duplicate creation during import is resolved. Permanent Firestore document removal for transactions and categories is verified on physical hardware. Historical transaction visibility confirmed. Legacy soft-deleted documents remain preserved in Firestore for separate administrative cleanup. Ready for roadmap transition / release polish.

---

# 18. CHECKPOINT: TRANSACTIONS SEARCH BUG FIX (2026-09-03)

**STATUS: VERIFIED / BUILD PASS**

## 1. CURRENT PROJECT STATE

FinTrack este în dezvoltare ca aplicație Android:

- Kotlin
- Jetpack Compose
- Room
- Firebase / Firestore
- Google Sign-In
- Kotlin Coroutines / Flow
- Gradle
- Unit tests / Robolectric

GitHub repository rămâne sursa oficială de adevăr pentru cod.

Google AI Studio / Android AI Studio și Android Studio sunt medii de lucru, nu surse independente de adevăr atunci când există diferențe față de repository.

## 2. FIRESTORE → GOOGLE SHEETS SYNC STATUS

Faza Apps Script Firestore → Google Sheets este stabilizată.

Ultimul checkpoint verificat:

STEP 13.18 — PASS

Rezultatul verificat:

- Firestore documents read: 1371
- Expense: 1067
- Income: 304
- Raw snapshot: 1371
- Expense analytical output: 1067
- Income analytical output: 304
- Expense sorted by transactionDate ASC
- Income sorted by transactionDate ASC
- Expense reconciliation: PASS
- Income reconciliation: PASS
- Known transaction verified: true
- Formulas preserved/restored
- Firestore writes: false
- Android writes: false
- Manual formula modification: false

STEP 13.16 și STEP 13.18 au confirmat funcționarea corectă a pipeline-ului.

FinTrackSync.gs este considerat scriptul final pentru sincronizarea Firestore → Google Sheets.

Architecture:

```text
Firestore
    ↓
Firestore REST API
    ↓
Native Apps Script OAuth token
    ↓
In-memory validation
    ↓
_raw transactions
    ↓
Expense / Income analytical output
```

Authentication model:

- Native Apps Script OAuth
- Service Account private-key authentication NU mai este utilizată.

Legacy Service Account / private-key infrastructure este considerată deprecated și trebuie eliminată doar după verificarea finală că nu mai este necesară.

## 3. NEW BUG INVESTIGATION — TRANSACTIONS SEARCH

Bug observat:

În tab-ul Transactions, câmpul Search accepta text, dar lista tranzacțiilor nu era filtrată.

Celelalte filtre funcționau:

- date / period filters
- transaction type
- category filters

Investigația read-only a identificat cauza exactă.

Primary root cause:

`MainViewModel.updateSearchQuery(query)` primea query-ul, dar îl ignora.

Problema era:

```kotlin
fun updateSearchQuery(query: String) {
    viewModelScope.launch {
        val current = filterSettings.value
        settingsRepository.updateSelectedPeriod(current.selectedPeriod)
    }
}
```

Query-ul nu era introdus în `FilterSettings`.

Ca urmare:

```text
TransactionsScreen
    ↓
onSearchQueryChanged(query)
    ↓
MainViewModel.updateSearchQuery(query)
    ↓
query discarded
    ↓
FilterSettings.searchQuery = ""
    ↓
FinancialAnalyticsEngine nu aplica search filtering
    ↓
lista rămânea neschimbată
```

## 4. SEARCH FIX IMPLEMENTED

Fix-ul a fost implementat minimal și fără modificări de schemă.

MainViewModel folosește acum stare temporară în memorie pentru search:

```kotlin
private val _searchQuery = MutableStateFlow("")
```

`filterSettings` combină:

```text
settingsRepository.filterSettingsFlow
+
_searchQuery
```

Conceptual:

```kotlin
combine(
    settingsRepository.filterSettingsFlow,
    _searchQuery
) { settings, query ->
    settings.copy(searchQuery = query)
}
```

`updateSearchQuery()`:

```kotlin
fun updateSearchQuery(query: String) {
    _searchQuery.value = query
}
```

Search query NU este persistat în DataStore și NU este stocat în Room.

Motiv:

Search-ul din Transactions este UI/session state și nu reprezintă o preferință persistentă a utilizatorului.

## 5. IME / ENTER FIX

TransactionsScreen a fost actualizat pentru Search/Enter handling.

`OutlinedTextField` folosește explicit:

```kotlin
keyboardOptions = KeyboardOptions(
    imeAction = ImeAction.Search
)
```

și:

```kotlin
keyboardActions = KeyboardActions(
    onSearch = {
        keyboardController?.hide()
        onSearchQueryChanged(searchQuery)
    }
)
```

Comportamentul final:

- search-ul se actualizează reactiv la tastare
- Enter/Search confirmă query-ul
- tastatura este închisă la Search/Enter
- Clear elimină query-ul
- lista este recalculată automat

Design-ul existent al TransactionsScreen a fost păstrat.

## 6. EXISTING SEARCH ENGINE

`FinancialAnalyticsEngine` avea deja implementarea corectă pentru text filtering.

Search-ul este:

- case-insensitive
- bazat pe `contains()`

Câmpuri suportate:

- description
- category
- subCategory
- account

Nu au fost adăugate alte câmpuri precum destination sau amount.

Search-ul este combinat cu filtrele existente prin logică AND:

- date / period
- transaction type
- category
- search query

`FinancialAnalyticsEngine` nu a fost modificat inutil dacă implementarea existentă era deja corectă.

## 7. TESTING — SEARCH FIX

Testele au fost executate cu succes.

**Stage3AViewModelTest:**

- toate cele 10 teste PASS
- `testUpdateSearchQueryFiltersTransactionsInViewModel`: PASS

Acest test confirmă:

`updateSearchQuery("groceries")`

actualizează imediat `filteredTransactions` cu tranzacțiile corespunzătoare.

Clearing search-ul restaurează lista completă.

**FinancialAnalyticsEngineTest:**

`testSearchFilteringSupportsDescriptionCategorySubCategoryAccountAndCaseInsensitive`: PASS

Acoperă:

- description
- category
- subCategory
- account
- case-insensitive search
- empty query
- non-matching query
- combinații cu period filters
- combinații cu type filters

**GreetingScreenshotTest:**

`transactionsScreen_screenshot`: PASS

Confirmă că UI-ul TransactionsScreen și search input-ul nu au regresii vizuale.

**Applet compilation:**

PASS

**Application build:**

PASS

Concluzie:

**TRANSACTIONS SEARCH BUG = FIXED AND VERIFIED**

## 8. MANUAL APK VERIFICATION

După testele automate, fix-ul a fost verificat manual în APK.

Search-ul funcționează.

Comportamentul confirmat:

- introducerea textului filtrează tranzacțiile
- Search/Enter funcționează
- Clear funcționează
- filtrele existente continuă să funcționeze
- combinația Search + alte filtre funcționează

Nu există regresii observate.

## 9. GIT CHECKPOINT

Fix-ul trebuie considerat un checkpoint Git separat.

Recommended commit message:

```text
fix(fintrack): restore Transactions search filtering
```

Commit description:

- connect search query to MainViewModel filter state
- add IME Search/Enter handling
- preserve reactive filtering and clear behavior
- add ViewModel and analytics search tests
- verify TransactionsScreen UI regression
- build and tests PASS

Nu presupune că commit-ul sau push-ul a fost efectuat decât dacă repository history confirmă acest lucru.

## 10. CURRENT NEXT DEVELOPMENT TARGET

Transactions Search este considerat rezolvat.

Nu mai investiga acest bug decât dacă apare o regresie.

Următoarea dezvoltare trebuie pornită ca problemă separată și trebuie urmat workflow-ul:

```text
ANALIZĂ
↓
IPOTEZĂ
↓
VERIFICARE
↓
MODIFICARE MINIMĂ
↓
COMPILE
↓
TESTE
↓
INTERPRETAREA REZULTATULUI
↓
CHECKPOINT
```

Nu modifica sincronizarea Firestore / Apps Script pentru probleme UI fără o cauză demonstrată.

## 11. IMPORTANT PROJECT MEMORY RULES

Păstrează următoarele ca reguli permanente:

- GitHub repository = source of truth pentru cod.
- FinTrackSync.gs = scriptul final pentru Firestore → Google Sheets.
- STEP 13.18 = ultimul checkpoint valid al sincronizării Apps Script.
- Transactions Search fix = implementat și verificat.
- Nu considera o problemă rezolvată doar pentru că aplicația compilează.
- Pentru coroutine/test lifecycle problems, identifică job-ul/coroutine responsabil înainte de modificări.
- Nu modifica artificial testele pentru a ascunde probleme reale.
- Preferă modificări mici și reversibile.
- Nu face refactoring fără legătură cu problema investigată.
- După fiecare modificare relevantă: compile + teste + interpretarea rezultatului.
- Nu modifica Firestore, Room, authentication sau sync architecture pentru probleme care pot fi rezolvate la nivel UI/ViewModel.

## 12. HISTORICAL MEMORY STATUS — TRANSACTIONS SEARCH CHECKPOINT

**CHECKPOINT NAME:**

FinTrack — Transactions Search Fix — Verified (2026-09-03)

**STATUS:**

HISTORICAL PASS

- Search: FIXED
- ViewModel: VERIFIED
- Analytics filtering: VERIFIED
- UI: VERIFIED
- Tests: PASS
- Build: PASS
- Firestore sync: STABLE — STEP 13.18 PASS
- Apps Script: FinTrackSync.gs = FINAL SCRIPT

---

## 13. FINTRACK DESIGN SYSTEM V1 UI OVERHAUL (PHASES 7–13 VERIFIED BASELINE)

Authoritative milestone documentation for Phases 7–13.

### PHASE 7 — CATEGORIES VISUAL OVERHAUL
**STATUS: COMPLETE / AUDIT GO**

Verified:
- CategoriesScreen aligned with FinTrack Design System v1.
- FinTrackCard containers.
- RadiusLarge geometry.
- Tonal category icon containers.
- IncomeEmerald / ExpenseCoral semantic colors.
- FinTrackSegmentedControl for category type selection.
- FinTrackEmptyState for empty category state.
- Responsive max width of 680dp.
- Dialog/form visual standardization.
- Existing semantic test tags preserved.
- No business logic changes.

---

### PHASE 8 — SETTINGS & HOUSEHOLD VISUAL OVERHAUL
**STATUS: COMPLETE / AUDIT GO**

Modified presentation files:
- `SettingsScreen.kt`
- `HouseholdOverviewCard.kt`

Verified:
- FinTrackCard section hierarchy.
- Account Identity & Security section.
- Pending Invitations.
- Household Setup / Overview.
- Currency selector.
- Dark / Light / System theme selector.
- CSV Import / Export.
- EUR synchronization controls.
- Sync diagnostics.
- Household role/status badges.
- Owner-only invite action.
- Responsive max width of 680dp.
- Minimum 48dp interactive targets.
- Existing callbacks and state bindings preserved.
- Existing semantic test tags preserved.
- No changes to Auth, RBAC, sync, repositories or ViewModels.

---

### PHASE 9 — DIALOGS & FORMS POLISH
**STATUS: COMPLETE / AUDIT GO**

Verified:
- Modal surfaces standardized using SurfaceDark.
- RadiusXLarge geometry.
- SurfaceContainerDark form fields.
- CobaltBlue focus states.
- FinTrackButton action hierarchy:
  - PRIMARY
  - SECONDARY
  - DESTRUCTIVE
- Dialog width constraints generally 480–560dp.
- verticalScroll added/standardized where required for keyboard safety.
- Header dismiss actions standardized.
- Inline field-level validation/error presentation standardized.
- Existing state flows and callbacks preserved.
- Existing semantic test tags preserved.
- No business logic changes.

Affected visual areas include:
- Migration dialogs
- CSV import dialogs
- Category forms
- Transaction-related forms/dialogs
- Other existing modal surfaces

---

### PHASE 10 — EMPTY / LOADING / ERROR STATES
**STATUS: COMPLETE / AUDIT GO**

Verified:
- FinTrackEmptyState standardized.
- Compact mode introduced for charts and embedded/dialog contexts.
- FinTrackLoadingState introduced for unified loading presentation.
- Dashboard chart empty states standardized.
- Analytics sparse/insufficient-data states standardized.
- Categories empty state standardized.
- Authentication loading/error presentation standardized.
- Create Household dialog loading/error presentation standardized.
- Invite Member dialog loading/error presentation standardized.
- Sync Diagnostics clean state standardized.
- No business logic changes.
- Existing semantic test tags preserved.

---

### PHASE 11 — MOTION FOUNDATION
**STATUS: COMPLETE / AUDIT GO**

New foundation:
- `app/src/main/java/com/example/ui/theme/Motion.kt`

Verified motion vocabulary:
- DurationFast = 150ms
- DurationStandard = 200ms
- DurationEmphasized = 250ms
- DurationSyncSpin = 1000ms
- StandardEasing = FastOutSlowInEasing
- LinearCurve = LinearEasing

Reusable APIs:
- `fastTween()`
- `standardTween()`
- `emphasizedTween()`
- `contentFade()`

Theme integration:
- `LocalFinTrackMotion`
- provided through `FinTrackTheme`

Existing motion migrated to centralized tokens:
- DashboardScreen
- CurrencyToggle
- FinTrackSegmentedControl
- FinTrackStatusBadge

Infinite animation policy:
- Only SYNCING badge rotation remains infinite.
- It is justified as functional sync feedback.
- It is lifecycle scoped to BadgeVariant.SYNCING.
- No unmanaged CoroutineScope / Job / GlobalScope introduced.
- No motion-related UncompletedCoroutinesError introduced.

Test file:
- `app/src/test/java/com/example/FinTrackMotionTest.kt`

Verified tests include:
- motion token constants
- tween specs
- contentFade
- CompositionLocal resolution
- CurrencyToggle integration
- FinTrackSegmentedControl integration
- FinTrackStatusBadge syncing/static states

---

### PHASE 12 — ACCESSIBILITY + RESPONSIVE QA
**STATUS: COMPLETE / AUDIT GO**

Verified:
- Dashboard chart controls use FinTrackSegmentedControl.
- 48dp minimum interactive targets enforced for applicable controls.
- Dashboard hero metric layout adapts on narrow widths.
- Dashboard and Transactions use centered max-width 680dp containers.
- AuthScreen uses vertical scrolling and approximately 480dp content constraint.
- High font-scale layouts hardened.
- Semantic roles preserved.
- Content descriptions preserved.
- Financial polarity is not communicated by color alone.
- Responsive behavior checked for narrow phones (<340dp, <360dp), standard phones (<480dp), tablets and foldables (<680dp).

---

### PHASE 13 — FINAL VISUAL QA
**STATUS: GO — CLEAN**

Final presentation-layer audit completed.

Audited:
- Dashboard
- Transactions
- Transaction Form
- Analytics
- Categories
- Settings
- Household
- Auth
- Dialogs / Forms
- Empty / Loading / Error states
- Shared UI components
- Design tokens
- Motion
- Accessibility
- Responsive behavior
- Semantic/test contracts

Verified Design System tokens:
- CanvasDark
- SurfaceDark
- SurfaceContainerDark
- SurfaceContainerHighDark
- CobaltBlue
- IncomeEmerald
- ExpenseCoral
- WarningAmber
- TextPrimary
- TextSecondary
- TextMuted
- Space4
- Space8
- Space12
- Space16
- Space20
- RadiusSmall
- RadiusMedium
- RadiusLarge
- RadiusXLarge
- centralized typography tokens
- centralized Motion tokens

Final hardening included:
- Material 3 HorizontalDivider replacing deprecated Divider.
- + Sub action minimum 48dp.
- Category emoji picker minimum 40dp.
- Icon content descriptions verified.

---

## 14. ARCHITECTURAL LOCK

The following architectural domains remain explicitly protected from visual/UI changes:

- **ROOM**: Database, DAOs, Entities, migrations.
- **FIRESTORE**: Repositories, snapshot listeners, DTOs, rules, indexes.
- **AUTHENTICATION**: AuthRepository, FirebaseAuthRepository, Google Sign-In.
- **HOUSEHOLD / RBAC**: Household resolution, permission evaluation, invitation state, role evaluation.
- **SYNCHRONIZATION**: OutboundSyncEngine, SyncOutbox, SyncOutboxDao, SyncStatus, SyncDiagnosticsHolder.
- **VIEWMODELS**: MainViewModel, AuthViewModel, other business StateFlows.
- **DOMAIN**: FinancialAnalyticsEngine, financial calculations, conversion logic.
- **CSV / BNR**: Importer, Orchestrator, exporter, exchange-rate services.
- **NAVIGATION**: Routes, navigation graph, bottom navigation behavior.
- **BUILD**: Gradle configuration, dependency versions.

Future visual phases must NOT modify these areas unless explicitly declared as a separate technical phase.

---

## 15. TEST / BUILD BASELINE (PHASE 13)

Reported Phase 13 verification results:

**Unit Tests Execution:**
```text
gradle :app:testDebugUnitTest

Result:
- 72 tests executed
- 72 passed
- 0 failed
- 0 errors
- 1 intentionally skipped (finalizeTestRoborazziDebug)
- BUILD SUCCESSFUL
- 0 coroutine/memory leaks detected
- 0 UncompletedCoroutinesError detected
```

**Build Execution:**
```text
gradle :app:assembleDebug

Result:
- BUILD SUCCESSFUL
- Debug APK generated successfully
```

*(Note: These are the reported Phase 13 verification results).*

---

## 16. TEST TAG CONTRACT

Documented semantic/test-tag contract preserved across the presentation layer:

- **Dashboard:**
  - `dashboard_top_card`
  - `sync_status_indicator`
  - `card_savings_rate`
  - `card_expense_pressure`
- **Currency:**
  - `currency_toggle_RON`
  - `currency_toggle_EUR`
- **Transactions:**
  - `transaction_item_*`
  - `tx_duplicate_*`
  - `tx_delete_*`
  - `tx_input_amount`
  - `tx_input_desc`
  - `save_transaction_button`
- **Analytics:**
  - `analytics_smart_insights_card`
  - `analytics_eur_incomplete_warning_card`
- **Categories:**
  - `fab_add_category`
  - `category_card_*`
  - `edit_category_group_*`
  - `delete_category_group_*`
  - `edit_subcategory_*`
  - `delete_subcategory_*`
- **Settings:**
  - `account_info_card`
  - `account_uid_text`
  - `sign_out_button`
  - `pending_invitations_card`
  - `pending_invite_item`
  - `accept_invite_button`
  - `decline_invite_button`
  - `household_setup_card`
  - `create_household_button`
  - `export_csv_button`
  - `import_csv_button`
  - `retry_eur_conversions_button`
  - `run_bnr_diagnostic_button`
  - `view_sync_diagnostic_button`

Tags must not be renamed or removed during future UI work.

---

## 17. CURRENT ROADMAP

- Phase 1  — Foundation & Design System Tokens: STATUS: COMPLETE
- Phase 2  — Core UI Components & Library: STATUS: COMPLETE
- Phase 3  — Dashboard Visual Overhaul: STATUS: COMPLETE / AUDIT GO
- Phase 4  — Transactions Visual Overhaul: STATUS: COMPLETE / AUDIT GO
- Phase 5  — Transaction Form Visual Overhaul: STATUS: COMPLETE / AUDIT GO
- Phase 6  — Analytics Visual Overhaul: STATUS: COMPLETE / AUDIT GO
- Phase 7  — Categories Visual Overhaul: STATUS: COMPLETE / AUDIT GO
- Phase 8  — Settings & Household Visual Overhaul: STATUS: COMPLETE / AUDIT GO
- Phase 9  — Dialogs & Forms Polish: STATUS: COMPLETE / AUDIT GO
- Phase 10 — Empty / Loading / Error States: STATUS: COMPLETE / AUDIT GO
- Phase 11 — Motion Foundation: STATUS: COMPLETE / AUDIT GO
- Phase 12 — Accessibility + Responsive QA: STATUS: COMPLETE / AUDIT GO
- Phase 13 — Final Visual QA: STATUS: COMPLETE / AUDIT GO (GO — PHASE 13 CLEAN)

---

## 18. CURRENT MEMORY BASELINE

**CHECKPOINT NAME:**
FinTrack — Post Phase 13 Verified UI Baseline (2026-09-05)

**PRESENTATION LAYER:**
VERIFIED CLEAN AFTER PHASE 13

**DESIGN SYSTEM:**
FinTrack Design System v1 verified across presentation layer.

**MOTION:**
Centralized FinTrackMotion foundation verified.

**ACCESSIBILITY:**
Responsive and accessibility hardening completed.

**TEST CONTRACTS:**
Existing semantic/test tags preserved.

**ARCHITECTURAL LOCKS:**
Preserved across all 10 protected domains.

**BUSINESS LOGIC:**
No changes introduced by Phases 7–13.

**SYNC / AUTH / RBAC:**
Not modified by the UI phases.

**HISTORICAL UNRESOLVED / INVESTIGATION CONTEXT:**
Historical technical investigation context regarding sync permission issues and coroutine lifecycle problems remains documented and preserved.
