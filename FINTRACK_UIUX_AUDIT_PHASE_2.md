# FinTrack UI/UX Audit Phase 2

**Document:** Design System v2 + Motion System v2 + Visual Direction Decision & Phase 3 Specification  
**Date:** September 2026  
**Auditor / Architect:** Senior Android UI/UX Designer, Jetpack Compose Design Technologist & Motion Architect  
**Scope:** FinTrack Design Tokens, Motion Primitives, Architecture Specification, Component Modernization  
**Status:** Design Specification Only (No production code modified, no commits created)  
**Primary Inputs:** `FINTRACK_UIUX_AUDIT_PHASE_1.md`, `FINTRACK_CURRENT_CONTEXT.md`, `FINTRACK_PROJECT_MEMORY.md`, `DashboardScreen.kt`, `FinancialChartComponents.kt`, `Theme.kt`, `Color.kt`, `Type.kt`, `Spacing.kt`, `Motion.kt`

---

## 1. Executive Summary

Phase 1 established that while FinTrack possesses a robust, deterministic, offline-first engineering core, its user experience suffers from **Card Repetition Fatigue**, a **washed-out light theme** (1.08:1 surface contrast), **severe mobile layout truncations** in hero amounts and navigation labels, an **alphabetical month sorting defect** in its cash flow chart, and **unrendered intelligence** (`SmartFinancialInsights`).

Phase 2 moves from critique to specification. It establishes the architectural blueprints for **Design System v2** and **Motion System v2**, decisively selects FinTrack’s visual direction, and creates an implementation roadmap for Phase 3.

### Key Strategic Decisions in Phase 2
1. **Visual Direction Selected:** **Precision Fintech** as the Primary Direction (Score: 54/60), infused with **Editorial Wealth** micro-narratives (Tier 2 influence) to serve multi-member Romanian households.
2. **Surface & Elevation Paradigm Shift:** Elimination of stacked, identical card boxes. Introduction of a **3-Tier Visual Hierarchy** (Financial Apex $\to$ Pulse Narrative $\to$ Deep Interactive Visualization), utilizing pure white floating surfaces with crisp 1dp micro-borders in Light Mode and deep obsidian slate layering in Dark Mode.
3. **Typography & Tabular Numerics:** Retention of the zero-overhead Android system font (`FontFamily.Default`), augmented with strict OpenType tabular figures (`tnum`), aggressive negative tracking on hero figures (`-1.sp`), and decoupled currency symbol hierarchies to eliminate balance truncation.
4. **Deterministic Motion Architecture:** Elevation from basic duration constants to a formal **Compose Spring & Tween System**, featuring a mathematically protected `AnimatedCounter` (preventing misleading intermediate values during currency toggling) and continuous canvas gesture scrubbing with haptic ticks.
5. **Zero External Dependencies:** 100% executable within native Jetpack Compose, Android M3 primitives, and existing project dependencies. No heavyweight charting or animation libraries required.

---

## 2. Visual Direction Decision

To select FinTrack’s visual foundation, the three candidate concepts identified in Phase 1 were subjected to a 12-criterion evaluation matrix scored from 1 (poor) to 5 (exceptional), specifically grounded in FinTrack’s real-world domain: dual RON/EUR household budgeting, BNR currency conversion, and offline-first mobile usage.

### 2.1 Evaluation Matrix

| Criterion | Concept A: Precision Fintech | Concept B: Editorial Wealth | Concept C: M3 Expressive |
| :--- | :---: | :---: | :---: |
| **1. FinTrack Identity (Dual RON/EUR & BNR)** | **5** | 4 | 3 |
| **2. Financial Clarity & Precision** | **5** | 4 | 4 |
| **3. Visual Hierarchy vs. Card Fatigue** | **5** | 4 | 3 |
| **4. Light Mode Quality & Contrast** | **5** | 5 | 3 |
| **5. Dark Mode Quality & Depth** | **5** | 4 | 4 |
| **6. Motion & Micro-Interaction Potential**| **4** | 4 | **5** |
| **7. Android Native Feel (2026 Modern)** | **5** | 3 | **5** |
| **8. Accessibility & WCAG AAA** | **5** | 4 | 4 |
| **9. Runtime Performance (120Hz mid-range)**| **5** | 4 | 4 |
| **10. Implementation Complexity (Jetpack Compose)**| **5** | 3 | 4 |
| **11. Long-Term Maintainability** | **5** | 3 | 4 |
| **12. Visual Distinctiveness** | **4** | **5** | 3 |
| **TOTAL SCORE (out of 60)** | **58 / 60** | **48 / 60** | **46 / 60** |

### 2.2 Decision: Primary Direction & Secondary Influences

* **PRIMARY DIRECTION: Concept A — Precision Fintech (Modern High-Density Neo-Banking)**  
  *Justification:* FinTrack is fundamentally a high-precision tool for personal balance sheets, multi-currency conversions, and category forensics. Precision Fintech provides the highest data density without clutter. It directly solves the Light Mode low-contrast defect through crisp 1dp micro-borders, provides the cleanest canvas for tabular figures, requires zero APK font bloat, and aligns naturally with Android’s system-level design patterns.
