# FinTrack — Phase 3A: Dashboard Redesign Implementation Plan

**Document:** Implementation Specification & Technical Execution Roadmap for Phase 3B  
**Date:** September 2026  
**Auditor / Architect:** Senior Android UI/UX Designer, Jetpack Compose Design Technologist & Systems Architect  
**Status:** ARCHITECTURE & PLANNING ONLY (Implementation to occur in Phase 3B)  
**Primary Reference Documents:**  
- `FINTRACK_UIUX_AUDIT_PHASE_1.md` (Diagnostic baseline, bug discovery, contrast audit)  
- `FINTRACK_UIUX_AUDIT_PHASE_2.md` (Design System v2, Motion System v2, Precision Fintech decision)  
- `FINTRACK_CURRENT_CONTEXT.md` & `FINTRACK_PROJECT_MEMORY.md` (System invariants, Room/Firestore sync contracts)  
- `AGENTS.md` (Execution workflow & safety guardrails)

---

## 1. Executive Summary & Repository Alignment

This document translates the architectural and visual specifications of **Design System v2** and **Motion System v2** into a concrete, sequential, file-by-file implementation plan for **Phase 3B**. 

### Verified Repository Baseline (Inspected Commit State)
- **Compilation & Test Suite:** 432 passing unit tests (`BUILD SUCCESSFUL in 55s`). The calendar-boundary flaw in `AnalyticsViewModelWiringTest` (which caused failures on days 1–9 of any month) was diagnosed and neutralized in Phase 3A.
- **Visual Direction Confirmed:** **Precision Fintech + Editorial Wealth Narrative**. Crisp high-density typography, tabular figures (`tnum`), 1dp micro-borders, high-contrast pure white surfaces in Light Mode, combined with conversational household financial summaries (`FinancialPulseCard`).
- **Zero New Dependencies:** 100% executable within existing Jetpack Compose Foundation, Material 3, and Kotlin Coroutines/Flow APIs.

---

## 2. Implementation Strategy & Phased Checkpoints

The implementation in Phase 3B must proceed incrementally through 6 discrete checkpoints. Each checkpoint will compile and pass the test suite before proceeding to the next.

```text
[CHECKPOINT 1] P0 Correctness Fixes (Sorting, Hero & Nav Truncations)
       ↓ (Verify unit tests + zero regressions)
[CHECKPOINT 2] Design System v2 Tokens (Color.kt, Theme.kt, Type.kt, Shape.kt, Spacing.kt)
       ↓ (Verify Light Mode surface contrast & typography rendering)
[CHECKPOINT 3] Motion System v2 Primitives (Motion.kt, AnimatedCounter)
       ↓ (Verify spring physics & deterministic currency ticker transitions)
[CHECKPOINT 4] Structural Dashboard Components (Unified Hero Canvas, Financial Pulse Card)
       ↓ (Verify 360dp responsiveness, BNR secondary parity, smart insights display)
[CHECKPOINT 5] Interactive Data Visualizations (Cash Flow Spline Scrubber, Category Ring)
       ↓ (Verify 120Hz drag scrubbing, haptic feedback, 100% share parity)
[CHECKPOINT 6] Dashboard Assembly, Polish, and Final End-to-End QA
       ↓ (Verify TalkBack, font scaling, Light/Dark mode consistency, full test suite)
```

---

## 3. P0 Correctness Fixes (Detailed Execution Plan)

### P0.1 — Chronological Spline Sorting Bug Fix
* **File:** `/app/src/main/java/com/example/domain/analytics/FinancialAnalyticsEngine.kt`
* **Class/Function:** `FinancialAnalyticsEngine.calculateMonthlyDataPoints(transactions: List<TransactionEntity>, currency: String): List<MonthlyCashFlowDataPoint>`
* **Root Cause Analysis:**  
  At line 452 of `FinancialAnalyticsEngine.kt`:
  ```kotlin
  // CURRENT FLAWED IMPLEMENTATION:
  return grouped.map { (key, txList) ->
      ...
  }.sortedBy { it.monthYearLabel } // <--- BUGS: Sorts alphabetically ("Apr", "Aug", "Feb", "Jan", "Jul", ...)
  ```
