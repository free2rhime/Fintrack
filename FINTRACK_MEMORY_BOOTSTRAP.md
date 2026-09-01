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
- no deterministic category-ID hashing as identity;
- household-scoped CSV Category/SubCategory resolution and reuse: existing Category/SubCategory entities belonging to the active household must be reused during CSV import, preserving stable IDs and preventing duplicate entity creation;
- Expense vs Income separation and parent Category separation during CSV import matching;
- CSV import duplicate prevention does not automatically clean or alter existing duplicate records already stored in the database (historical duplicates preserved).

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
Git baseline: Step 12.3Y Real CSV Import / Transaction Visibility Verification Checkpoint (Steps 12.3S–12.3Y Complete)
Android test baseline: 380/380 PASS (Full Android JVM/Robolectric test cases passing, 0 failed, 0 errors, 0 skipped; 31/31 focused hard-delete/sync tests PASS; 8/8 targeted Account UI label tests PASS)
Firestore rules test baseline: 100/100 test cases preserved in tests/firestore.rules.test.ts and Firestore test suites
GitHub Actions baseline: Build Debug APK (.github/workflows/build-apk.yml) with safe Firebase configuration secret injection
Physical Device Smoke baseline: Step 12.2 PASS on Device A and Device B; Step 12.3 CSV Import real-device verification PASS; Step 12.3U Hard Delete real-device verification PASS (Transaction & Category permanent deletion verified on physical device and Firestore, SyncStatus = Synced, no tombstones created); Step 12.3Y Real 33-Row CSV Import & Period Filter Visibility Resolution PASS
Branch: main
Remote branch: origin/main
Working tree: clean / synchronized
```

### Transaction & Category Firestore Hard Deletion (Step 12.3S / 12.3T / 12.3U — COMPLETE)
- **Transaction Outbound Hard Delete:** `DefaultFirestoreSnapshotSource.deleteTransaction()` permanently removes the document from Firestore via `document.delete().await()` at `/households/{householdId}/transactions/{transactionId}`, replacing the legacy soft-delete (`isDeleted = true`).
- **Category Outbound Hard Delete:** Preserved direct permanent removal via `DefaultFirestoreSnapshotSource.deleteCategory()` (`document.delete().await()`) at `/households/{householdId}/categories/{categoryId}`.
- **Inbound REMOVED Change Processing:** `FirestoreSnapshotSource.listenToTransactions()` and `DefaultFirestoreSnapshotSource.listenToTransactions()` track `DocumentChange.Type.REMOVED`. `FirestoreSyncRepository.processTransactionSnapshot()` deletes local Room entities when remote transactions are deleted.
- **Outbox Shielding for Removals:** `processTransactionSnapshot()` checks `activeOutboxIds` before deleting local entities; if a local mutation is pending, it shields the local Room record, logs a conflict event (`UPDATE_VS_DELETE`), and invokes `onConflictDetected`.
- **Category Mirror Reconciliation & Data Safety:** Category mirror sync via `processCategorySnapshot()` and `deleteCategoriesNotIn()` physically deletes removed categories locally without cascading to historical transactions; existing transaction category and subcategory string attributes remain intact (no foreign-key cascade).
- **Backward Compatibility for Legacy Tombstones:** Inbound documents with `isDeleted == true` continue to be parsed safely and remove local Room entities.
- **Real-Device Physical Verification (Step 12.3U):** Real-device verification on physical hardware confirmed transaction hard deletion, category hard deletion, and clean `SyncStatus.Synced` state without creating soft-delete tombstones in Firestore.
- **Historical Firestore Data:** Existing historical documents with `isDeleted == true` created prior to Step 12.3S were not deleted and remain untouched in Firestore, requiring separate administrative cleanup.

### Account / Payment Method UI Label Localization (Step 12.3W / 12.3X — COMPLETE)
- **Internal Account Value vs UI Display Label:** Maintained strict decoupling between internal domain enum/string value `"Meal Tickets"` and localized Romanian UI display label `"Tichete de masa"`.
- **Data Model Compatibility:** Internal value `"Meal Tickets"` remains unchanged across Room, `TransactionEntity`, `TransactionDao`, Firestore DTOs, Firestore security rules, and CSV import format.
- **UI Localization:** UI components (selection dropdowns, filter chips, transaction cards, creation forms) display `"Tichete de masa"` for the Meal Tickets payment method. Card remains `"Card"`, Cash remains `"Cash"`.
- **No Database / Firestore Migration:** No database migration or Firestore schema modification was performed; the change is strictly an architectural UI layer mapping.
- **Category vs Account Distinction:** Preserved the distinct category rename (`💳 Meal Tickets` → `💳 Tichete de masa`), clarifying that category identity is separate from the Account internal value.
- **Test Baseline:** 8/8 targeted UI label tests PASS, full Android JVM/Robolectric test suite PASS.

### Real CSV Import & Historical Transaction Visibility Resolution (Step 12.3Y — COMPLETE)
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

### CSV Category & SubCategory Deduplication (Step 12.3L — COMPLETE)
- **Root Cause Resolved:** `CsvImportOrchestrator` now passes the active `householdId` when querying existing categories via `CategoryRepository.getAllCategoriesList(householdId)`.
- **Existing Category & SubCategory Reuse:** `CsvImporter.parseAndValidate()` accurately identifies existing household categories and subcategories, reusing their stable UUIDs instead of flagging them as missing.
- **Duplicate Prevention:** Prevents creating duplicate `CategoryEntity` records and sync outbox mutations on repeated CSV imports.
- **Strict Isolation & Scoping:** Preserves household isolation (categories in household A cannot be reused in household B), Expense vs. Income separation (same-name categories with different types remain distinct), and parent category hierarchy separation.
- **Step 12.3G Protection:** Preserves authenticated UID propagation, active `householdId`, immutable `createdByUid`, Room persistence, outbox queueing, and Firestore synchronization.
- **Existing Data Safety:** Prevents new duplicates on import; existing historical duplicate records in the database are not altered/deleted (tracked for separate cleanup audit).

### CSV Import Authenticated Context Propagation & PermissionDenied Fix (Step 12.3G)
- **Authenticated Context Propagation:** The CSV import pipeline propagates the authenticated user and active household context through `MainViewModel` → `CsvImportOrchestrator` → `CsvImporter` → `TransactionRepository` → `RoomTransactionRepository` → `Room` → `SyncOutbox` → `OutboundSyncEngine` → `Firestore`.
- **Preserved Transaction Attributes:** Imported transactions explicitly receive `householdId = activeHouseholdId`, `userId = authenticatedUserUid`, and `createdByUid = authenticatedUserUid`.
- **Resolved Visibility & Permission Denied:** Resolves the defect where imported transactions had `householdId = null` (invisible to Room household-filtered UI queries) and serialized fallback `"remote_user"` as `createdByUid` (triggering Firestore `PERMISSION_DENIED`).
- **No Rule Weakening:** Firestore security rules remain strictly enforced without modification.
- **Local Persistence Resilience:** Room persistence operates atomically; local transactions remain valid and queryable even if outbound network sync fails or encounters transient errors.
- **Real-Device Verification:** Verified on physical hardware with 33 transactions imported, BNR EUR rates converted, visible in UI, queryable in export, and synced cleanly to Firestore.

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