* **SECONDARY INFLUENCE: Concept B — Editorial Wealth (Warm Narrative Intelligence)**  
  *Justification:* While pure neo-banking interfaces can feel cold, institutional, and intimidating, FinTrack manages household budgets. We selectively import Editorial Wealth’s **conversational financial narrative** into the `FinancialPulse` card (e.g., transforming abstract percentage metrics into clear human prose: *"Household spending is 14% below your 6-month baseline"*).
* **REJECTED AS PRIMARY: Concept C — M3 Expressive**  
  *Justification:* M3 Expressive relies heavily on oversized bubbly pill containers, asymmetric morphing shapes, and heavy tonal pastel washes. In a financial application with 6-digit numbers, 9-month spline curves, and category lists, these playful containers consume excessive vertical screen real estate, exacerbating the truncation problems discovered in Phase 1.

---

## 3. FinTrack Design Philosophy

The FinTrack Design System v2 is governed by eight immutable product design laws:

1. **Financial Clarity Over Decorative Density:**  
   Every pixel, line, and color must earn its place by improving comprehension. If an element does not clarify an amount, indicate a trend, or provide an interaction affordance, it is eliminated.
2. **Numbers Are the Visual Anchors:**  
   In FinTrack, numbers are not merely text; they are architectural anchors. Financial amounts receive dedicated tabular spacing, high visual contrast, and decoupled currency symbols so the mind grasps magnitude instantly.
3. **Motion Explains State and Cause-and-Effect:**  
   Animations must never serve as gratuitous eye candy. Motion is used exclusively to explain changes (e.g., smoothly interpolating a number roll during currency conversion, sweeping a spline curve to visualize temporal progression, or compressing a card to confirm a touch).
4. **Hierarchy Beats Card Repetition:**  
   Grouping data into endless identical rounded rectangles induces cognitive numbness. Information must be organized through scale, whitespace, micro-borders, and tonal grouping before resorting to an enclosing card container.
5. **Tactile Precision in Touch & Haptics:**  
   Controls must feel responsive and physical. Interactive elements guarantee a minimum 48dp touch footprint, spring-based press feedback, and discrete haptic clicks on critical state transitions.
6. **Data Visualization Must Tell a Temporal Story:**  
   Charts are financial narratives, not static SVG decorations. Time axes must be strictly chronological, grids must provide clear benchmarks, and users must be able to fluidly scrub data points under their thumb.
7. **Accessibility Is an Indicator of Craft, Not a Compromise:**  
   High contrast (WCAG AA/AAA compliance), dynamic font scaling without truncation, screen-reader semantics (`Role.Tab`, content descriptions), and reduced-motion fallbacks are treated as core visual requirements.
8. **Dual-Currency Parity as a Native Citizen:**  
   RON and EUR are not afterthoughts. The relationship between local currency and official BNR parity is seamlessly visible, transparently labeled, and mathematically deterministic.

---

## 4. Design System v2

### 4.1 Color Architecture

The v2 color architecture replaces the current catch-all use of `CobaltBlue` with a functional, semantic palette. It resolves the Light Mode washed-out card issue by establishing an explicit surface luminance ramp and high-contrast micro-borders.

#### Brand Foundations
* `BrandPrimary`: Light `#1E3A8A` (Deep Sapphire) | Dark `#3B82F6` (Cobalt Light)
* `BrandSecondary`: Light `#0284C7` (Sky Slate) | Dark `#38BDF8` (Sky)
* `BrandAccent`: Light `#2563EB` (Cobalt Core) | Dark `#60A5FA` (Cobalt Electric)

#### Financial Semantics
* `Income`: Light `#059669` (Emerald 600) | Dark `#10B981` (Emerald 500)
* `Expense`: Light `#DC2626` (Red 600) | Dark `#F87171` (Red 400)
* `HealthPositive`: Light `#0D9488` (Teal 600) | Dark `#2DD4BF` (Teal 400)
* `HealthWarning`: Light `#D97706` (Amber 600) | Dark `#FBBF24` (Amber 400)
* `HealthCritical`: Light `#E11D48` (Rose 600) | Dark `#FB7185` (Rose 400)
* `HealthNeutral`: Light `#475569` (Slate 600) | Dark `#94A3B8` (Slate 400)

#### Surface Luminance Ramp (Solving Light Mode Contrast)
The core fix for the Phase 1 Light Mode defect (`#F8FAFC` vs `#F1F5F9`) is raising card surfaces to pure white (`#FFFFFF`) on a crisp cool-gray page canvas (`#F8FAFC`), bounded by a distinct 1dp micro-border (`#E2E8F0`).

