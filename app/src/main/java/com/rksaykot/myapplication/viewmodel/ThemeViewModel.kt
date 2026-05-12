package com.rksaykot.myapplication.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import android.content.SharedPreferences
import android.content.res.Configuration

/**
 * Simple theme manager that persists the selected theme option in SharedPreferences.
 * Supports three options: SYSTEM, LIGHT, DARK. Default is SYSTEM.
 */
enum class ThemeOption { SYSTEM, LIGHT, DARK }

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS = "app_prefs"
        private const val KEY_THEME = "app_theme_option"
    }

    private val prefs: SharedPreferences =
        application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var selectedOption by mutableStateOf(ThemeOption.SYSTEM)
        private set

    // computed boolean used by MyApplicationTheme
    var isDarkMode by mutableStateOf(false)
        private set

    init {
        val saved = prefs.getString(KEY_THEME, ThemeOption.SYSTEM.name) ?: ThemeOption.SYSTEM.name
        selectedOption = try {
            ThemeOption.valueOf(saved)
        } catch (e: Exception) {
            ThemeOption.SYSTEM
        }

        // initialize isDarkMode according to current selection
        applyEffectiveMode()
    }

    private fun applyEffectiveMode() {
        isDarkMode = when (selectedOption) {
            ThemeOption.SYSTEM -> {
                val cfg: Int = getApplication<Application>().resources.configuration.uiMode
                (cfg and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            }
            ThemeOption.LIGHT -> false
            ThemeOption.DARK -> true
        }
    }

    fun setThemeOption(option: ThemeOption) {
        selectedOption = option
        prefs.edit().putString(KEY_THEME, option.name).apply()
        applyEffectiveMode()
    }
}