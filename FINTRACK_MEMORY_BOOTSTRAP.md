# FinTrack Memory Bootstrap

## Purpose

This is the standard bootstrap instruction for any AI agent working on FinTrack.

It explains how to use project memory and how to work safely.

It is intentionally separate from the project memory itself.

---

## 1. REQUIRED CONTEXT LOADING

Before making any code change:

1. Read `FINTRACK_CURRENT_CONTEXT.md`.
2. Treat the current Git repository as the implementation source of truth.
3. Read `FINTRACK_PROJECT_MEMORY.md` when deeper historical or architectural context is required.
4. Use Git history when a historical implementation detail must be verified.
5. Do not assume that information from an older checkpoint is still current.

The three historical sources are:

- GitHub / repository → implementation and commit history
- ChatGPT project history → requirements, decisions and rationale
- Antigravity history → investigations, experiments and observations

When sources disagree, identify the conflict and verify against the current repository.

---

## 2. INFORMATION CLASSIFICATION

Distinguish clearly between:

- **VERIFIED** — supported by current repository/tests/runtime evidence.
- **HISTORICAL** — true at an earlier checkpoint.
- **OPEN** — known unfinished work.
- **UNKNOWN** — not sufficiently demonstrated.
- **CONFLICT** — sources disagree and verification is required.

Never silently convert HISTORICAL, OPEN or UNKNOWN information into VERIFIED current state.

---

## 3. BEFORE IMPLEMENTATION

At the beginning of a development session report:

1. Current Git commit.
2. Working-tree status.
3. Relevant VERIFIED state.
4. Relevant OPEN items.
5. The explicitly selected task.
6. Relevant non-regression constraints.

Do not begin implementation until the task and scope are clear.

Do not select an OPEN item automatically merely because it appears in project memory.

---

## 4. DEVELOPMENT PROTOCOL

Use this sequence:

```text
ANALYZE
→ HYPOTHESIS
→ VERIFY
→ PROPOSE MINIMAL CHANGE
→ MODIFY
→ COMPILE
→ RUN TARGETED TESTS
→ INTERPRET RESULT
→ INSPECT DIFF
→ COMMIT
→ PUSH
→ UPDATE MEMORY IF NECESSARY
```

Rules:

- Explain the suspected cause and proposed solution before modifying code.
- Identify exact files/classes/methods before changing code.
- Prefer small, reversible changes.
- Create/propose a backup before important changes.
- Compilation alone is never proof that a problem is solved.
- Run tests relevant to the changed behavior.
- Do not modify tests simply to make them pass.
- Do not hide production defects with artificial delays or sleeps.
- Do not perform broad refactors when a smaller verified change is sufficient.

---

## 5. SYNCHRONIZATION INVESTIGATION

For synchronization problems, analyze these areas separately:

1. Authentication
2. Household resolution
3. Firestore authorization / Security Rules
4. Inbound synchronization
5. Outbound synchronization
6. `SyncStatus`
7. Firestore listeners
8. Room / outbox
9. Coroutine / Job lifecycle

Do not describe a synchronization problem simply as "Firestore sync is broken" without identifying which layer is failing.

---

## 6. COROUTINE / TEST LIFECYCLE

When `UncompletedCoroutinesError` or a hanging coroutine test occurs:

1. Identify the surviving coroutine/job.
2. Identify which component owns it.
3. Identify how its lifecycle is started.
4. Identify how it is expected to stop/cancel.
5. Reproduce the failure.
6. Only then propose a production-code or test change.

Never add arbitrary sleeps, delays or timeout extensions just to make the test pass.

---

## 7. FIRESTORE / HOUSEHOLD SAFETY

Never weaken authorization rules to solve synchronization symptoms.

Preserve:

- household isolation;
- explicit household resolution;
- default-deny security principles;
- OWNER/MEMBER RBAC;
- shared household financial data.

Never introduce a synthetic/fallback household merely to keep synchronization moving.

---

## 8. CATEGORY & MEMBER NON-REGRESSION RULES

Preserve the current category and member authorization architecture:

- household-scoped categories;
- stable UUID identity;
- shared household category structure;
- OWNER category management (OWNER may create, update, delete categories/subcategories; ADMIN permissions preserved; MEMBER cannot mutate categories);
- MEMBER consumption;
- MEMBER MANAGEMENT is OWNER-ONLY (household member management, invitation administration, role changes, member removal);
- invitation security (atomic token binding, pre-commit PENDING check, replay protection, household binding, email verification);
- initial seeding only when a household is explicitly created;
- no recurring startup category seeding;
- no deterministic category-ID hashing as identity.

Do not broaden transaction permissions into category or member management permissions.

---

## 9. TRANSACTION / OUTBOX NON-REGRESSION RULES

Preserve:

