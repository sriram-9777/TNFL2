package com.tnfl2.v2

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Global theme state holder.
 * Any composable that reads [isDark] will automatically recompose when it changes.
 */
object ThemeManager {
    var isDark by mutableStateOf(false)
        private set

    fun init(prefs: SharedPreferences, systemDark: Boolean) {
        isDark = prefs.getBoolean("is_dark_theme", systemDark)
    }

    fun toggle(prefs: SharedPreferences) {
        isDark = !isDark
        prefs.edit().putBoolean("is_dark_theme", isDark).apply()
    }
}
