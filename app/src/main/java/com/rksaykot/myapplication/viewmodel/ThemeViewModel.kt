package com.rksaykot.myapplication.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import android.content.Context
import android.content.SharedPreferences

class ThemeViewModel : ViewModel() {
    var isDarkMode by mutableStateOf(false)

    fun toggleDarkMode() {
        isDarkMode = !isDarkMode
    }

    fun updateDarkMode(isDark: Boolean) {
        isDarkMode = isDark
    }

    fun toggleTheme() {
        isDarkMode = !isDarkMode
    }
}