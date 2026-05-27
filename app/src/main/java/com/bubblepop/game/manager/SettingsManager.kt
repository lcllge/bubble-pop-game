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
    
    fun saveHighScore(score: Int) {
        val scores = getHighScores().toMutableList()
        scores.add(score)
        scores.sortDescending()
        val top3 = scores.take(3)
        
        prefs.edit {
            putInt(KEY_HIGH_SCORE_1, top3.getOrElse(0) { 0 })
            putInt(KEY_HIGH_SCORE_2, top3.getOrElse(1) { 0 })
            putInt(KEY_HIGH_SCORE_3, top3.getOrElse(2) { 0 })
        }
    }
    
    fun getHighScores(): List<Int> {
        return listOf(
            prefs.getInt(KEY_HIGH_SCORE_1, 0),
            prefs.getInt(KEY_HIGH_SCORE_2, 0),
            prefs.getInt(KEY_HIGH_SCORE_3, 0)
        )
    }
    
    companion object {
        private const val KEY_SOUND = "pref_sound"
        private const val KEY_VIBRATION = "pref_vibration"
        private const val KEY_BACKGROUND = "pref_background"
        private const val KEY_POP_COUNT = "pref_pop_count"
        private const val KEY_TOTAL_SCORE = "pref_total_score"
        private const val KEY_HIGH_SCORE_1 = "pref_high_score_1"
        private const val KEY_HIGH_SCORE_2 = "pref_high_score_2"
        private const val KEY_HIGH_SCORE_3 = "pref_high_score_3"
    }
}
