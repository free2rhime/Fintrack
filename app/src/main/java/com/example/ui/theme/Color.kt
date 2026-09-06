package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// FinTrack Design System v1 — Foundation Tokens
// ==========================================

// ==========================================
// DARK-SPECIFIC FOUNDATION TOKENS
// CAUTION: The following tokens are hardcoded for Dark Theme only.
// In Phase 2+, UI components should consume MaterialTheme.colorScheme
// instead of referencing these tokens directly to prevent Light Theme contamination.
// ==========================================

// Canvas & Surfaces (Dark Theme Tonal Elevation)
val CanvasDark = Color(0xFF0F172A)
val SurfaceDark = Color(0xFF1E293B)
val SurfaceContainerDark = Color(0xFF27354A)
val SurfaceContainerHighDark = Color(0xFF334155)

// Financial Semantics
val IncomeEmerald = Color(0xFF22C55E)
val IncomeContainer = Color(0x2622C55E)
val ExpenseCoral = Color(0xFFF43F5E)
val ExpenseContainer = Color(0x26F43F5E)

// Interactive & System Semantics
val CobaltBlue = Color(0xFF3B82F6)
val WarningAmber = Color(0xFFF59E0B)

// Typography & Content Semantics (Dark Theme Specific)
// CAUTION: TextPrimary (#F8FAFC) and TextSecondary (#94A3B8) are near-white / light-gray
// tokens intended exclusively for dark surfaces. Using them directly on light surfaces
// causes zero or inadequate contrast. Use MaterialTheme.colorScheme.onSurface instead.
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// ==========================================
// Legacy Compatibility Aliases
// (Guarantees zero regressions in existing screens & tests)
// ==========================================
val PrimaryGreen = IncomeEmerald
val PrimaryGreenDark = Color(0xFF16A34A)
val PrimaryGreenContainer = Color(0xFFDCFCE7)
val PrimaryGreenLight = Color(0xFF86EFAC)

val SecondaryBlue = CobaltBlue
val SecondaryBlueDark = Color(0xFF2563EB)

val TertiaryViolet = Color(0xFF8B5CF6)

val IncomeGreen = IncomeEmerald
val ExpenseRed = ExpenseCoral

// ==========================================
// Neutral Palette — Clean Minimalism Light (M3 Foundation)
// ==========================================
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF8FAFC)
val SurfaceContainerLight = Color(0xFFF1F5F9)
val SurfaceContainerHighLight = Color(0xFFE2E8F0)
val SurfaceContainerHighestLight = Color(0xFFCBD5E1)
val SurfaceVariantLight = Color(0xFFF1F5F9)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val OutlineLight = Color(0xFFCBD5E1)
val OutlineVariantLight = Color(0xFFE2E8F0)

// Neutral Palette — Dark Aliases
val BackgroundDark = CanvasDark
val SurfaceVariantDark = SurfaceContainerHighDark
val TextPrimaryDark = TextPrimary
val TextSecondaryDark = TextSecondary

// Status Rules Colors
val StatusGreen = IncomeEmerald
val StatusOrange = WarningAmber
val StatusRed = ExpenseCoral


