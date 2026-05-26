package com.bubblepop.game.view

import android.animation.ValueAnimator
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
import com.bubblepop.game.manager.SettingsManager
import com.bubblepop.game.manager.SoundManager
import com.bubblepop.game.model.Bubble
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
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
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private val settingsManager: SettingsManager
        get() = (context.applicationContext as com.bubblepop.game.BubblePopApplication).settingsManager
    
    private val soundManager: SoundManager
        get() = (context.applicationContext as com.bubblepop.game.BubblePopApplication).soundManager
    
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    private var screenWidth = 0
    private var screenHeight = 0
    private var score = 0
    private var isGameOver = false
    private var isPaused = false
    private var lastSpawnTime = 0L
    
    // Effects state
    private var celebrationMode = 0 // 0=none, 1=firework(100), 2=golden(500), 3=ultimate(1000)
    private var celebrationProgress = 0f
    private var celebrationAnimator: ValueAnimator? = null
    private val particles = mutableListOf<Particle>()
    private val goldenRays = mutableListOf<GoldenRay>()
    private var edgeGlowAlpha = 0f
    private var edgeGlowDirection = 1f
    
    // Callbacks
    var onScoreChanged: ((Int) -> Unit)? = null
    var onGameOver: (() -> Unit)? = null
    var onCelebration: ((Int) -> Unit)? = null
    
    companion object {
        private const val MAX_BUBBLES = 30
        private const val MIN_BUBBLES = 8
        private const val GROWTH_RATE = 0.15f // pixels per frame
        private const val GLOW_THRESHOLD = 0.33f // 1/3 screen width
    }
    
    init {
        highlightPaint.style = Paint.Style.FILL
        glowPaint.style = Paint.Style.FILL
        particlePaint.style = Paint.Style.FILL
    }
    
    private val glowThreshold: Float
        get() = screenWidth * GLOW_THRESHOLD
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w
        screenHeight = h
        
        if (bubbles.isEmpty() && !isGameOver) {
            repeat(MIN_BUBBLES) {
                bubbles.add(Bubble.createRandom(w, h))
            }
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw background
        drawBackground(canvas)
        
        // Draw edge glow
        drawEdgeGlow(canvas)
        
        if (!isGameOver) {
            // Update and draw bubbles
            bubbles.removeAll { it.isPopped && it.popProgress >= 1f }
            
            for (bubble in bubbles) {
                if (bubble.isPopped) {
                    bubble.popProgress += 0.04f
                    drawPoppingBubble(canvas, bubble)
                } else {
                    updateBubble(bubble)
                    drawBubble(canvas, bubble)
                }
            }
            
            // Spawn new bubbles
            val now = System.currentTimeMillis()
            if (bubbles.count { !it.isPopped } < MIN_BUBBLES && now - lastSpawnTime > 500) {
                bubbles.add(Bubble.createRandom(screenWidth, screenHeight))
                lastSpawnTime = now
            }
            
            // Check game over
            if (bubbles.count { !it.isPopped } >= MAX_BUBBLES && 
                bubbles.all { it.radius >= glowThreshold || it.isPopped }) {
                triggerGameOver()
            }
        }
        
        // Draw celebration effects
        drawCelebration(canvas)
        
        // Draw particles
        drawParticles(canvas)
        
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
    
    private fun drawEdgeGlow(canvas: Canvas) {
        if (bubbles.any { it.radius >= glowThreshold && !it.isPopped }) {
            edgeGlowAlpha = min(1f, edgeGlowAlpha + 0.02f * edgeGlowDirection)
            if (edgeGlowAlpha >= 1f) edgeGlowDirection = -1f
            if (edgeGlowAlpha <= 0.3f) edgeGlowDirection = 1f
            
            val glowColor = Color.argb((edgeGlowAlpha * 80).toInt(), 255, 200, 100)
            val gradientSize = 60f
            
            // Top edge
            val topGrad = RadialGradient(
                screenWidth / 2f, 0f, gradientSize,
                glowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            paint.shader = topGrad
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), gradientSize, paint)
            
            // Bottom edge
            val bottomGrad = RadialGradient(
                screenWidth / 2f, screenHeight.toFloat(), gradientSize,
                glowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            paint.shader = bottomGrad
            canvas.drawRect(0f, screenHeight - gradientSize, screenWidth.toFloat(), screenHeight.toFloat(), paint)
            
            // Left edge
            val leftGrad = RadialGradient(
                0f, screenHeight / 2f, gradientSize,
                glowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            paint.shader = leftGrad
            canvas.drawRect(0f, 0f, gradientSize, screenHeight.toFloat(), paint)
            
            // Right edge
            val rightGrad = RadialGradient(
                screenWidth.toFloat(), screenHeight / 2f, gradientSize,
                glowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            paint.shader = rightGrad
            canvas.drawRect(screenWidth - gradientSize, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paint)
            
            paint.shader = null
        } else {
            edgeGlowAlpha = 0f
        }
    }
    
    private fun updateBubble(bubble: Bubble) {
        // Grow bubble
        bubble.radius += GROWTH_RATE
        
        // Wobble effect
        bubble.wobblePhase += bubble.wobbleSpeed
        val wobbleOffset = sin(bubble.wobblePhase * PI / 180f).toFloat() * bubble.wobbleAmplitude
        
        // Move bubble
        bubble.x += bubble.velocityX + wobbleOffset * 0.2f
        bubble.y += bubble.velocityY
        
        // Bounce off edges (keep bubble fully on screen)
        if (bubble.x - bubble.radius < 0) {
            bubble.x = bubble.radius
            bubble.velocityX = kotlin.math.abs(bubble.velocityX)
        }
        if (bubble.x + bubble.radius > screenWidth) {
            bubble.x = screenWidth - bubble.radius
            bubble.velocityX = -kotlin.math.abs(bubble.velocityX)
        }
        if (bubble.y - bubble.radius < 0) {
            bubble.y = bubble.radius
            bubble.velocityY = kotlin.math.abs(bubble.velocityY)
        }
        if (bubble.y + bubble.radius > screenHeight) {
            bubble.y = screenHeight - bubble.radius
            bubble.velocityY = -kotlin.math.abs(bubble.velocityY)
        }
        
        // Slight gravity
        bubble.velocityY += 0.01f
        
        // Glow phase for breathing effect
        bubble.glowPhase += 2f
    }
    
    private fun drawBubble(canvas: Canvas, bubble: Bubble) {
        val isGlowing = bubble.radius >= glowThreshold
        
        if (isGlowing) {
            // Breathing glow
            val breathe = (sin(bubble.glowPhase * PI / 180f).toFloat() + 1f) / 2f
            val glowRadius = bubble.radius * (1.3f + breathe * 0.3f)
            val glowAlpha = (80 + breathe * 60).toInt()
            
            val glowGradient = RadialGradient(
                bubble.x, bubble.y, glowRadius,
                bubble.color,
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            glowPaint.shader = glowGradient
            glowPaint.alpha = glowAlpha
            canvas.drawCircle(bubble.x, bubble.y, glowRadius, glowPaint)
            glowPaint.shader = null
        }
        
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
        highlightPaint.alpha = if (isGlowing) 90 else 60
        canvas.drawCircle(
            bubble.x - bubble.radius * 0.25f,
            bubble.y - bubble.radius * 0.25f,
            bubble.radius * 0.3f,
            highlightPaint
        )
        
        // Extra glow ring for big bubbles
        if (isGlowing) {
            val breathe = (sin(bubble.glowPhase * PI / 180f).toFloat() + 1f) / 2f
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f + breathe * 3f
            paint.alpha = (100 + breathe * 80).toInt()
            paint.color = lightenColor(bubble.color, 100)
            canvas.drawCircle(bubble.x, bubble.y, bubble.radius + 5f, paint)
            paint.style = Paint.Style.FILL
            paint.shader = null
        }
    }
    
    private fun drawPoppingBubble(canvas: Canvas, bubble: Bubble) {
        val progress = bubble.popProgress
        val expandedRadius = bubble.radius * (1 + progress * 1.2f)
        val alpha = (220 * (1 - progress)).toInt()
        
        paint.color = bubble.color
        paint.alpha = alpha
        canvas.drawCircle(bubble.x, bubble.y, expandedRadius, paint)
        
        // Draw pop particles
        val particleCount = if (bubble.radius >= glowThreshold) 12 else 6
        for (i in 0 until particleCount) {
            val angle = i * (360f / particleCount) + progress * 120f
            val rad = angle * PI / 180f
            val px = bubble.x + cos(rad).toFloat() * expandedRadius * 1.3f
            val py = bubble.y + sin(rad).toFloat() * expandedRadius * 1.3f
            val particleSize = bubble.radius * 0.12f * (1 - progress)
            
            paint.alpha = alpha
            canvas.drawCircle(px, py, particleSize, paint)
        }
        
        paint.shader = null
    }
    
    private fun lightenColor(color: Int, amount: Int): Int {
        val r = min(255, Color.red(color) + amount)
        val g = min(255, Color.green(color) + amount)
        val b = min(255, Color.blue(color) + amount)
        return Color.rgb(r, g, b)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (!isGameOver && !isPaused) {
                    handleTouch(event.getX(event.actionIndex), event.getY(event.actionIndex))
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun handleTouch(x: Float, y: Float) {
        var closestBubble: Bubble? = null
        var closestDist = Float.MAX_VALUE
        
        for (bubble in bubbles) {
            if (bubble.isPopped) continue
            val dx = bubble.x - x
            val dy = bubble.y - y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < bubble.radius + 10f && dist < closestDist) {
                closestDist = dist
                closestBubble = bubble
            }
        }
        
        closestBubble?.let { popBubble(it) }
    }
    
    private fun popBubble(bubble: Bubble) {
        bubble.isPopped = true
        bubble.popProgress = 0f
        
        val isBig = bubble.radius >= glowThreshold
        val scorePoints = calculateScore(bubble)
        score += scorePoints
        onScoreChanged?.invoke(score)
        
        // Sound
        if (settingsManager.soundEnabled) {
            soundManager.playPop(isBig)
        }
        
        // Vibrate - stronger for bigger bubbles
        if (settingsManager.vibrationEnabled) {
            val duration = if (isBig) 80 else 30
            vibrate(duration)
        }
        
        // Spawn particles
        spawnPopParticles(bubble)
        
        // Check milestones
        checkMilestones()
    }
    
    private fun calculateScore(bubble: Bubble): Int {
        val halfScreen = screenWidth * 0.5f
        return when {
            bubble.radius >= halfScreen -> 3
            bubble.radius >= glowThreshold -> 1
            else -> 0
        }
    }
    
    private fun checkMilestones() {
        when {
            score >= 1000 && celebrationMode < 3 -> {
                celebrationMode = 3
                triggerCelebration()
            }
            score >= 500 && celebrationMode < 2 -> {
                celebrationMode = 2
                triggerCelebration()
            }
            score >= 100 && celebrationMode < 1 -> {
                celebrationMode = 1
                triggerCelebration()
            }
        }
    }
    
    private fun triggerCelebration() {
        isPaused = true
        celebrationProgress = 0f
        
        celebrationAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = when (celebrationMode) {
                1 -> 2500L
                2 -> 4000L
                3 -> 6000L
                else -> 2000L
            }
            addUpdateListener {
                celebrationProgress = it.animatedValue as Float
                spawnCelebrationParticles()
            }
            start()
        }
        
        // Sound effects
        if (settingsManager.soundEnabled) {
            soundManager.playFirework()
            if (celebrationMode >= 2) {
                postDelayed({ soundManager.playCheer() }, 500)
            }
            if (celebrationMode >= 3) {
                postDelayed({ soundManager.playCheer() }, 1500)
                postDelayed({ soundManager.playFirework() }, 2000)
            }
        }
        
        onCelebration?.invoke(celebrationMode)
        
        // Resume after celebration
        postDelayed({
            isPaused = false
            if (celebrationMode >= 3) {
                // Ultimate: keep celebration going
                celebrationMode = 2
            }
        }, (celebrationAnimator?.duration ?: 2000) + 500)
    }
    
    private fun spawnCelebrationParticles() {
        val count = when (celebrationMode) {
            1 -> 3
            2 -> 8
            3 -> 15
            else -> 0
        }
        
        repeat(count) {
            particles.add(Particle(
                x = Random.nextFloat() * screenWidth,
                y = screenHeight + 20f,
                vx = Random.nextFloat() * 6f - 3f,
                vy = -Random.nextFloat() * 15f - 8f,
                color = Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256)),
                size = Random.nextFloat() * 8f + 3f,
                life = 1f,
                decay = Random.nextFloat() * 0.01f + 0.005f
            ))
        }
        
        // Golden rays for mode 2+
        if (celebrationMode >= 2) {
            repeat(2) {
                goldenRays.add(GoldenRay(
                    x = Random.nextFloat() * screenWidth,
                    y = Random.nextFloat() * screenHeight * 0.5f,
                    angle = Random.nextFloat() * 360f,
                    length = Random.nextFloat() * 200f + 100f,
                    alpha = 1f,
                    decay = 0.02f
                ))
            }
        }
    }
    
    private fun spawnPopParticles(bubble: Bubble) {
        val count = if (bubble.radius >= glowThreshold) 10 else 5
        repeat(count) {
            val angle = Random.nextFloat() * 360f
            val speed = Random.nextFloat() * 5f + 2f
            particles.add(Particle(
                x = bubble.x,
                y = bubble.y,
                vx = cos(angle * PI / 180f).toFloat() * speed,
                vy = sin(angle * PI / 180f).toFloat() * speed,
                color = bubble.color,
                size = Random.nextFloat() * 5f + 2f,
                life = 1f,
                decay = Random.nextFloat() * 0.03f + 0.02f
            ))
        }
    }
    
    private fun drawCelebration(canvas: Canvas) {
        if (celebrationMode == 0) return
        
        // Golden overlay for mode 2+
        if (celebrationMode >= 2) {
            val breathe = (sin(celebrationProgress * PI * 4).toFloat() + 1f) / 2f
            val alpha = (30 + breathe * 40).toInt()
            paint.color = Color.argb(alpha, 255, 215, 0)
            canvas.drawColor(paint.color)
        }
        
        // Golden rays
        for (ray in goldenRays) {
            ray.alpha -= ray.decay
            if (ray.alpha > 0) {
                paint.color = Color.argb((ray.alpha * 150).toInt(), 255, 215, 0)
                paint.strokeWidth = 3f
                val endX = ray.x + cos(ray.angle * PI / 180f).toFloat() * ray.length
                val endY = ray.y + sin(ray.angle * PI / 180f).toFloat() * ray.length
                canvas.drawLine(ray.x, ray.y, endX, endY, paint)
            }
        }
        goldenRays.removeAll { it.alpha <= 0 }
    }
    
    private fun drawParticles(canvas: Canvas) {
        for (p in particles) {
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.15f // gravity
            p.life -= p.decay
            
            if (p.life > 0) {
                particlePaint.color = p.color
                particlePaint.alpha = (p.life * 255).toInt()
                canvas.drawCircle(p.x, p.y, p.size * p.life, particlePaint)
            }
        }
        particles.removeAll { it.life <= 0 }
    }
    
    private fun triggerGameOver() {
        isGameOver = true
        onGameOver?.invoke()
    }
    
    private fun vibrate(duration: Int) {
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(duration.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(duration.toLong())
            }
        }
    }
    
    fun resetGame() {
        bubbles.clear()
        particles.clear()
        goldenRays.clear()
        score = 0
        isGameOver = false
        isPaused = false
        celebrationMode = 0
        celebrationProgress = 0f
        edgeGlowAlpha = 0f
        onScoreChanged?.invoke(0)
        
        repeat(MIN_BUBBLES) {
            bubbles.add(Bubble.createRandom(screenWidth, screenHeight))
        }
    }
    
    fun getScore(): Int = score
    
    data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var color: Int,
        var size: Float,
        var life: Float,
        var decay: Float
    )
    
    data class GoldenRay(
        var x: Float,
        var y: Float,
        var angle: Float,
        var length: Float,
        var alpha: Float,
        var decay: Float
    )
}