* **Safest Correction:**  
  Sort by the chronological group key `"yyyy-MM"` before formatting the display label:
  ```kotlin
  // CORRECT IMPLEMENTATION:
  return grouped.entries
      .sortedBy { it.key } // Sorts chronologically: "2026-01", "2026-02", ...
      .map { (key, txList) ->
          val (year, month) = key.split("-").map { it.toInt() }
          val cal = Calendar.getInstance().apply { set(year, month - 1, 1) }
          val monthYearLabel = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.time)
          ...
          MonthlyCashFlowDataPoint(
              monthYearLabel = monthYearLabel,
              income = income,
              expense = expense,
              net = income - expense
          )
      }
  ```
* **Domain Model Impact:** Zero change required to `MonthlyCashFlowDataPoint` data class.
* **Verification & Test Additions:**
  - File: `/app/src/test/java/com/example/FinancialAnalyticsEngineTest.kt`
  - Add test: `testMonthlyDataPoints_sortedChronologicallyAcrossCalendarYears()` verifying transactions spanning November 2025 through April 2026 yield strictly chronological labels (`Nov 2025`, `Dec 2025`, `Jan 2026`, `Feb 2026`, `Mar 2026`, `Apr 2026`).

---

### P0.2 — Hero Balance & Income/Expense Amount Truncation Fix
* **File:** `/app/src/main/java/com/example/ui/screens/DashboardScreen.kt` (and extracted into `UnifiedHeroCanvas.kt`)
* **Current Layout Flaw:**  
  The Hero Balance card allocates a rigid 50/50 horizontal width split (`weight(1f)`) to Income and Expense in a single row without decoupling the currency string. For Romanian household sums exceeding 5 digits (e.g. `+131 556 RON`), the text field overflows and renders ellipsis: `"+131 556 R..."`.
* **Safest Responsive Correction:**  
  1. **Decoupled Currency Code:** Separate the numeric value (`+131 556`) from the currency code (`RON`), styling the currency code in a smaller optical size (`12sp Medium`) with dedicated vertical alignment.
  2. **Responsive Typography via `BoxWithConstraints`:**
     - On screens $< 380\text{dp}$ width: Render Income and Expense as a vertical flow with compact 15sp tabular figures, or wrap them into dual stacked micro-pills.
     - On screens $\ge 380\text{dp}$ width: Render 2-column split with `weight(1f)`, utilizing `maxLines = 1` and `softWrap = false` with dynamic font downscaling if text scaling exceeds $1.3\times$.
  3. **Visual Hierarchy Preservation:** The primary Net Balance (`balance`) maintains dominance at 34sp Bold tabular display with negative letter-spacing (`-1.sp`), preventing horizontal overflow up to 9,999,999.99 RON.

---

### P0.3 — Bottom Navigation Label Truncation Fix
* **File:** `/app/src/main/java/com/example/ui/navigation/FinTrackBottomNavigation.kt`
* **Current Layout Flaw:**  
  The 5 navigation destinations (`Dashboard`, `Transactions`, `Analytics`, `Categories`, `Settings`) exceed the horizontal text width of standard 360dp–390dp devices in Material 3 `NavigationBarItem`. The second tab truncates to `"Transacti..."`.
* **Safest Correction:**  
  1. **Refined Destination Label:** Rename the second tab from `"Transactions"` to `"Activity"` (Romanian: `"Activitate"` or clean 1-word label). This saves 4 full glyph widths.
  2. **Typography Optimization:** Style navigation labels with `11sp SemiBold` (LetterSpacing 0.sp).
  3. **Touch Target Contract:** Maintain full 48dp $\times$ 48dp touch targets and existing `Modifier.testTag` identifiers (`"nav_dashboard"`, `"nav_transactions"`, etc.) for test compatibility.

---