- household-scoped transactions;
- cross-user transaction editing & deletion: all active household members may create, edit, and delete transactions within their authorized household (transaction creator is not an authorization boundary);
- immutable `createdByUid` preservation: `createdByUid` represents original creator audit identity and must NOT be overwritten when another member edits a transaction;
- Room/outbox synchronization;
- FAILED outbox shielding: `SyncOutboxDao.getActiveEntityIdsByType` includes `FAILED` entries to prevent stale inbound snapshots from overwriting local un-synced/failed mutations;
- active household preservation: `MainViewModel.activeHouseholdId` preserves resolved household context during `SyncStatus.PermissionDenied` and `SyncStatus.Offline` states;
- bidirectional transaction synchronization;
- deletion propagation;
- correct Expense vs. Income semantics;
- Firestore serialization rules;
- omission of `migrationId` when it is null.

Do not perform a broad coroutine/outbox rewrite without new reproducible evidence.

---

## 10. SYNCSTATUS RULE

`SyncStatus.Synced` represents:

- completed inbound transaction snapshot handshake;
- completed inbound category snapshot handshake;
- valid active household;
- absence of active inbound error;
- zero `PENDING` outbox entries;
- zero `IN_PROGRESS` outbox entries;
- zero `FAILED` outbox entries.

`SyncStatus.Connecting` / `"Syncing..."` represents inbound handshake in progress OR unresolved active outbound work (`PENDING` or `IN_PROGRESS`).

`FAILED` outbox entries map to:
- `SyncStatus.PermissionDenied` (when `errorCode == "PERMISSION_DENIED"`);
- `SyncStatus.Offline` (for other fatal outbound errors).

`OutboundSyncEngine` processing is decoupled from public `SyncStatus`, enabling queue processing whenever inbound sync is healthy even while the public status is `Connecting`/`Syncing`.

Outbox state is evaluated using direct/finite Room queries and event-driven notifications from `OutboundSyncEngine`, avoiding infinite Room Flow collection loops.

---

## 11. UI SAFETY

FinTrack's current design direction includes:

- dark-first identity;
- green for primary/positive/income;
- blue for navigation/selection;
- red for expense/destructive semantics;
- five-tab navigation;
- visible synchronization status;
- visible household context.

UI work should not change backend or synchronization architecture unless explicitly required.

---

## 12. CURRENT BASELINE

At the time this bootstrap was updated:

```text
Git baseline: 1bef33f
Commit: feat: automate BNR EUR conversion for CSV imports
Previous baseline: 7a8b6bf (docs: update project memory after step 12.1J) / aed996f (ci: enable Firebase-configured online APK builds) / 14f5338 / 0da6b96 / 4ed7894 / 1ed28ec / 37155bc / 32fc27b / a739400 / baf2f70
Android test baseline: 355/355 PASS (0 failed, 0 errors, 0 skipped; 16/16 focused CsvImportOrchestrator tests)
Firestore rules test baseline: 95/95 test cases preserved in tests/firestore.rules.test.ts
GitHub Actions baseline: Build Debug APK (.github/workflows/build-apk.yml) with safe Firebase configuration secret injection
Physical Device Smoke baseline: Step 12.2 PASS on Device A and Device B
Branch: main
Remote branch: origin/main
Working tree: clean
```

### CSV Import & Automatic BNR EUR Conversion (Step 12.3 — 1bef33f)
- **Automatic RON → EUR Conversion:** When importing RON transactions via CSV, `CsvImportOrchestrator` automatically resolves official BNR exchange rates and calculates `amountEUR` during pre-import validation/preview.
- **Historical Rate Resolution & Caching:** Resolves official rate for each transaction date via `TransactionRepository.getOfficialRate()`; distinct dates reuse resolved rates; weekend and public holiday dates fall back to the preceding publishing day via existing BNR effective-date rules.
- **Existing Service Reuse:** Reuses existing `ExchangeRateService.calculateAmountEUR` for rounding and computation consistency.
- **Conversion Metadata:** Populates `exchangeRate`, `exchangeRateDate`, `exchangeRateSource = "BNR"`, and `conversionStatus = ConversionStatus.AUTO_CONVERTED` when an official rate is available.
- **Safe Offline / Unavailable Fallback:** If offline or if no rate is available for a date, safely falls back to `conversionStatus = ConversionStatus.PENDING`, `amountEUR = 0.0`, `exchangeRate = 0.0`, and `exchangeRateSource = "NONE"` without failing or rejecting the import.
- **Preview & Persistence Consistency:** The preview dialog displays the exact converted EUR amounts and resolved exchange rates; the subsequent atomic import into Room and Outbox persists identical values.
- **Duplicate Mode Compatibility:** Fully compatible with both `SKIP_EXISTING` and `UPDATE_EXISTING` modes.
- **Stateless Parser & Clean Separation:** `CsvImporter` remains a purely stateless CSV parser/validator; `CsvImportOrchestrator` handles rate resolution, domain orchestration, and backup generation.
- **Security & Data Integrity:** Household isolation, cross-user transaction permissions, immutable `createdByUid`, atomic Room persistence, and Outbox generation remain strictly preserved. Direct Firestore writes are not performed.