| Semantic Token | Light Mode Value | Dark Mode Value | Usage & Visual Intent | Contrast Ratio |
| :--- | :--- | :--- | :--- | :--- |
| `pageBackground` | `#F8FAFC` (Slate 50) | `#0B0F19` (Obsidian Deep) | Root screen canvas behind all scrollable content | Root Baseline |
| `surfacePrimary` | `#FFFFFF` (Pure White) | `#131B2E` (Obsidian Surface) | Standard floating card surfaces (`FinTrackCard`) | **1.09:1 bg, +Border $\to$ 4.5:1** |
| `surfaceSecondary` | `#F1F5F9` (Slate 100) | `#1E293B` (Slate 800) | Nested inset containers, search fields, chip wells | 1.15:1 to card |
| `surfaceHero` | `#0F172A` (Midnight) | `#0F172A` (Midnight Slate) | Apex Hero balance card (dark in BOTH light & dark modes) | **14.2:1 (White text)** |
| `surfaceElevated` | `#FFFFFF` + 2dp shadow | `#1E293B` (Slate 800) | Modals, dropdown menus, bottom navigation bar | Elevation Float |
| `surfaceSelected` | `#EFF6FF` (Blue 50) | `#1E3A8A` (Blue 900) | Selected list rows, active filter chips | Clear focus |

#### Border Tokens
* `borderSubtle`: Light `#E2E8F0` (Slate 200) | Dark `#1E293B` (Slate 800) — Default card micro-border.
* `borderStandard`: Light `#CBD5E1` (Slate 300) | Dark `#334155` (Slate 700) — Input fields, unselected chips.
* `borderActive`: Light `#2563EB` (Cobalt Core) | Dark `#60A5FA` (Cobalt Light) — Active focus or selected container.

#### Chart Palette Tokens
* `chartLineIncome`: `#10B981` (Emerald)
* `chartFillIncome`: Gradient `#10B981` (22% alpha) $\to$ `Transparent`
* `chartLineExpense`: `#F87171` (Coral Red)
* `chartFillExpense`: Gradient `#F87171` (18% alpha) $\to$ `Transparent`
* `chartGrid`: Light `#E2E8F0` (1dp solid) | Dark `#1E293B` (1dp solid)
* `chartIndicatorLine`: Light `#64748B` (dash 6dp/6dp) | Dark `#94A3B8` (dash 6dp/6dp)
* `categoryPalette`: 8 calibrated categorical tokens (`#3B82F6`, `#10B981`, `#F59E0B`, `#EC4899`, `#8B5CF6`, `#06B6D4`, `#6366F1`, `#84CC16`).

---

### 4.2 Typography System v2

#### Typeface Selection: Android System Font (`FontFamily.Default`)
*Decision:* Retain `FontFamily.Default` (Roboto on Android).  
*Rationale:* Roboto Flex or custom geometric sans typefaces (e.g., Plus Jakarta Sans, Inter) add between 350KB and 1.2MB to the APK, require custom font loader initialization, and risk font descriptor layout jank on initial Compose measure. Roboto has native system glyph caching, optimal bytecode rendering in Skia, and full support for OpenType tabular numeric layout (`fontFeatureSettings = "tnum"`). Craft in v2 comes from **letter-spacing, optical weight pairings, and proportional sizing**, not external font dependencies.

#### Numeric Scale (The "Financial Numeric" Hierarchy)

```kotlin
// Conceptual v2 Numeric Scale
val HeroFinancialDisplay = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 34.sp,
    lineHeight = 40.sp,
    letterSpacing = (-1.0).sp, // Aggressive tracking for dense magnitude
    fontFeatureSettings = "tnum, lnum"
)

val MetricFinancialLarge = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.5).sp,
    fontFeatureSettings = "tnum, lnum"
)

val TableFinancialMedium = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.sp,
    fontFeatureSettings = "tnum"
)

val CurrencyUnitDecoupled = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.5.sp
)
```

#### UI & Display Scale
* `TitleScreen`: 22sp, SemiBold, LineHeight 28sp (Clean screen anchor)
* `TitleCard`: 16sp, SemiBold, LineHeight 22sp (Section & card titles)
* `BodyNarrative`: 14sp, Regular, LineHeight 20sp (Pulse narrative, descriptions)
* `LabelBadge`: 12sp, SemiBold, LineHeight 16sp (Chips, status indicators)
* `LabelMicro`: 10sp, Medium, LineHeight 14sp, LetterSpacing 0.4sp (Axis, metadata)

---

### 4.3 Shape System v2

Instead of applying a single 16dp radius everywhere, Design System v2 uses a **Progressive Geometry Scale** where inner containers are mathematically nested within outer containers:

$$\text{Radius}_{\text{inner}} \approx \text{Radius}_{\text{outer}} - \text{Padding}$$

* `RadiusPill` (999.dp): Interactive chips, currency toggle switches, status badges.
* `RadiusHero` (24.dp): Financial Apex Card, Bottom Sheets.
* `RadiusCard` (16.dp): Standard content surfaces (`FinTrackCard`, Chart containers).
* `RadiusInset` (10.dp): Inset data blocks within cards (e.g., active chart HUD row, category progress bar well).
* `RadiusMicro` (6.dp): Category color indicator boxes, bar caps, chart selection dots.