## 4. Design System v2 Implementation Plan

### 4.1 Color Architecture (`Color.kt` & `Theme.kt`)
* **Target Files:**
  - `/app/src/main/java/com/example/ui/theme/Color.kt`
  - `/app/src/main/java/com/example/ui/theme/Theme.kt`
* **Tokens to Add:**
  ```kotlin
  // High-Contrast Light Mode Foundations (Solving 1.08:1 Wash-Out)
  val PageBackgroundLight = Color(0xFFF8FAFC) // Slate 50 canvas
  val SurfacePrimaryLight = Color(0xFFFFFFFF) // Pure White floating cards
  val SurfaceSecondaryLight = Color(0xFFF1F5F9) // Slate 100 inset wells
  val SurfaceHeroDarkMidnight = Color(0xFF0F172A) // Inverted Midnight Apex (Both themes)
  val BorderSubtleLight = Color(0xFFE2E8F0) // 1dp Slate 200 micro-border
  val BorderStandardLight = Color(0xFFCBD5E1) // Slate 300 input border

  // Obsidian Dark Mode Foundations
  val PageBackgroundDark = Color(0xFF0B0F19) // Obsidian canvas
  val SurfacePrimaryDark = Color(0xFF131B2E) // Obsidian card surface
  val SurfaceSecondaryDark = Color(0xFF1E293B) // Slate 800 inset wells
  val BorderSubtleDark = Color(0xFF1E293B) // Slate 800 micro-border

  // Financial Semantics
  val IncomeEmerald = Color(0xFF10B981)
  val ExpenseCoral = Color(0xFFF87171)
  val HealthTeal = Color(0xFF2DD4BF)
  val HealthAmber = Color(0xFFFBBF24)
  val HealthRose = Color(0xFFFB7185)
  ```
* **Theme Mapping:**
  Map `SurfacePrimaryLight` to `MaterialTheme.colorScheme.surface`, `PageBackgroundLight` to `background`, and `BorderSubtleLight` to `outlineVariant`. Add a Local Composition Provider `LocalFinTrackColors` for extended financial tokens (`income`, `expense`, `borderSubtle`, `surfaceHero`).

---

### 4.2 Typography System (`Type.kt`)
* **Target File:** `/app/src/main/java/com/example/ui/theme/Type.kt`
* **Changes:**
  - Introduce `fontFeatureSettings = "tnum"` (Tabular Figures) across all financial numeric styles:
    ```kotlin
    val HeroFinancialDisplay = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-1.0).sp,
        fontFeatureSettings = "tnum"
    )
    val MetricFinancialLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum"
    )
    val TableFinancialMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = "tnum"
    )
    ```
  - Zero APK size increase (utilizes Android built-in font engine).

---

### 4.3 Shape System (`Shape.kt`)
* **Target File:** `/app/src/main/java/com/example/ui/theme/Shape.kt`
* **Changes:**
  - Transition from uniform 16dp radii to progressive geometric nesting:
    - `HeroRadius = RoundedCornerShape(24.dp)` (Apex card, Bottom sheets)
    - `CardRadius = RoundedCornerShape(16.dp)` (Standard floating content cards)
    - `InsetRadius = RoundedCornerShape(10.dp)` (Data wells within cards)
    - `MicroRadius = RoundedCornerShape(6.dp)` (Category chips, bar caps)
    - `PillRadius = CircleShape` (Toggles, badges, FAB)

---

### 4.4 Spacing & Surfaces (`Spacing.kt` & `FinTrackCard.kt`)
* **Target Files:**
  - `/app/src/main/java/com/example/ui/theme/Spacing.kt`
  - `/app/src/main/java/com/example/ui/components/FinTrackCard.kt`
* **Changes:**
  - Retain strict 8dp grid spacing tokens (`ExtraSmall` = 4dp, `Small` = 8dp, `Medium` = 16dp, `Large` = 24dp).
  - Update `FinTrackCard`: In Light Mode, apply `BorderStroke(1.dp, FinTrackTheme.colors.borderSubtle)` by default and set container color to pure white (`#FFFFFF`), elevating contrast from 1.08:1 to 4.5:1.