### Cross-User Transaction Permission & Data Integrity (Step 12.1 — 4ed7894)
- **Cross-User Transaction Mutations:** All active household members may create, update, and delete transactions in their household (`firestore.rules`). Transaction creator identity (`createdByUid`) is preserved immutably across cross-user edits (`FirestoreDtos.kt`).
- **FAILED Outbox Shielding:** `SyncOutboxDao.getActiveEntityIdsByType` includes `FAILED` status, protecting local un-synced edits from destructive overwrite by stale inbound remote snapshots (`SyncOutboxDao.kt`, `Stage9OutboxShieldTest`).
- **Active Household Preservation:** `MainViewModel.activeHouseholdId` preserves resolved household ID during `SyncStatus.PermissionDenied` and `SyncStatus.Offline` states without falsely converting error status to `Synced` (`MainViewModel.kt`, `CategoryPermissionsTest`).
- **Preserved Boundaries:** Category/subcategory mutations remain OWNER/ADMIN-only. Household member management and invitation administration remain OWNER-only. Cross-household isolation strictly enforced.

### CI Baseline Cleanup & Firebase-Configured Online APK (Steps 12.1G–12.1I — 14f5338 / aed996f)
- **GitHub Actions Workflows Removed:** Redundant workflows (`build-debug-apk.yml`, `unit-tests.yml`, `firestore-rules-tests.yml`) removed from `.github/workflows/`.
- **Retained Workflow:** `.github/workflows/build-apk.yml` ("Build Debug APK") actively retained for release artifact generation.
- **Firebase Secret Handling (Step 12.1I — aed996f):** Temporarily reconstructs `app/google-services.json` from `secrets.GOOGLE_SERVICES_JSON` during workflow execution; validates JSON structure safely without secret logging; cleans up the file in an `always()` post-step. `google-services.json` remains strictly outside the Git repository.
- **Online APK Status:** PASS on CI; builds signed release-compatible debug artifact with real Firebase configuration.
- **Testing & Security Preserved:** Android 343/343 tests passing locally; Firestore security rules test suite (`tests/firestore.rules.test.ts`) preserved.

### Physical Two-Device Beta Smoke Test Regression (Step 12.2 — COMPLETE)
- **Two-Device Setup:** Successfully verified on physical Device A and Device B.
- **Authentication:** Google Sign-In verified on real devices with Firebase Authentication.
- **Household & Permissions:** Household creation, member invitations, and role boundaries verified.
- **Cross-User Transactions:** Non-creator edit/delete and real-time bidirectional sync verified PASS. Historical bug with `PermissionDenied` on cross-user edits confirmed resolved.
- **Outbox & Offline:** Local persistence, outbox recovery on reconnection, and foreground synchronization verified PASS.
- **Lifecycle:** App restart, FirebaseAuth session restoration, and sync recovery verified PASS.

Always verify these values before acting; they are a baseline, not an instruction to assume the repository has not changed.

---

## 13. MEMORY MAINTENANCE

Do not copy entire conversations into project memory.

After a meaningful validated change:

### Update `FINTRACK_CURRENT_CONTEXT.md` when:
- the current checkpoint changes;
- a major issue becomes VERIFIED/CLOSED;
- an important issue becomes OPEN;
- the next task changes;
- a non-regression rule changes.

### Update `FINTRACK_PROJECT_MEMORY.md` when:
- architecture changes;
- a major product/technical decision is made;
- a significant historical reconciliation is completed;
- a new durable non-regression rule is established.

Do not update either file for trivial implementation details that have no durable project-state impact.

---

## 14. FINAL VERIFICATION BEFORE CLAIMING SUCCESS

Before saying a task is complete:

1. Verify the actual Git diff.
2. Compile the affected module.
3. Run the relevant targeted tests.
4. Run broader tests when appropriate.
5. Interpret failures instead of ignoring them.
6. Confirm the repository state.
7. If the change is meaningful, update the appropriate memory/checkpoint.

Use precise language:

- "implemented" = code changed;
- "compiles" = build succeeded;
- "targeted test passes" = specific behavior tested;
- "verified" = evidence supports the claim;
- "resolved" = the original reproducible problem is demonstrated as fixed.

Do not use "resolved" based only on compilation.

---

## 15. COMMUNICATION STYLE

When reporting work, use:

```text
CAUSE
SOLUTION
FILES
CHANGES
VERIFICATION
RESULT
NEXT STEP
```

Keep reports concise, factual and evidence-based.

If uncertain, say so.

If sources conflict, stop and surface the conflict instead of guessing.

---

## 16. FIRST ACTION IN A NEW SESSION

Before touching code:

```text
1. Read FINTRACK_CURRENT_CONTEXT.md
2. Verify Git state
3. Read FINTRACK_PROJECT_MEMORY.md if needed
4. State the current relevant context
5. State the selected task
6. State the proposed verification path
7. Wait for/execute the explicitly selected task
```

The goal is continuity without loading the entire historical conversation into every session.

## OPTIONAL DEVELOPMENT SKILLS

Use these skills only when they materially improve the task:

* `research` — investigation and source-based analysis.
* `debug` — root-cause diagnosis of defects.
* `architecture` — architectural analysis and design decisions.
* `implement` — controlled implementation of an approved solution.
* `verify` — fresh evidence that the change works.

Skills are optional tools, not mandatory workflow stages.
FinTrack project rules and Project Memory take precedence over any skill instructions.
