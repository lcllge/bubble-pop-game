package com.bubblepop.game

import android.app.Application
import com.bubblepop.game.manager.SettingsManager
import com.bubblepop.game.manager.SoundManager

class BubblePopApplication : Application() {
    
    lateinit var settingsManager: SettingsManager
        private set
    
    lateinit var soundManager: SoundManager
        private set

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(this)
        soundManager = SoundManager(this)
    }
    
    override fun onTerminate() {
        super.onTerminate()
        soundManager.release()
    }
}