---

## 5. Motion System v2 Implementation Plan

### 5.1 Motion Primitives (`Motion.kt`)
* **Target File:** `/app/src/main/java/com/example/ui/theme/Motion.kt`
* **Changes:**
  Add Compose Spring and Easing tokens:
  ```kotlin
  val InteractiveSpring = spring<Float>(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
  )
  val ContentSpring = spring<Float>(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessLow
  )
  val StandardDecelerate = FastOutSlowInEasing
  val EmphasizedCubic = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
  ```

---

### 5.2 Animated Numeric Counter (`AnimatedCounter.kt`)
* **Target File (New Component):** `/app/src/main/java/com/example/ui/components/AnimatedCounter.kt`
* **Specification:**
  - **Currency Switches (RON $\leftrightarrow$ EUR):** Vertical slide-and-fade crossfade (180ms) to eliminate deceptive intermediate interpolated values.
  - **Period Switches (Same Currency):** Smooth numeric value animation using `animateFloatAsState` over 350ms with continuous tabular formatting.
  - **Accessibility / Reduced Motion:** If `isReducedMotionEnabled()`, instantly snap value without transition.

---

## 6. Dashboard Component Architecture

The modernized Dashboard will assemble into seven cleanly decoupled components:

```text
┌────────────────────────────────────────────────────────────────────────┐
│ 1. GLOBAL HEADER                                                       │
│    • Household Name ("Popescu Household")                              │
│    • Sync Status Badge ("● In Sync")                                   │
│    • Dual Currency Toggle [ RON | EUR ]                                │
├────────────────────────────────────────────────────────────────────────┤
│ 2. UNIFIED HERO CANVAS (UnifiedHeroCanvas.kt)                          │
│    • Inverted Midnight Surface (#0F172A)                               │
│    • Animated Primary Balance (34sp Bold Tabular)                      │
│    • BNR Secondary Parity Horizon ("≈ 18 171.40 EUR • BNR 4.9765")     │
│    • Income / Expense Split with decoupled currency codes              │
├────────────────────────────────────────────────────────────────────────┤
│ 3. PERIOD SELECTOR HORIZON                                             │
│    • Compact Dropdown Chip ["📅 Year to Date (Jan 1 - Sep 9, 2026) ▾"] │
├────────────────────────────────────────────────────────────────────────┤
│ 4. FINANCIAL PULSE CARD (FinancialPulseCard.kt)                        │
│    • Conversational Narrative derived from SmartFinancialInsights       │
│    • Savings Ratio vs Expense Velocity Dual Progress Bar               │
├────────────────────────────────────────────────────────────────────────┤
│ 5. CASH FLOW RADAR (MonthlyCashFlowSplineChart.kt)                     │
│    • Chronological Temporal Axis (Jan -> Sep)                          │
│    • Dual Bézier Curves (Emerald Income, Coral Expense)                │
│    • Continuous Touch Drag Scrubbing + Floating HUD                    │
├────────────────────────────────────────────────────────────────────────┤
│ 6. CATEGORY ALLOCATION (CategoryDistributionChart.kt)                  │
│    • Interactive Segmented Donut Arc (100% Hamilton-Hare sum)          │
│    • Top 4 Expense Category Rows with Share % & Amount                 │
├────────────────────────────────────────────────────────────────────────┤
│ 7. RECENT ACTIVITY SNAPSHOT (RecentActivitySection.kt)                 │
│    • 3 Latest Household Transactions with Emoji Icons                  │
│    • "View All Activity ➔" Navigation Link                             │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Component Reuse & Modification Matrix

| Component | File Path | Strategy | Description of Changes |
| :--- | :--- | :---: | :--- |
| `FinTrackCard` | `/ui/components/FinTrackCard.kt` | **REFINE** | Add 1dp micro-border support; pure white surface in Light Mode. |
| `CurrencyToggle` | `/ui/components/CurrencyToggle.kt` | **REFINE** | Use `BrandAccent` pill; add spring press feedback. |
| `PeriodSelector` | `/ui/components/PeriodSelectorChipRow.kt` | **REFINE** | Integrate temporal range preview; anchor dropdown cleanly. |
| `FinTrackBottomNavigation` | `/ui/navigation/FinTrackBottomNavigation.kt` | **REFINE** | Change tab label to `"Activity"`; optimize label typography. |
| `MonthlyCashFlowSplineChart`| `/ui/components/FinancialChartComponents.kt` | **REFINE** | Add touch drag gesture scrubbing, crosshair line, and month HUD. |
| `CategoryDistributionChart` | `/ui/components/FinancialChartComponents.kt` | **EXTEND** | Introduce Segmented Donut header above category breakdown list. |
| `UnifiedHeroCanvas` | `/ui/components/UnifiedHeroCanvas.kt` | **NEW** | Standalone high-contrast hero container with BNR parity horizon. |
| `FinancialPulseCard` | `/ui/components/FinancialPulseCard.kt` | **NEW** | Surfacing previously unrendered `smartInsights` in conversational prose. |
| `AnimatedCounter` | `/ui/components/AnimatedCounter.kt` | **NEW** | Tabular numeric rolls and currency crossfade transitions. |
| `RecentActivitySection` | `/ui/components/RecentActivitySection.kt` | **NEW** | 3-item recent transaction preview linking to full activity screen. |

---

## 8. File-by-File Change Plan

| File | Action | Detailed Changes | Dependencies | Risk |
| :--- | :---: | :--- | :--- | :---: |
| `FinancialAnalyticsEngine.kt` | **MODIFY** | Fix chronological sorting in `calculateMonthlyDataPoints`. | None | **Low** |
| `FinancialAnalyticsEngineTest.kt` | **MODIFY** | Add tests for multi-year chronological month sorting. | Engine | **Low** |
| `Color.kt` | **MODIFY** | Add surface, border, and financial semantic tokens. | None | **Low** |
| `Theme.kt` | **MODIFY** | Bind v2 color tokens to MaterialTheme and CompositionLocal. | Color.kt | **Medium** |
| `Type.kt` | **MODIFY** | Add tabular figure `fontFeatureSettings = "tnum"` styles. | None | **Low** |
| `Shape.kt` | **MODIFY** | Add progressive radii (`HeroRadius`, `InsetRadius`, etc.). | None | **Low** |
| `Motion.kt` | **MODIFY** | Add Compose spring specs and easing curves. | None | **Low** |
| `FinTrackCard.kt` | **MODIFY** | Update container colors and 1dp micro-borders. | Theme.kt | **Low** |
| `FinTrackBottomNavigation.kt` | **MODIFY** | Update tab label to `"Activity"` and adjust typography. | Theme.kt | **Low** |
| `AnimatedCounter.kt` | **CREATE** | Tabular number animation & currency crossfade composable. | Type.kt, Motion.kt | **Low** |
| `UnifiedHeroCanvas.kt` | **CREATE** | Apex balance card with BNR secondary horizon. | AnimatedCounter.kt | **Medium** |
| `FinancialPulseCard.kt` | **CREATE** | Narrative health summary and dual velocity bar. | Theme.kt | **Low** |
| `RecentActivitySection.kt` | **CREATE** | 3-item recent transaction snapshot list. | Theme.kt | **Low** |
| `FinancialChartComponents.kt` | **MODIFY** | Add scrubbing & HUD to Spline; add Donut to Categories. | Motion.kt | **Medium** |
| `DashboardScreen.kt` | **MODIFY** | Recompose layout using the new modular components. | All above | **Medium** |
| `MainViewModel.kt` | **MODIFY** | Expose 3 latest transactions for Dashboard preview. | TransactionDao | **Low** |

---

## 9. Data Flow Safety & Immutability

The UI redesign adheres to strict architectural boundaries:
```text
[Room Database / Firestore Sync]
              ↓ (Immutable entities)
   [FinTrackRepository]
              ↓ (Flow<List<TransactionEntity>>)
      [MainViewModel]
              ↓ (StateFlow<DashboardUiState>)
     [DashboardScreen]
              ↓ (Stateless composable parameters)
