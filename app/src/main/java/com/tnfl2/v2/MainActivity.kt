package com.tnfl2.v2

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.tnfl2.v2.ui.components.GlobalLoader
import com.tnfl2.v2.ui.theme.V2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize theme BEFORE first composition — no flash, no race condition
        val prefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val systemDark = (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        ThemeManager.init(prefs, systemDark)

        setContent {
            // ThemeManager.isDark is a global Compose state — reading it here
            // means this scope recomposes whenever it changes.
            val isDark = ThemeManager.isDark

            V2Theme(darkTheme = isDark) {
                Box(modifier = Modifier.fillMaxSize()) {
                    var token by remember { mutableStateOf<String?>(null) }

                    if (token != null) {
                        MainScreen(
                            onLogout = { token = null },
                            token = token!!,
                            onThemeChange = { ThemeManager.toggle(prefs) },
                            isDarkTheme = isDark
                        )
                    } else {
                        LoginScreen(
                            isDark = isDark,
                            onThemeChange = { ThemeManager.toggle(prefs) },
                            onLoginSuccess = { newToken -> token = newToken }
                        )
                    }

                    GlobalLoader()
                }
            }
        }
    }
}
