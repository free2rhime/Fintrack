# FINTRACK CURRENT CONTEXT

> Compact operational context for continuing FinTrack development.
> Last verified: 2026-08-29
> Git baseline: `baf2f70`
> Previous functional baseline: `873017a`

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
HEAD:         baf2f70
origin/main:  baf2f70
Remote:       https://github.com/free2rhime/Fintrack.git
```

At the time this context was verified, the repository working tree was clean.

Current commit:

`baf2f70 fix: harden firestore security rules`

Previous functional baseline:

`873017a fix: align sync status with outbox state`

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
- **Android Regression: 317/330 PASS (0 new regressions).** 13 historical UI/migration preview test fixture failures remain.

### Categories
- Categories are household-scoped.
- Stable UUIDs are used as identity.
- Categories are shared by household members.
- OWNER manages category hierarchy.
- Initial default-category seeding occurs only on explicit household creation.
- Startup/repeated category seeding loops were removed.
- Deterministic category-ID hashing is not used.
- Duplicate default-category creation/reconciliation was addressed.
- Historical transactions must remain protected when categories are deleted.

### Transactions
- Transactions are household-scoped.
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
2. **Phase 2 — NEXT (P1):** Android Test Suite & Migration Stabilization (resolving the 13 historical test failures: Room `MigrationTestHelper` schema assets, Stage 2B preflight fixtures, Compose UI preview semantics).
3. **Phase 3:** Outbox Reliability & Recovery Polish.
4. **Phase 4:** Incremental Architecture Cleanup.
5. **Phase 5:** Beta Release Candidate / Multi-Device Verification.

### Open Areas:
1. P1: Test suite stabilization (13 historical migration/preflight test failures).
2. Offline/recovery synchronization handling beyond current coverage.
3. Outbox failure, retry and error-recovery semantics where still incomplete.
4. Concurrent/conflicting household edits resolution.
5. Room ↔ Firestore mirror integrity verification.
6. End-to-end migration execution verification.

## 7. UNKNOWN / NOT FULLY VERIFIED

- Full unit/Robolectric regression suite green state across all edge cases (13 historical failures pending).
- Degraded-network and prolonged-offline runtime behavior.
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
- per-user financial/category isolation inside a shared household.

Preserve:

- strict household isolation;
- default-deny Firestore security;
- OWNER/MEMBER RBAC;
- Room/outbox synchronization;
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