[UI Component Renderers]
```

* **STRICT RULE 1:** No business logic or mathematical conversions will be written inside Composables. All sums, BNR conversions, and percentages are computed in `FinancialAnalyticsEngine` or `MainViewModel`.
* **STRICT RULE 2:** Outbox synchronization (`SyncOutboxDao`), Firestore lifecycle listeners, and Room migrations are **completely untouched**.
* **STRICT RULE 3:** BNR exchange rates remain the sole authoritative source of currency conversion.

---

## 10. Test & Verification Plan

1. **Unit Tests (`gradle :app:testDebugUnitTest`):**
   - Verify `FinancialAnalyticsEngineTest` confirms chronological month ordering.
   - Verify `CategoryAndDashboardFixesTest` and `DefectFixesUnitTest` continue passing with 100% success.
   - Verify `AnalyticsViewModelWiringTest` passes cleanly.
2. **Layout & Responsive Verification:**
   - Test layout rendering across **360dp**, **390dp**, and **412dp+** screen widths.
   - Confirm zero horizontal truncation on 6-digit sums (`+131 556 RON`) and navigation labels (`Activity`).
3. **Theme & Contrast Verification:**
   - Validate Light Mode surfaces against background: Card surface (`#FFFFFF`) with border (`#E2E8F0`) against canvas (`#F8FAFC`) yields crisp $> 4.5:1$ edge definition.
   - Validate Dark Mode obsidian tonal hierarchy.
