package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.theme.PlayerTheme

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "video_player_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_THEME = "selected_theme"
        private const val KEY_HW_DECODING = "hw_decoding"
        private const val KEY_SMART_ENHANCER = "smart_enhancer"
        private const val KEY_REFRESH_RATE = "refresh_rate"
        private const val KEY_APP_ICON = "app_icon"
    }

    fun getSelectedTheme(): PlayerTheme {
        val themeName = prefs.getString(KEY_THEME, PlayerTheme.NEON_BLUE.name)
        return try {
            PlayerTheme.valueOf(themeName ?: PlayerTheme.NEON_BLUE.name)
        } catch (e: Exception) {
            PlayerTheme.NEON_BLUE
        }
    }

    fun setSelectedTheme(theme: PlayerTheme) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    fun isHardwareDecoding(): Boolean {
        return prefs.getBoolean(KEY_HW_DECODING, true) // true = HW, false = SW
    }

    fun setHardwareDecoding(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HW_DECODING, enabled).apply()
    }

    fun isSmartEnhancerEnabled(): Boolean {
        return prefs.getBoolean(KEY_SMART_ENHANCER, false)
    }

    fun setSmartEnhancerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_ENHANCER, enabled).apply()
    }

    fun getRefreshRate(): String {
        return prefs.getString(KEY_REFRESH_RATE, "120Hz") ?: "120Hz"
    }

    fun setRefreshRate(rate: String) {
        prefs.edit().putString(KEY_REFRESH_RATE, rate).apply()
    }

    fun getAppIcon(): String {
        return prefs.getString(KEY_APP_ICON, "Default") ?: "Default"
    }

    fun setAppIcon(icon: String) {
        prefs.edit().putString(KEY_APP_ICON, icon).apply()
    }
}