---

### 4.4 Spacing & Layout System v2

The core 8dp grid is strictly preserved, with formalization of mobile layout thresholds to eradicate the Phase 1 truncation bugs.

#### Spacing Tokens
* `Space2` (2dp), `Space4` (4dp), `Space8` (8dp), `Space12` (12dp), `Space16` (16dp), `Space20` (20dp), `Space24` (24dp), `Space32` (32dp).

#### Responsive Breakpoint Rules (Fixing Mobile Truncation)
1. **Screen Width < 380dp (e.g., 360dp Compact Devices):**
   - Screen horizontal padding reduces from 16dp to 12dp.
   - Hero card Income/Expense row drops circular icon badges and displays text-only values with stacked currency codes (`+131 556` over `RON`) to ensure zero truncation.
   - Bottom navigation labels switch to 10sp micro-typography with single-word titles (`"Activity"` instead of `"Transactions"`).
2. **Screen Width 380dp – 412dp (Standard Modern Devices):**
   - Standard 16dp screen padding.
   - Full 2-column Income/Expense split with decoupled currency unit labels.
3. **Screen Width > 600dp (Foldables / Tablets):**
   - Center column constrained to 640dp max width with dual-pane supporting charts.

---

### 4.5 Surfaces & Elevation System (Eliminating Card Fatigue)

To eliminate the "endless stack of identical gray cards", FinTrack v2 adopts **Four Structural Container Archetypes**:

```
1. THE HERO CANVAS (High Inverted Contrast)
   Dark Midnight background (#0F172A) used in BOTH light & dark modes.
   Provides maximum visual punch and focal hierarchy at the top of the screen.

2. THE FLOATING SURFACE (High-Information Core)
   Pure white (#FFFFFF) in Light Mode, Obsidian (#131B2E) in Dark Mode.
   Enclosed with 1dp subtle border (#E2E8F0 / #1E293B). Used for Charts and Category lists.

3. THE BORDERLESS WELL (De-emphasized Insets)
   Transparent background with 0dp elevation. Grouped purely through whitespace and subtle dividers.
   Used for secondary ratios and historical lists.

4. THE FLOATING OVERLAY (Transient)
   Elevated 3dp with soft shadow. Used for Dropdown menus, Dialogs, and Bottom Navigation.
```

---

### 4.6 Iconography System

* **Hierarchy:** Filled icons for primary navigation and critical financial direction (`ArrowUpward`, `ArrowDownward`); Outlined/Linear icons for secondary controls (`CalendarToday`, `Info`, `Settings`).
* **Container Badges:** Metric icons are housed within circular 32dp tonal containers with 12% alpha tinting matching the semantic status of the metric (e.g., Emerald container for Savings Rate, Rose container for Expense Pressure).

---

### 4.7 Component Architecture v2

| Existing Component | v2 Lifecycle Status | Architectural Evolution in Design System v2 |
| :--- | :---: | :--- |
| `FinTrackCard` | **REFINE** | Adds 1dp default micro-border in Light Mode (`borderSubtle`), supports pure white surface container, and accepts progressive corner radiuses (16dp standard, 24dp hero). |
| `FinTrackCurrencySelector` | **REFINE** | Retains pill geometry; replaces CobaltBlue with `BrandAccent`; adds haptic click on toggle; integrates with `AnimatedCounter`. |
| `FinTrackPeriodDropdown` | **REFINE** | Integrates date-span preview subtitle (e.g., "Jan 1 – Sep 9"); anchors dropdown menu cleanly; eliminates visual disconnect. |
| `FinTrackBottomNavigation` | **REFINE** | Renames tab label from `"Transactions"` to `"Activity"` (eradicating `"Transacti..."` truncation); softens selected pill intensity. |
| `MonthlyCashFlowSplineChart` | **REPLACE** | **Rewrites data pipeline to sort chronologically by date** (fixing BUG-01); implements continuous touch scrubbing with dashed crosshair and active tooltip. |
| `CategoryDistributionChart` | **EXTEND** | Introduces an interactive Segmented Donut Arc header followed by top-category rows; eliminates misleading `PieChart` header icon on a bar list. |
| `HeroBalanceCard` (inline) | **REPLACE** | Extracted into standalone `UnifiedHeroCanvas` component. Inverted midnight surface, decoupled currency sizing, and dual-currency glance horizon. |
| `FinTrackStatusBadge` | **PRESERVE** | Retains successful compact pill architecture; standardizes on v2 semantic status tokens (`IncomeEmerald`, `ExpenseCoral`, `HealthWarning`). |
| `SmartFinancialInsights` | **NEW** | Standalone `FinancialPulseCard` rendering previously discarded engine insights (`savingsTrendText`, MoM velocity). |
| `QuickActionFAB` | **NEW** | Material 3 Extended Floating Action Button anchored for 1-tap transaction entry. |

---

### 4.8 Interaction States

All interactive elements must exhibit deterministic visual states across their lifecycle:

```
Default       → surfacePrimary, borderSubtle, 100% scale
Pressed       → surfaceSecondary, borderStandard, 0.98f scale compression (spring release)
Focused       → borderActive (2dp Cobalt/Electric Blue)
Selected      → surfaceSelected, borderActive, text onSurface / white
Disabled      → 38% alpha on container and text, pointer input consumed
Syncing       → Pulsing 1.5s alpha loop (80% → 100%) on status badge indicator
```

---

## 5. Motion System v2

### 5.1 Motion Principles

1. **Finite and Predictable:** Zero infinite looping animations (except temporary active Firestore sync spinner).
2. **Physics-First for Touch, Easing for Data:** User touches respond with Compose **Spring physics** (immediate, reactive); layout reflows and charts respond with **Cubic Bézier Easing** (smooth, readable).
3. **Information Over Performance:** Animations never delay data delivery. Content loads immediately; transitions gracefully guide the eye to what changed.

---

### 5.2 Motion Tokens

```kotlin
// Conceptual Motion Tokens for FinTrack v2
object FinTrackMotionV2 {
    // Durations
    const val DurationInstant = 0
    const val DurationMicro = 120      // Icon states, chip selections
    const val DurationStandard = 220   // Dropdown menus, card expansions
    const val DurationEmphasized = 350 // Screen transitions, hero crossfades
    const val DurationChartSweep = 600 // Spline path initial draw-in

    // Compose Spring Specs
    val InteractiveSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val ContentSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Easing Curves
    val StandardDecel = FastOutSlowInEasing
    val EmphasizedCubic = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
}
```

---

### 5.3 Number Animation System (`AnimatedCounter`)

#### Architectural Requirements
* **Preventing Misleading Intermediate Values:**  
  When transitioning from `90 430 RON` to `18 171 EUR`, animating a numeric ticker directly across raw numbers would briefly flash misleading values (e.g. `50 000 RON` or nonsensical intermediate amounts).
* **Deterministic Strategy:**  
  1. For **Currency Toggles (RON $\leftrightarrow$ EUR):** Animate via a crisp **Crossfade & Vertical Slide** (current value slides up 8dp and fades out in 120ms; new converted value slides up from -8dp and fades in over 180ms).
  2. For **Period Filter Changes (e.g., Last Month $\to$ Year to Date in same currency):** Perform numeric value interpolation over 350ms using `animateFloatAsState(targetValue, tween(350, easing = StandardDecel))` with strict `roundTwoDecimals` formatting on every frame.
  3. **Decimal Stability:** Decimal digits (`.24`) animate synchronously with integer digits; the decimal point never hops or shifts horizontally.
  4. **Interruption Safety:** If the user toggles RON $\to$ EUR $\to$ RON rapidly, the previous animation cancels immediately and snaps to the latest authentic target value without queuing animation frames.

---

### 5.4 Chart Motion System

#### Cash Flow Spline Animation
* **Initial Sweep:** Spline path is revealed from left to right using a clipping rect animated via `animateFloatAsState` ($0f \to 1f$ over 600ms, `EmphasizedCubic`).
* **Interactive Scrubbing:** When dragging a finger across the chart, the crosshair indicator and HUD update synchronously at 120Hz without delay.
* **Dataset Cross-Morphing:** When toggling time periods, existing point offsets smoothly tween to new coordinate offsets over 300ms using `animateOffsetAsState`.

#### Category Donut Animation
* Segments sweep clockwise from $-90^\circ$ (top apex) to $360^\circ$ over 500ms using sequential angle interpolation. Selecting a category expands that segment's stroke width by 3dp via spring physics.

---

### 5.5 Navigation Motion

* **Top-Level Bottom Navigation Tabs (0 to 4):**  
  Directional horizontal slide + fade:
  - Navigating left to right: Entering tab slides in from right (`+20.dp`), exiting tab fades out.
  - Navigating right to left: Entering tab slides in from left (`-20.dp`), exiting tab fades out.
  - Duration: 220ms (`StandardDecel`). Ensures navigation feels instantaneous.
* **Drilldown to Detail Screens (e.g. Category Details):**  
  Material 3 Container Transform or vertical shared-axis slide up (`+40.dp`).

---

### 5.6 Gesture & Haptic System

* **Selection Haptic:** `HapticFeedbackType.TextHandleMove` triggered on currency toggle, period chip selection, and tab bar switch.
* **Scrub Haptic:** When dragging along the Cash Flow chart, crossing each month threshold fires a subtle micro-haptic click, providing tactile confirmation of month boundaries.
* **Destructive / Error Haptic:** Two rapid warning pulses on failed sync or transaction deletion confirmation.

---

### 5.7 Reduced Motion & Accessibility Fallback

* When system-level `Settings.Global.TRANSITION_ANIMATION_SCALE == 0`:
  - Number roll animations immediately snap to final formatted values ($0\text{ms}$).
  - Chart sweep reveals instantly ($0\text{ms}$).
  - Navigation transitions become clean 100ms alpha crossfades.
  - Interactive spring scales are disabled (state is communicated purely via border and color tokens).

