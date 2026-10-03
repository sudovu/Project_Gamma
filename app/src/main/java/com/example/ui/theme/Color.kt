package com.example.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * GAMMA Theme Presets available for user customization.
 */
enum class GammaThemePreset(
    val title: String,
    val subtitle: String,
    val primaryColor: Color,
    val primaryVariant: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color,
    val accentColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val surfaceElevatedColor: Color,
    val surfaceHighlightColor: Color,
    val surfaceGlassColor: Color,
    val textPrimaryColor: Color,
    val textSecondaryColor: Color,
    val textMutedColor: Color,
    val dividerColor: Color,
    val primaryGlowColor: Color,
    val secondaryGlowColor: Color,
    val auraTopColor: Color,
    val isDark: Boolean
) {
    COSMIC_CYAN(
        title = "Cosmic Cyan",
        subtitle = "Aurora Cyan & Electric Violet with obsidian depth",
        primaryColor = Color(0xFF00F5D4),
        primaryVariant = Color(0xFF00BFA5),
        secondaryColor = Color(0xFF9D4EDD),
        tertiaryColor = Color(0xFFFF007A),
        accentColor = Color(0xFF70E000),
        backgroundColor = Color(0xFF07090E),
        surfaceColor = Color(0xFF0E121D),
        surfaceElevatedColor = Color(0xFF161C2C),
        surfaceHighlightColor = Color(0xFF222B3F),
        surfaceGlassColor = Color(0xCC0E121D),
        textPrimaryColor = Color(0xFFF1F5F9),
        textSecondaryColor = Color(0xFF94A3B8),
        textMutedColor = Color(0xFF64748B),
        dividerColor = Color(0xFF1E293B),
        primaryGlowColor = Color(0x3300F5D4),
        secondaryGlowColor = Color(0x339D4EDD),
        auraTopColor = Color(0xFF18102E),
        isDark = true
    ),

    SOLFEGGIO_VIOLET(
        title = "Solfeggio Violet",
        subtitle = "Radiant Purple & Electric Magenta cyberpunk resonance",
        primaryColor = Color(0xFFA855F7),
        primaryVariant = Color(0xFF9333EA),
        secondaryColor = Color(0xFFEC4899),
        tertiaryColor = Color(0xFF38BDF8),
        accentColor = Color(0xFFF43F5E),
        backgroundColor = Color(0xFF090714),
        surfaceColor = Color(0xFF130E26),
        surfaceElevatedColor = Color(0xFF1E1638),
        surfaceHighlightColor = Color(0xFF2D2250),
        surfaceGlassColor = Color(0xCC130E26),
        textPrimaryColor = Color(0xFFF8FAFC),
        textSecondaryColor = Color(0xFFC084FC),
        textMutedColor = Color(0xFF7E57C2),
        dividerColor = Color(0xFF2A1D4E),
        primaryGlowColor = Color(0x33A855F7),
        secondaryGlowColor = Color(0x33EC4899),
        auraTopColor = Color(0xFF2A0F38),
        isDark = true
    ),

    SOLAR_AMBER(
        title = "Solar Amber",
        subtitle = "Warm molten gold, acoustic wood & amber harmonics",
        primaryColor = Color(0xFFF59E0B),
        primaryVariant = Color(0xFFD97706),
        secondaryColor = Color(0xFFF97316),
        tertiaryColor = Color(0xFFE11D48),
        accentColor = Color(0xFFFBBF24),
        backgroundColor = Color(0xFF0D0B0A),
        surfaceColor = Color(0xFF171311),
        surfaceElevatedColor = Color(0xFF241E1A),
        surfaceHighlightColor = Color(0xFF362C26),
        surfaceGlassColor = Color(0xCC171311),
        textPrimaryColor = Color(0xFFFFFBEB),
        textSecondaryColor = Color(0xFFFDE68A),
        textMutedColor = Color(0xFFA16207),
        dividerColor = Color(0xFF2E251E),
        primaryGlowColor = Color(0x33F59E0B),
        secondaryGlowColor = Color(0x33F97316),
        auraTopColor = Color(0xFF2E1708),
        isDark = true
    ),

    EMERALD_ZEN(
        title = "Emerald Zen",
        subtitle = "Natural organic tuning, healing jade & vibrant mint",
        primaryColor = Color(0xFF10B981),
        primaryVariant = Color(0xFF059669),
        secondaryColor = Color(0xFF06B6D4),
        tertiaryColor = Color(0xFF84CC16),
        accentColor = Color(0xFF34D399),
        backgroundColor = Color(0xFF05100B),
        surfaceColor = Color(0xFF0B1B14),
        surfaceElevatedColor = Color(0xFF132B20),
        surfaceHighlightColor = Color(0xFF1C3D2E),
        surfaceGlassColor = Color(0xCC0B1B14),
        textPrimaryColor = Color(0xFFECFDF5),
        textSecondaryColor = Color(0xFF6EE7B7),
        textMutedColor = Color(0xFF047857),
        dividerColor = Color(0xFF163A29),
        primaryGlowColor = Color(0x3310B981),
        secondaryGlowColor = Color(0x3306B6D4),
        auraTopColor = Color(0xFF09291B),
        isDark = true
    ),

    SOLAR_DAYBREAK(
        title = "Solar Daybreak",
        subtitle = "Clean high-contrast daytime layout with sapphire accents",
        primaryColor = Color(0xFF2563EB),
        primaryVariant = Color(0xFF1D4ED8),
        secondaryColor = Color(0xFF7C3AED),
        tertiaryColor = Color(0xFFDB2777),
        accentColor = Color(0xFF059669),
        backgroundColor = Color(0xFFF8FAFC),
        surfaceColor = Color(0xFFFFFFFF),
        surfaceElevatedColor = Color(0xFFF1F5F9),
        surfaceHighlightColor = Color(0xFFE2E8F0),
        surfaceGlassColor = Color(0xEEFFFFFF),
        textPrimaryColor = Color(0xFF0F172A),
        textSecondaryColor = Color(0xFF475569),
        textMutedColor = Color(0xFF94A3B8),
        dividerColor = Color(0xFFCBD5E1),
        primaryGlowColor = Color(0x222563EB),
        secondaryGlowColor = Color(0x227C3AED),
        auraTopColor = Color(0xFFE0E7FF),
        isDark = false
    )
}

