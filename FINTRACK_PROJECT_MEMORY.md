# FINTRACK PROJECT MEMORY v2

> Canonical compact operational memory for the FinTrack project.
> Last reconciled: 2026-08-28
> Current verified Git checkpoint: `951b4aa`

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

Repository state verified by the user on 2026-08-28:

```text
Branch:       main
HEAD:         951b4aa
origin/main:  951b4aa
Working tree: clean
Remote:       https://github.com/free2rhime/Fintrack.git
```

Commit:

`951b4aa fix: stabilize transaction outbound sync`

The repository contains a 72-commit chronological history from the initial commit to `951b4aa`.

### Important historical correction

Some earlier handoff/context files identify `cea443f` as the latest checkpoint.

That was historically correct at the time those files were produced, but it is **NOT the current checkpoint**.

Current checkpoint = **`951b4aa`**.

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
- OWNER/MEMBER RBAC;
- collection-group access only where explicitly authorized;
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

Current `SyncStatus.Synced` indicates successful completion of the inbound Firestore listener/handshake.

It does **not** guarantee:

- outbox empty;
- outbound writes successful;
- all pending local mutations confirmed in Firestore.

Therefore:

```text
Inbound synchronization complete
        ≠
All outbound work complete
```

Coexistence of `Synced` with `PENDING` or `FAILED` outbound work is an **OPEN design area**, not automatically a confirmed bug.

Do not redefine `SyncStatus.Synced` without an explicit architectural/product decision and tests.

---

# 7. CURRENT VERIFIED / RESOLVED ITEMS

At checkpoint `951b4aa` the following have been addressed:

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

These statements mean the corresponding implementation work exists at the current checkpoint. They do **not** imply that every possible runtime edge case has been exhaustively verified.

---

# 8. OPEN WORK

Known open areas:

## OPEN-1 — Offline/recovery synchronization
Need explicit verification of behavior when devices lose connectivity and later recover.

## OPEN-2 — Outbox failure/retry/recovery semantics
Need explicit validation of failure states, retry behavior and recovery.

## OPEN-3 — Concurrent household edits
Need defined conflict behavior when multiple household members modify shared data concurrently.

## OPEN-4 — Room ↔ Firestore mirror integrity
Need explicit end-to-end verification that local and remote representations remain consistent.

## OPEN-5 — End-to-end migration verification
Migration state exists, but complete end-to-end execution needs verification.

## OPEN-6 — Full regression suite
The complete unit/Robolectric test matrix has not been promoted to VERIFIED green status merely from successful compilation or localized tests.

## OPEN-7 — SyncStatus outbound-health semantics
Need an explicit design decision if the UI/status model should distinguish inbound synchronization completion from outbound queue health.

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
```

The current baseline is:

```text
951b4aa
fix: stabilize transaction outbound sync
```

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
