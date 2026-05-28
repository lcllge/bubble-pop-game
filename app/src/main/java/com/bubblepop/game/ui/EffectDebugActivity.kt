package com.bubblepop.game.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bubblepop.game.R
import com.bubblepop.game.view.BubbleView
import com.bubblepop.game.view.BubbleView.DebugEffectConfig

class EffectDebugActivity : AppCompatActivity() {
    
    private lateinit var bubbleView: BubbleView
    private lateinit var controlPanel: ScrollView
    private lateinit var btnTogglePanel: Button
    private lateinit var btnTogglePanelFab: ImageButton
    private lateinit var btnTrigger: Button
    private lateinit var btnTriggerFab: Button
    private lateinit var btnQuick1: Button
    private lateinit var btnQuick2: Button
    private lateinit var btnQuick3: Button
    private lateinit var btnBack: Button
    
    private lateinit var cbConfetti: CheckBox
    private lateinit var cbGoldenRain: CheckBox
    private lateinit var cbScreenShake: CheckBox
    private lateinit var cbScreenFlash: CheckBox
    private lateinit var cbFirework: CheckBox
    private lateinit var cbCelebrationParticles: CheckBox
    private lateinit var cbSpiral: CheckBox
    private lateinit var cbRingExplosion: CheckBox
    private lateinit var cbShockwave: CheckBox
    private lateinit var cbCelebrationText: CheckBox
    
    private lateinit var etConfettiDuration: EditText
    private lateinit var etConfettiSize: EditText
    private lateinit var etGoldenRainDuration: EditText
    private lateinit var etGoldenRainSize: EditText
    private lateinit var etScreenShakeDuration: EditText
    private lateinit var etScreenFlashDuration: EditText
    private lateinit var etFireworkDuration: EditText
    private lateinit var etFireworkSize: EditText
    private lateinit var etCelebrationParticlesDuration: EditText
    private lateinit var etCelebrationParticlesSize: EditText
    private lateinit var etSpiralDuration: EditText
    private lateinit var etSpiralSize: EditText
    private lateinit var etRingExplosionDuration: EditText
    private lateinit var etRingExplosionSize: EditText
    private lateinit var etShockwaveDuration: EditText
    
    private lateinit var sbMode: SeekBar
    private lateinit var tvModeValue: TextView
    private lateinit var etCelebrationText: EditText
    