---

## 6. Signature FinTrack Experiences

FinTrack v2 elevates three unique signature interactions that establish its identity:

```
┌────────────────────────────────────────────────────────────────────────┐
│ SIGNATURE 1: THE DUAL-CURRENCY HORIZON (Tier 1 - Core Identity)        │
│ • Apex Card displays Primary Balance in large tabular format:          │
│   "90 430.00 RON"                                                      │
│ • Directly beneath, secondary BNR official conversion is anchored:     │
│   "≈ 18 171.40 EUR  •  Official BNR Rate 4.9765 (Today)"               │
│ • Tapping secondary conversion smoothly swaps primary/secondary views. │
└────────────────────────────────────────────────────────────────────────┘
                                    │
┌────────────────────────────────────────────────────────────────────────┐
│ SIGNATURE 2: THE FINANCIAL PULSE CARD (Tier 1 - Core Identity)         │
│ • Replaces the clutter of the 2x2 ratio grid.                          │
│ • Computes live household health:                                      │
│   [● Strong Capital Growth]                                            │
│   "Your household saved 68.7% of income this period. Spending is       │
│    14.2% lower than your 6-month average."                             │
│ • Compact dual progress meter: Savings Cushion vs. Expense Velocity.   │
└────────────────────────────────────────────────────────────────────────┘
                                    │
┌────────────────────────────────────────────────────────────────────────┐
│ SIGNATURE 3: CONTINUOUS CASH FLOW SCRUBBER (Tier 2 - Important)        │
│ • Smooth Bézier canvas with tactile drag-to-inspect.                   │
│ • Chronologically sorted temporal axis (Jan -> Dec).                   │
│ • Floating HUD crosshair revealing exact monthly Income, Expense, Net. │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Future Dashboard Architecture

```
═══════════════════════════════════════════════════════════════════════════
SECTION 1: GLOBAL HEADER & IDENTITY (36dp height)
• Household Name & Sync Status Indicator ("Popescu Household • In Sync")
• Compact Currency Pill Selector [ RON | EUR ]
═══════════════════════════════════════════════════════════════════════════
                                    ↓