/**
 * Global Theme Manager with reactive state, custom accent color, and persistence.
 */
object GammaThemeManager {
    var selectedThemePreset by mutableStateOf(GammaThemePreset.COSMIC_CYAN)
        private set

    var customAccentColor by mutableStateOf<Color?>(null)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("gamma_theme_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getString("selected_preset", GammaThemePreset.COSMIC_CYAN.name)
        val customHex = prefs.getString("custom_accent_hex", null)
        val preset = GammaThemePreset.entries.find { it.name == saved } ?: GammaThemePreset.COSMIC_CYAN
        selectedThemePreset = preset
        if (!customHex.isNullOrBlank()) {
            try {
                val parsedColor = Color(android.graphics.Color.parseColor(customHex))
                customAccentColor = parsedColor
            } catch (_: Exception) {}
        }
    }

    fun selectTheme(preset: GammaThemePreset, context: Context? = null) {
        selectedThemePreset = preset
        customAccentColor = null
        context?.let {
            val prefs = it.getSharedPreferences("gamma_theme_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("selected_preset", preset.name)
                .remove("custom_accent_hex")
                .apply()
        }
    }

    fun setCustomColor(color: Color, context: Context? = null) {
        customAccentColor = color
        context?.let {
            val prefs = it.getSharedPreferences("gamma_theme_prefs", Context.MODE_PRIVATE)
            val argb = color.toArgb()
            val hex = String.format("#%08X", argb)
            prefs.edit().putString("custom_accent_hex", hex).apply()
        }
    }

    fun clearCustomColor(context: Context? = null) {
        customAccentColor = null
        context?.let {
            val prefs = it.getSharedPreferences("gamma_theme_prefs", Context.MODE_PRIVATE)
            prefs.edit().remove("custom_accent_hex").apply()
        }
    }
}

// ==========================================
// DYNAMIC REACTIVE COLOR TOKENS
// ==========================================
val GammaBackground: Color
    get() = GammaThemeManager.selectedThemePreset.backgroundColor

val GammaSurface: Color
    get() = GammaThemeManager.selectedThemePreset.surfaceColor

val GammaSurfaceElevated: Color
    get() = GammaThemeManager.selectedThemePreset.surfaceElevatedColor

val GammaSurfaceHighlight: Color
    get() = GammaThemeManager.selectedThemePreset.surfaceHighlightColor

val GammaSurfaceGlass: Color
    get() = GammaThemeManager.selectedThemePreset.surfaceGlassColor

val GammaPrimary: Color
    get() = GammaThemeManager.customAccentColor ?: GammaThemeManager.selectedThemePreset.primaryColor

val GammaPrimaryVariant: Color
    get() = GammaThemeManager.customAccentColor?.copy(alpha = 0.8f) ?: GammaThemeManager.selectedThemePreset.primaryVariant

val GammaSecondary: Color
    get() = GammaThemeManager.selectedThemePreset.secondaryColor

val GammaTertiary: Color
    get() = GammaThemeManager.selectedThemePreset.tertiaryColor

val GammaAccent: Color
    get() = GammaThemeManager.customAccentColor ?: GammaThemeManager.selectedThemePreset.accentColor

val GammaTextPrimary: Color
    get() = GammaThemeManager.selectedThemePreset.textPrimaryColor

val GammaTextSecondary: Color
    get() = GammaThemeManager.selectedThemePreset.textSecondaryColor

val GammaTextMuted: Color
    get() = GammaThemeManager.selectedThemePreset.textMutedColor

val GammaDivider: Color
    get() = GammaThemeManager.selectedThemePreset.dividerColor

val GammaError: Color
    get() = Color(0xFFFF5252)

val GammaSuccess: Color
    get() = GammaThemeManager.customAccentColor ?: GammaThemeManager.selectedThemePreset.accentColor

val GammaGlowCyan: Color
    get() = GammaThemeManager.customAccentColor?.copy(alpha = 0.25f) ?: GammaThemeManager.selectedThemePreset.primaryGlowColor

val GammaGlowViolet: Color
    get() = GammaThemeManager.selectedThemePreset.secondaryGlowColor

val GammaFrequencyBrush: Brush
    get() = Brush.horizontalGradient(
        colors = listOf(GammaPrimary, GammaSecondary)
    )

val GammaAuraBrush: Brush
    get() = Brush.verticalGradient(
        colors = listOf(
            GammaThemeManager.customAccentColor?.copy(alpha = 0.15f) ?: GammaThemeManager.selectedThemePreset.auraTopColor,
            GammaBackground
        )
    )

// Light Theme Alternates (Backward Compatibility)
val GammaLightBackground = Color(0xFFF8FAFC)
val GammaLightSurface = Color(0xFFFFFFFF)
val GammaLightSurfaceElevated = Color(0xFFEEF2F6)
val GammaLightTextPrimary = Color(0xFF0F172A)
val GammaLightTextSecondary = Color(0xFF475569)
val GammaLightDivider = Color(0xFFE2E8F0)