    private var panelVisible = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_effect_debug)
        
        bubbleView = findViewById(R.id.debug_bubble_view)
        bubbleView.debugMode = true
        controlPanel = findViewById(R.id.control_panel)
        btnTogglePanel = findViewById(R.id.btn_toggle_panel)
        btnTogglePanelFab = findViewById(R.id.btn_toggle_panel_fab)
        btnTrigger = findViewById(R.id.btn_trigger)
        btnTriggerFab = findViewById(R.id.btn_trigger_fab)
        btnQuick1 = findViewById(R.id.btn_quick_1)
        btnQuick2 = findViewById(R.id.btn_quick_2)
        btnQuick3 = findViewById(R.id.btn_quick_3)
        btnBack = findViewById(R.id.btn_back)
        
        cbConfetti = findViewById(R.id.cb_confetti)
        cbGoldenRain = findViewById(R.id.cb_golden_rain)
        cbScreenShake = findViewById(R.id.cb_screen_shake)
        cbScreenFlash = findViewById(R.id.cb_screen_flash)
        cbFirework = findViewById(R.id.cb_firework)
        cbCelebrationParticles = findViewById(R.id.cb_celebration_particles)
        cbSpiral = findViewById(R.id.cb_spiral)
        cbRingExplosion = findViewById(R.id.cb_ring_explosion)
        cbShockwave = findViewById(R.id.cb_shockwave)
        cbCelebrationText = findViewById(R.id.cb_celebration_text)
        
        etConfettiDuration = findViewById(R.id.et_confetti_duration)
        etConfettiSize = findViewById(R.id.et_confetti_size)
        etGoldenRainDuration = findViewById(R.id.et_golden_rain_duration)
        etGoldenRainSize = findViewById(R.id.et_golden_rain_size)
        etScreenShakeDuration = findViewById(R.id.et_screen_shake_duration)
        etScreenFlashDuration = findViewById(R.id.et_screen_flash_duration)
        etFireworkDuration = findViewById(R.id.et_firework_duration)
        etFireworkSize = findViewById(R.id.et_firework_size)
        etCelebrationParticlesDuration = findViewById(R.id.et_celebration_particles_duration)
        etCelebrationParticlesSize = findViewById(R.id.et_celebration_particles_size)
        etSpiralDuration = findViewById(R.id.et_spiral_duration)
        etSpiralSize = findViewById(R.id.et_spiral_size)
        etRingExplosionDuration = findViewById(R.id.et_ring_explosion_duration)
        etRingExplosionSize = findViewById(R.id.et_ring_explosion_size)
        etShockwaveDuration = findViewById(R.id.et_shockwave_duration)
        
        sbMode = findViewById(R.id.sb_mode)
        tvModeValue = findViewById(R.id.tv_mode_value)
        etCelebrationText = findViewById(R.id.et_celebration_text)
        
        sbMode.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvModeValue.text = "等级: ${progress + 1}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        btnTrigger.setOnClickListener { triggerCurrentEffect() }
        btnTriggerFab.setOnClickListener { triggerCurrentEffect() }
        
        btnQuick1.setOnClickListener {
            bubbleView.triggerDebugEffect(DebugEffectConfig(
                mode = 1,
                duration = 3000,
                text = "不错哦！",
                enableConfetti = true,
                confettiDuration = 3000,
                confettiSize = 1f,
                enableGoldenRain = true,
                goldenRainDuration = 3000,
                goldenRainSize = 1f,
                enableScreenShake = true,
                screenShakeDuration = 3000,
                enableScreenFlash = true,
                screenFlashDuration = 3000,
                enableFirework = false,
                enableCelebrationParticles = false,
                enableSpiral = false,
                enableRingExplosion = false,
                enableShockwave = false,
                enableCelebrationText = true
            ))
        }
        
        btnQuick2.setOnClickListener {
            bubbleView.triggerDebugEffect(DebugEffectConfig(
                mode = 3,
                duration = 5000,
                text = "太厉害了！",
                enableConfetti = true,
                confettiDuration = 5000,
                confettiSize = 1f,
                enableGoldenRain = true,
                goldenRainDuration = 5000,
                goldenRainSize = 1f,
                enableScreenShake = true,
                screenShakeDuration = 5000,
                enableScreenFlash = true,
                screenFlashDuration = 5000,
                enableFirework = true,
                fireworkDuration = 5000,
                fireworkSize = 1f,
                enableCelebrationParticles = true,
                celebrationParticlesDuration = 5000,
                celebrationParticlesSize = 1f,
                enableSpiral = true,
                spiralDuration = 5000,
                spiralSize = 1f,
                enableRingExplosion = true,
                ringExplosionDuration = 5000,
                ringExplosionSize = 1f,
                enableShockwave = true,
                shockwaveDuration = 5000,
                enableCelebrationText = true
            ))
        }
        
        btnQuick3.setOnClickListener {
            bubbleView.triggerDebugEffect(DebugEffectConfig(
                mode = 2,
                duration = 4000,
                text = "超神！",
                enableConfetti = false,
                enableGoldenRain = false,
                enableScreenShake = true,
                screenShakeDuration = 4000,
                enableScreenFlash = true,
                screenFlashDuration = 4000,
                enableFirework = true,
                fireworkDuration = 4000,
                fireworkSize = 1f,
                enableCelebrationParticles = true,
                celebrationParticlesDuration = 4000,
                celebrationParticlesSize = 1f,
                enableSpiral = true,
                spiralDuration = 4000,
                spiralSize = 1f,
                enableRingExplosion = false,
                enableShockwave = true,
                shockwaveDuration = 4000,
                enableCelebrationText = true
            ))
        }
        
        btnTogglePanel.setOnClickListener { togglePanel() }
        btnTogglePanelFab.setOnClickListener { togglePanel() }
        
        btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun togglePanel() {
        panelVisible = !panelVisible
        controlPanel.visibility = if (panelVisible) View.VISIBLE else View.GONE
        btnTogglePanel.text = if (panelVisible) "隐藏面板" else "显示面板"
    }
    
    private fun getLong(text: String, default: Long): Long {
        return text.toLongOrNull() ?: default
    }
    
    private fun getFloat(text: String, default: Float): Float {
        return text.toFloatOrNull() ?: default
    }
    
    private fun triggerCurrentEffect() {
        val mode = sbMode.progress + 1
        val text = etCelebrationText.text.toString().ifBlank { "太棒了！" }
        
        val config = DebugEffectConfig(
            mode = mode,
            text = text,
            enableConfetti = cbConfetti.isChecked,
            confettiDuration = getLong(etConfettiDuration.text.toString(), 3000),
            confettiSize = getFloat(etConfettiSize.text.toString(), 1f),
            enableGoldenRain = cbGoldenRain.isChecked,
            goldenRainDuration = getLong(etGoldenRainDuration.text.toString(), 3000),
            goldenRainSize = getFloat(etGoldenRainSize.text.toString(), 1f),
            enableScreenShake = cbScreenShake.isChecked,
            screenShakeDuration = getLong(etScreenShakeDuration.text.toString(), 3000),
            enableScreenFlash = cbScreenFlash.isChecked,
            screenFlashDuration = getLong(etScreenFlashDuration.text.toString(), 3000),
            enableFirework = cbFirework.isChecked,
            fireworkDuration = getLong(etFireworkDuration.text.toString(), 3000),
            fireworkSize = getFloat(etFireworkSize.text.toString(), 1f),
            enableCelebrationParticles = cbCelebrationParticles.isChecked,
            celebrationParticlesDuration = getLong(etCelebrationParticlesDuration.text.toString(), 3000),
            celebrationParticlesSize = getFloat(etCelebrationParticlesSize.text.toString(), 1f),
            enableSpiral = cbSpiral.isChecked,
            spiralDuration = getLong(etSpiralDuration.text.toString(), 3000),
            spiralSize = getFloat(etSpiralSize.text.toString(), 1f),
            enableRingExplosion = cbRingExplosion.isChecked,
            ringExplosionDuration = getLong(etRingExplosionDuration.text.toString(), 3000),
            ringExplosionSize = getFloat(etRingExplosionSize.text.toString(), 1f),
            enableShockwave = cbShockwave.isChecked,
            shockwaveDuration = getLong(etShockwaveDuration.text.toString(), 3000),
            enableCelebrationText = cbCelebrationText.isChecked
        )
        
        bubbleView.triggerDebugEffect(config)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
