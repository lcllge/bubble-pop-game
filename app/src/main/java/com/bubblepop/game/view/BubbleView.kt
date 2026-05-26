package com.bubblepop.game.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.bubblepop.game.R
import com.bubblepop.game.manager.SettingsManager
import com.bubblepop.game.manager.SoundManager
import com.bubblepop.game.model.Bubble
import kotlin.math.sqrt
import kotlin.random.Random

class BubbleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bubbles = mutableListOf<Bubble>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private val settingsManager: SettingsManager
        get() = (context.applicationContext as com.bubblepop.game.BubblePopApplication).settingsManager
    
    private val soundManager: SoundManager
        get() = (context.applicationContext as com.bubblepop.game.BubblePopApplication).soundManager
    
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    private var screenWidth = 0
    private var screenHeight = 0
    private var popCountCallback: ((Int) -> Unit)? = null
    
    companion object {
        private const val MAX_BUBBLES = 25
        private const val MIN_BUBBLES = 15
        private const val SPAWN_INTERVAL = 800L
    }
    
    init {
        highlightPaint.style = Paint.Style.FILL
        highlightPaint.alpha = 80
    }
    
    fun setPopCountCallback(callback: (Int) -> Unit) {
        popCountCallback = callback
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w
        screenHeight = h
        
        if (bubbles.isEmpty()) {
            repeat(MIN_BUBBLES) {
                bubbles.add(Bubble.createRandom(w, h))
            }
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw background
        drawBackground(canvas)
        
        // Update and draw bubbles
        val currentTime = System.currentTimeMillis()
        bubbles.removeAll { it.isPopped && it.popProgress >= 1f }
        
        for (bubble in bubbles) {
            if (bubble.isPopped) {
                bubble.popProgress += 0.05f
                drawPoppingBubble(canvas, bubble)
            } else {
                updateBubble(bubble, currentTime)
                drawBubble(canvas, bubble)
            }
        }
        
        // Spawn new bubbles
        if (bubbles.count { !it.isPopped } < MIN_BUBBLES) {
            bubbles.add(Bubble.createRandom(screenWidth, screenHeight))
        }
        
        invalidate()
    }
    
    private fun drawBackground(canvas: Canvas) {
        val bg = when (settingsManager.background) {
            "dark" -> Color.parseColor("#0D0D0D")
            "sunset" -> Color.parseColor("#2D1B36")
            "ocean" -> Color.parseColor("#0C2340")
            else -> Color.parseColor("#1A1A2E")
        }
        canvas.drawColor(bg)
    }
    
    private fun updateBubble(bubble: Bubble, time: Long) {
        // Wobble effect
        bubble.wobblePhase += bubble.wobbleSpeed
        val wobbleOffset = kotlin.math.sin(Math.toRadians(bubble.wobblePhase.toDouble())).toFloat() * bubble.wobbleAmplitude
        
        // Move bubble
        bubble.x += bubble.velocityX + wobbleOffset * 0.3f
        bubble.y += bubble.velocityY
        
        // Bounce off edges
        if (bubble.x - bubble.radius < 0) {
            bubble.x = bubble.radius
            bubble.velocityX = -bubble.velocityX
        }
        if (bubble.x + bubble.radius > screenWidth) {
            bubble.x = screenWidth - bubble.radius
            bubble.velocityX = -bubble.velocityX
        }
        if (bubble.y - bubble.radius < 0) {
            bubble.y = bubble.radius
            bubble.velocityY = -bubble.velocityY
        }
        if (bubble.y + bubble.radius > screenHeight) {
            bubble.y = screenHeight - bubble.radius
            bubble.velocityY = -bubble.velocityY
        }
        
        // Slight gravity effect for falling feel
        bubble.velocityY += 0.02f
    }
    
    private fun drawBubble(canvas: Canvas, bubble: Bubble) {
        // Main bubble gradient
        val gradient = RadialGradient(
            bubble.x - bubble.radius * 0.3f,
            bubble.y - bubble.radius * 0.3f,
            bubble.radius,
            lightenColor(bubble.color, 60),
            bubble.color,
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        paint.alpha = (bubble.alpha * 220).toInt()
        
        canvas.drawCircle(bubble.x, bubble.y, bubble.radius, paint)
        
        // Highlight
        highlightPaint.color = Color.WHITE
        highlightPaint.alpha = 60
        canvas.drawCircle(
            bubble.x - bubble.radius * 0.25f,
            bubble.y - bubble.radius * 0.25f,
            bubble.radius * 0.3f,
            highlightPaint
        )
        
        paint.shader = null
    }
    
    private fun drawPoppingBubble(canvas: Canvas, bubble: Bubble) {
        val progress = bubble.popProgress
        val expandedRadius = bubble.radius * (1 + progress * 0.8f)
        val alpha = (220 * (1 - progress)).toInt()
        
        paint.color = bubble.color
        paint.alpha = alpha
        canvas.drawCircle(bubble.x, bubble.y, expandedRadius, paint)
        
        // Draw pop particles
        for (i in 0 until 6) {
            val angle = i * 60f + progress * 90f
            val rad = Math.toRadians(angle.toDouble())
            val px = bubble.x + kotlin.math.cos(rad).toFloat() * expandedRadius * 1.2f
            val py = bubble.y + kotlin.math.sin(rad).toFloat() * expandedRadius * 1.2f
            val particleSize = bubble.radius * 0.15f * (1 - progress)
            
            paint.alpha = alpha
            canvas.drawCircle(px, py, particleSize, paint)
        }
    }
    
    private fun lightenColor(color: Int, amount: Int): Int {
        val r = Math.min(255, Color.red(color) + amount)
        val g = Math.min(255, Color.green(color) + amount)
        val b = Math.min(255, Color.blue(color) + amount)
        return Color.rgb(r, g, b)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                handleTouch(event.x, event.y)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun handleTouch(x: Float, y: Float) {
        // Find closest bubble to touch point
        var closestBubble: Bubble? = null
        var closestDist = Float.MAX_VALUE
        
        for (bubble in bubbles) {
            if (bubble.isPopped) continue
            val dx = bubble.x - x
            val dy = bubble.y - y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < bubble.radius && dist < closestDist) {
                closestDist = dist
                closestBubble = bubble
            }
        }
        
        closestBubble?.let { bubble ->
            popBubble(bubble)
        }
    }
    
    private fun popBubble(bubble: Bubble) {
        bubble.isPopped = true
        bubble.popProgress = 0f
        
        // Update pop count
        settingsManager.popCount++
        popCountCallback?.invoke(settingsManager.popCount)
        
        // Play sound
        if (settingsManager.soundEnabled) {
            soundManager.playPop()
        }
        
        // Vibrate
        if (settingsManager.vibrationEnabled) {
            vibrate()
        }
        
        // Spawn a new bubble to replace
        if (bubbles.count { !it.isPopped } < MAX_BUBBLES) {
            bubbles.add(Bubble.createRandom(screenWidth, screenHeight))
        }
    }
    
    private fun vibrate() {
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(30)
            }
        }
    }
    
    fun clearAllBubbles() {
        bubbles.clear()
        repeat(MIN_BUBBLES) {
            bubbles.add(Bubble.createRandom(screenWidth, screenHeight))
        }
    }
}