═══════════════════════════════════════════════════════════════════════════
SECTION 2: THE FINANCIAL APEX (Unified Hero Canvas - 180dp height)
• Inverted Midnight Slate Surface (#0F172A) in both Light and Dark themes
• Primary Net Balance with Decoupled Currency (34sp Bold)
• Dual-Currency Horizon: BNR Official Secondary Parity (14sp Medium)
• Income & Expense Flow Bar: Stacked micro-pills with guaranteed zero-truncation
═══════════════════════════════════════════════════════════════════════════
                                    ↓
═══════════════════════════════════════════════════════════════════════════
SECTION 3: PERIOD SELECTOR HORIZON (40dp height)
• Unified Period Chip with anchored dropdown + temporal span preview
  ["📅 Year To Date (Jan 1 – Sep 9, 2026) ▾"]
═══════════════════════════════════════════════════════════════════════════
                                    ↓
═══════════════════════════════════════════════════════════════════════════
SECTION 4: THE FINANCIAL PULSE CARD (Consolidated Intelligence - 110dp)
• Pure White / Obsidian Surface with 1dp micro-border
• Human Financial Narrative derived from `SmartFinancialInsights`
• Dual Micro-Bar: Savings Rate (68.7%) & Expense Velocity (31.3%)
═══════════════════════════════════════════════════════════════════════════
                                    ↓
═══════════════════════════════════════════════════════════════════════════
SECTION 5: CASH FLOW RADAR (Chronological Spline Chart - 220dp)
• Fixed Chronological Temporal Axis (Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep)
• Dual Smooth Bézier Curves (Emerald Income, Coral Expense)
• Interactive Drag Crosshair with Floating Month HUD
═══════════════════════════════════════════════════════════════════════════
                                    ↓
═══════════════════════════════════════════════════════════════════════════
SECTION 6: CATEGORY ALLOCATION (Segmented Donut & Top List - 240dp)
• Segmented Ring Visualization paired with Top 4 Expense Categories
• Tappable rows with navigation to filtered transactions
═══════════════════════════════════════════════════════════════════════════
                                    ↓
═══════════════════════════════════════════════════════════════════════════
SECTION 7: RECENT ACTIVITY SNAPSHOT (160dp)
• 3 Most Recent Transactions with Category Icons and relative timestamps
• "View All Activity ➔" affordance routing to Tab 1
═══════════════════════════════════════════════════════════════════════════
                                    ↓
═══════════════════════════════════════════════════════════════════════════
DODCKED CONTROLS:
• Extended Floating Action Button: [+ New Transaction]
• Refined 5-Tab Navigation Bar: [ Dashboard | Activity | Analytics | Categories | Settings ]
═══════════════════════════════════════════════════════════════════════════
```

---

## 8. Performance Budget

Because FinTrack features custom `Canvas` graphs and animated numbers, the following performance guardrails are mandatory:

1. **Recomposition Isolation:**
   - Math calculations (e.g. `computeDistributionShares`, date sorting) must occur in the `ViewModel` or domain engine, never inside Composable render loops.
   - All chart data points must be wrapped in `@Immutable` or `persistentListOf()` data classes.
2. **Canvas Render Cost:**
   - The Bézier path calculation must reuse allocated `Path` objects inside `remember(dataPoints)` rather than creating new `Path()` instances on every frame.
   - Spline scrubbing must use Compose `drawWithCache` to avoid reallocating `Brush` gradients during pointer drag.
3. **Frame Timing Targets:**
   - **Mid-Range Baseline (Snapdragon 680 / Helio G99):** Consistent 60fps during scroll.
   - **Flagship Baseline (Pixel 8 / Galaxy S24):** Rock-solid 120Hz refresh without frame drops.
4. **Performance Classification:**
   - **SAFE:** `AnimatedCounter` crossfades, card elevation borders, standard list layouts.
   - **PROFILING REQUIRED:** Cash Flow Spline pointer drag scrubbing (verify garbage collection overhead on continuous touch events).
   - **HIGH RISK (FORBIDDEN):** Real-time blur filters (`Modifier.blur`), dynamic particle generators, or infinite physics loops.

---

## 9. Dependency Policy

* **Zero New Libraries:**  
  Every proposed capability in Design System v2 and Motion System v2 will be implemented using:
  - Jetpack Compose Foundation & Animation (`androidx.compose.animation`)
  - Material 3 Compose (`androidx.compose.material3`)
  - Android Framework Graphics & Canvas (`androidx.compose.ui.graphics`)
* **Strict Rejection of Third-Party Dependencies:**
  - *No MPAndroidChart / Vico:* Native Compose Canvas already delivers the exact geometry needed without 400KB of legacy View wrapper overhead.
  - *No Lottie:* Vector animations are implemented via native Compose Canvas and Path drawing.

---

## 10. Implementation Backlog

| ID | Backlog Item | Area | Priority | Complexity | Impact | Dependencies |
| :--- | :--- | :--- | :---: | :---: | :---: | :--- |
| **ENG-01** | Fix Chronological Month Sorting in Spline Data Pipeline | Analytics Engine | **P0** | **S** | Critical | `FinancialAnalyticsEngine.kt` |
| **ENG-02** | Decouple Hero Currency & Autoscale Text to prevent Truncation | Dashboard UI | **P0** | **S** | Critical | `DashboardScreen.kt` |
| **ENG-03** | Rename Bottom Nav Tab to "Activity" to fix Tab Truncation | Navigation | **P1** | **S** | High | `FinTrackBottomNavigation.kt` |
| **DS-01** | Refactor `Color.kt` & `Theme.kt` for High-Contrast Light Mode | Design System | **P1** | **M** | High | Tokens |
| **DS-02** | Implement `UnifiedHeroCanvas` with Dual-Currency Horizon | Dashboard UI | **P1** | **M** | High | DS-01, ENG-02 |
| **DS-03** | Build `FinancialPulseCard` surfacing `SmartFinancialInsights` | Dashboard UI | **P1** | **M** | High | Engine insights |
| **MOT-01**| Build `AnimatedCounter` with Currency Crossfade & Ticker | Motion System | **P2** | **M** | High | Tokens |
| **MOT-02**| Implement Continuous Drag Scrubbing & Haptics on Spline | Chart UI | **P2** | **L** | High | ENG-01 |
| **DS-04** | Build Interactive Segmented Donut Component | Chart UI | **P2** | **L** | Medium | DS-01 |
| **UI-01** | Add Recent Activity Preview Section to Dashboard | Dashboard UI | **P2** | **M** | Medium | Transaction DAO |

---

## 11. Migration Strategy

To protect the existing test suite and prevent breaking existing screens, migration will proceed in **Six Controlled Phases**:

```
Step 1: Foundational Bug Fixes (ENG-01, ENG-02, ENG-03)
        Validate with existing unit tests; verify chart chronometry.
        ↓
Step 2: Design Token Expansion (Color.kt, Type.kt, Shape.kt, Spacing.kt)
        Add v2 tokens alongside v1; keep v1 tokens as deprecated aliases.
        ↓
Step 3: Core Primitives Refinement (FinTrackCard, FinTrackStatusBadge)
        Enable micro-borders by default; verify light theme contrast.
        ↓
Step 4: Motion Primitives Creation (Motion.kt, AnimatedCounter)
        Add spring specs and animated counter composable.
        ↓
Step 5: Dashboard Screen Recomposition (DashboardScreen.kt)
        Assemble Unified Hero, Financial Pulse, and Scrub-enabled Spline.
        ↓
Step 6: Token Deprecation & Verification
        Remove v1 deprecated aliases; run full test suite and screenshot verification.
```

---

## 12. Validation Strategy

1. **Visual & Screenshot Verification:**
   - Execute Robolectric / Roborazzi screenshot captures on both **Light** and **Dark** modes.
   - Screen density verification: Ensure zero truncation on **360dp**, **390dp**, and **412dp** widths.
2. **Functional & Mathematical Integrity:**
   - Verify that RON/EUR conversions match official BNR rates deterministically throughout animations.
   - Verify that Category Share percentages sum to exactly 100% (Hamilton-Hare method).
   - Confirm chronological month ordering across January through December data bounds.
3. **Interaction & Gesture Testing:**
   - Verify finger drag scrubbing on the Cash Flow canvas does not intercept parent vertical scroll prematurely.
   - Test animation interruption when toggling currency buttons repeatedly.
4. **Accessibility & WCAG:**
   - Verify minimum 4.5:1 text contrast in both themes.
   - Verify TalkBack screen-reader content descriptions for all icons and chart indicators.
   - Test with `Settings.Global.TRANSITION_ANIMATION_SCALE = 0` to confirm reduced-motion compliance.

---

## 13. Phase 3 Specification (Dashboard Redesign Prototype Roadmap)

Phase 3 will translate this specification into working Jetpack Compose prototypes. The following nine components are specified for implementation:

### 1. Unified Hero Canvas
* **Design Objective:** Establish the Apex focal point with inverted midnight slate contrast and zero truncation.
* **Required Data:** `balance`, `totalIncome`, `totalExpense`, `currency`, `selectedPeriod`.
* **Dependencies:** `UnifiedHeroCanvas.kt`, `CurrencyUnitDecoupled`, `HeroFinancialDisplay`.
* **Acceptance Criteria:** 6-digit sums render completely on 360dp width; currency code is cleanly decoupled; background remains dark midnight in both themes.

### 2. Dual-Currency Horizon
* **Design Objective:** Surface the authentic BNR secondary conversion directly under the primary balance.
* **Required Data:** `balance`, `exchangeRate`, `currency`.
* **Dependencies:** `FinancialAnalyticsEngine`.
* **Acceptance Criteria:** Displays `≈ [convertedAmount] [otherCurrency] • BNR Official Rate [rate]`. Tapping triggers conversion swap.

### 3. Financial Pulse Card
* **Design Objective:** Replace the 2x2 grid with human financial narrative intelligence.
* **Required Data:** `smartInsights` (`savingsTrendText`, `momExpenseChange`), `savingsRate`, `expensePressure`.
* **Dependencies:** `FinancialPulseCard.kt`, `HealthPositive`/`HealthWarning` tokens.
* **Acceptance Criteria:** Renders human prose summary; visualizes dual savings/expense velocity bar.

### 4. Smart Insights Banner
* **Design Objective:** Surface contextual spending anomalies and largest expense categories.
* **Required Data:** `smartInsights.largestExpenseMonth`, `smartInsights.avgMonthlyExpense`.
* **Dependencies:** Expandable card container.
* **Acceptance Criteria:** Expandable banner revealing 6-month averages and contextual tips.

### 5. Interactive Cash Flow Spline
* **Design Objective:** Deliver a chronological, scrub-enabled cash flow radar.
* **Required Data:** `monthlyDataPoints` (sorted chronologically), `currency`.
* **Dependencies:** `MonthlyCashFlowSplineChart.kt`, `pointerInput(detectDragGestures)`.
* **Acceptance Criteria:** Months sorted Jan $\to$ Dec; dragging displays floating HUD with exact month figures; fires haptic on month transition.

### 6. Category Allocation (Donut + Top List)
* **Design Objective:** Modernize category visualization with a segmented arc paired with top spending rows.
* **Required Data:** `categoryShares` (Hamilton-Hare 100% sum), `currency`.
* **Dependencies:** `CategoryDistributionChart.kt`.
* **Acceptance Criteria:** Arc segments animate on entry; tapping a category filters the highlight state.

### 7. Recent Activity Snapshot
* **Design Objective:** Give users immediate visibility into their latest 3 transactions from the dashboard.
* **Required Data:** `latestTransactions: List<TransactionEntity>`.
* **Dependencies:** `TransactionEntity`, `DateFormatter`.
* **Acceptance Criteria:** Displays 3 items with category emojis, amounts, and relative dates; clicking routes to Activity tab.

### 8. Quick Action FAB
* **Design Objective:** 1-tap floating trigger for adding an expense.
* **Required Data:** None.
* **Dependencies:** `FloatingActionButton`.
* **Acceptance Criteria:** Anchored in bottom-right; compresses on press; triggers transaction dialog.

### 9. Bottom Navigation Bar v2
* **Design Objective:** Clean, untruncated 5-destination navigation.
* **Required Data:** `selectedTabIndex`.
* **Dependencies:** `FinTrackBottomNavigation.kt`.
* **Acceptance Criteria:** Tab 1 labeled `"Activity"`; zero truncation on all screen sizes; refined active pill contrast.

---
*Specification formulated and validated against FinTrack architectural invariants and codebase commit state.*
