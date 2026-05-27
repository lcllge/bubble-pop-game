package com.bubblepop.game.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class SettingsManager(context: Context) {
    
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    
    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit { putBoolean(KEY_SOUND, value) }
    
    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION, true)
        set(value) = prefs.edit { putBoolean(KEY_VIBRATION, value) }
    
    var background: String
        get() = prefs.getString(KEY_BACKGROUND, "default") ?: "default"
        set(value) = prefs.edit { putString(KEY_BACKGROUND, value) }
    
    var totalScore: Int
        get() = prefs.getInt(KEY_TOTAL_SCORE, 0)
        set(value) = prefs.edit { putInt(KEY_TOTAL_SCORE, value) }
    
    var popCount: Int
        get() = prefs.getInt(KEY_POP_COUNT, 0)
        set(value) = prefs.edit { putInt(KEY_POP_COUNT, value) }
    
    companion object {
        private const val KEY_SOUND = "pref_sound"
        private const val KEY_VIBRATION = "pref_vibration"
        private const val KEY_BACKGROUND = "pref_background"
        private const val KEY_POP_COUNT = "pref_pop_count"
        private const val KEY_TOTAL_SCORE = "pref_total_score"
    }
}
