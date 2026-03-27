package com.amakaflow.companion.data.nutrition

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AMA-1292: Repository for nutrition privacy settings.
 * Uses SharedPreferences for local-only storage — nutrition preferences never leave the device.
 */
@Singleton
class NutritionSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSettings(): NutritionSettings {
        return NutritionSettings(
            isEnabled = prefs.getBoolean(KEY_ENABLED, false),
            hasCompletedOnboarding = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false),
            displayMode = NutritionDisplayMode.entries.getOrElse(
                prefs.getInt(KEY_DISPLAY_MODE, 0)
            ) { NutritionDisplayMode.QUALITATIVE },
            proteinTargetGrams = prefs.getInt(KEY_PROTEIN_TARGET, DEFAULT_PROTEIN_TARGET),
            waterTargetMl = prefs.getInt(KEY_WATER_TARGET, DEFAULT_WATER_TARGET)
        )
    }

    fun updateSettings(settings: NutritionSettings) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, settings.isEnabled)
            .putBoolean(KEY_ONBOARDING_COMPLETE, settings.hasCompletedOnboarding)
            .putInt(KEY_DISPLAY_MODE, settings.displayMode.ordinal)
            .putInt(KEY_PROTEIN_TARGET, settings.proteinTargetGrams)
            .putInt(KEY_WATER_TARGET, settings.waterTargetMl)
            .apply()
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun setOnboardingComplete(complete: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, complete).apply()
    }

    fun setDisplayMode(mode: NutritionDisplayMode) {
        prefs.edit().putInt(KEY_DISPLAY_MODE, mode.ordinal).apply()
    }

    fun setProteinTarget(grams: Int) {
        prefs.edit().putInt(KEY_PROTEIN_TARGET, grams).apply()
    }

    fun setWaterTarget(ml: Int) {
        prefs.edit().putInt(KEY_WATER_TARGET, ml).apply()
    }

    /**
     * AMA-1292: Delete all nutrition data — full privacy wipe.
     */
    fun deleteAllData() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "nutrition_settings"
        private const val KEY_ENABLED = "nutrition_enabled"
        private const val KEY_ONBOARDING_COMPLETE = "nutrition_onboarding_complete"
        private const val KEY_DISPLAY_MODE = "nutrition_display_mode"
        private const val KEY_PROTEIN_TARGET = "nutrition_protein_target"
        private const val KEY_WATER_TARGET = "nutrition_water_target"
        const val DEFAULT_PROTEIN_TARGET = 120
        const val DEFAULT_WATER_TARGET = 2500
    }
}
