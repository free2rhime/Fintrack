# FINTRACK PROJECT MEMORY v2

> Canonical compact operational memory for the FinTrack project.
> Last reconciled: 2026-08-29
> Current verified Git checkpoint: `baf2f70`
> Previous functional baseline: `873017a`

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

Repository state verified on 2026-08-29:

```text
Branch:       main
HEAD:         baf2f70
origin/main:  baf2f70
Working tree: clean
Remote:       https://github.com/free2rhime/Fintrack.git
```

Commit:

`baf2f70 fix: harden firestore security rules`

The repository contains a chronological Git history from the initial commit through `873017a` to `baf2f70`.

### Relationship to Previous Baseline:

```text
873017a
fix: align sync status with outbox state

        ↓

Step 8 security hardening (self-join closure, role protection, financial validation)

        ↓

baf2f70
fix: harden firestore security rules
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
- OWNER/MEMBER/ADMIN RBAC (with owner-only privilege for role/status mutations and member deletions);
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
- OWNER-managed;
- MEMBER consumption;
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
Outbox
  ↓
OutboundSyncEngine
  ↓
Firestore

Firestore
  ↓
snapshot/listener
  ↓
FirestoreSyncRepository
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

These statements mean the corresponding implementation work exists at the current checkpoint. They do **not** imply that every possible runtime edge case has been exhaustively verified.

---

# 8. OPEN WORK

## Current Development Priorities:
1. **Phase 1 — COMPLETED:** Firestore Security Hardening (`baf2f70`).
2. **Phase 2 — NEXT (P1):** Android Test Suite & Migration Stabilization (13 historical failures).
3. **Phase 3:** Outbox Reliability & Recovery Polish.
4. **Phase 4:** Incremental Architecture Cleanup.
5. **Phase 5:** Beta Release Candidate / Multi-Device Verification.

## Reconciled Open Items:

### OPEN-1 — Offline/recovery synchronization
Need explicit verification of behavior when devices lose connectivity and later recover.

### OPEN-2 — Outbox failure/retry/recovery semantics
Need explicit validation of failure states, retry behavior and recovery beyond current coverage.

### OPEN-3 — Concurrent household edits
Need defined conflict behavior when multiple household members modify shared data concurrently.

### OPEN-4 — Room ↔ Firestore mirror integrity
Need explicit end-to-end verification that local and remote representations remain consistent.

### OPEN-5 — End-to-end migration verification
Migration state exists, but complete end-to-end execution needs verification.

### OPEN-6 — Full regression suite (P1 NEXT PRIORITY)
The complete unit/Robolectric test matrix has 13 historical failures from migration/preflight test suite debt (Room `MigrationTestHelper` schema assets, Stage 2B preflight fixtures, Compose UI preview semantics).

### OPEN-7 — SyncStatus outbound-health semantics (RESOLVED in Step 7.9 — 873017a)
Resolved: `SyncStatus.Synced` requires both inbound handshake readiness and zero unresolved outbox work (0 PENDING, 0 IN_PROGRESS, 0 FAILED). Outbound engine is decoupled from public UI status.

### OPEN-8 — Firestore Rules Security (RESOLVED in Step 8 — baf2f70)
Resolved: Closed self-join bypass, role escalation, and invalid financial values. Verified with 92/92 Firestore emulator tests and TypeScript compilation.

### OPEN-9 — Household Offline-First (INVALID / DESIGN DECISION)
Household creation and invitation management are intentionally online-only control-plane operations.

### OPEN-10 — Transaction Hard-Delete Mirror (CAN DEFER / P3)
Normal application operations use the verified soft-delete model. Physical deletion mirroring can be evaluated as a future enhancement.

---

# 9. UNKNOWN

Currently not fully demonstrated:

- full test-suite green state across all edge cases;
- prolonged offline/high-latency behavior;
- complete runtime behavior under degraded network recovery;
- complete end-to-end migration behavior;
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
8. artificial delays to mask coroutine lifecycle defects;
9. arbitrary coroutine cancellation/restart logic;
10. broad rewrites without reproducible evidence.

Preserve:

- household isolation;
- default-deny security;
- OWNER/MEMBER RBAC;
- shared household financial data;
- Room/outbox architecture;
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
```

The current baseline is:

```text
baf2f70
fix: harden firestore security rules
```

Previous functional baseline: `873017a`.

The next development task must be explicitly selected rather than inferred from the OPEN list.

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
| Security Gate | **PASS** | Firestore security rules hardened; P0-1, P0-2, P1 resolved; 92/92 emulator rules tests passing |
| Inbound/Outbound Sync Gate | **PASS** | Handshake, outbox draining, and SyncStatus alignment verified |
| Full Android Regression Gate | **NOT YET PASS** | 317/330 passing; 13 historical migration/UI preview test fixture failures remain |
| Migration Verification Gate | **OPEN** | Migration execution & preview fixture debt to be stabilized in Step 9 |
| Multi-Device Production Smoke | **NOT YET VERIFIED** | Requires physical/multi-device smoke validation |

**Overall Beta Status: NOT READY**
*Reason:* Migration and test suite debt (13 historical failures) and multi-device release verification, not Firestore security rules.