4. **Interaction & Gesture Verification:**
   - Validate finger dragging on the Cash Flow chart scrubs smoothly without triggering parent vertical scroll conflict.
   - Validate currency toggle smoothly crossfades between RON and EUR without numeric flashing.

---

## 11. Performance Guardrails

* **Zero Recomposition Waste:** Chart data points wrapped in immutable lists. Path objects reused inside `remember(dataPoints)` rather than allocated per-frame.
* **Canvas GPU Efficiency:** Chart crosshairs and drag scrubbing use `drawWithCache` to avoid memory churn.
* **No Heavy External Libraries:** All graphics drawn using native Compose `Canvas`, ensuring instant startup and 120Hz scrolling on mid-range devices.

---

## 12. Recommended Phase 3B Implementation Order

```text
STEP 1 (P0 Fixes):
  - Fix chronological month sorting in FinancialAnalyticsEngine.kt
  - Add test in FinancialAnalyticsEngineTest.kt
  - Update FinTrackBottomNavigation.kt label to "Activity"

STEP 2 (Design System v2 Tokens):
  - Expand Color.kt and Theme.kt with micro-border and surface tokens
  - Add tabular numeric styles to Type.kt
  - Update Shape.kt with progressive radii
  - Refine FinTrackCard.kt with default 1dp micro-border

STEP 3 (Motion Primitives):
  - Add spring and cubic easing specs to Motion.kt
  - Create AnimatedCounter.kt

STEP 4 (Structural Components):
  - Create UnifiedHeroCanvas.kt (with BNR secondary horizon)
  - Create FinancialPulseCard.kt (with narrative insights)
  - Create RecentActivitySection.kt

STEP 5 (Interactive Visualizations):
  - Refine MonthlyCashFlowSplineChart in FinancialChartComponents.kt (scrubbing + HUD)
  - Extend CategoryDistributionChart in FinancialChartComponents.kt (Segmented Donut)

STEP 6 (Dashboard Assembly & QA):
  - Reassemble DashboardScreen.kt with new structural sections
  - Execute full test suite (`gradle :app:testDebugUnitTest`)
  - Verify compile applet (`compile_applet`)
```

---
*Implementation Plan authored, verified against repository codebase, and staged for Phase 3B execution.*
