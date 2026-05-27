package com.bubblepop.game.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.LinearGradient
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
    private val baroquePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
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
    private var totalScore = 0 // 累计分数
    private var isGameOver = false
    private var isPaused = false
    private var lastSpawnTime = 0L
    private var totalScoreLoaded = false
    
    // Press interaction state
    private var pressedBubble: Bubble? = null
    private var isPressing = false
    private var pressProgress = 0f
    private var pressGlowAlpha = 0f
    
    // Effects state
    private var celebrationMode = 0
    private var celebrationProgress = 0f
    private var celebrationAnimator: ValueAnimator? = null
    private val particles = mutableListOf<Particle>()
    private val goldenRays = mutableListOf<GoldenRay>()
    private var edgeGlowAlpha = 0f
    private var edgeGlowDirection = 1f
    
    // Baroque decorative elements
    private val baroqueOrnaments = mutableListOf<BaroqueOrnament>()
    
    // Callbacks
    var onScoreChanged: ((Int) -> Unit)? = null
    var onTotalScoreChanged: ((Int) -> Unit)? = null
    var onGameOver: (() -> Unit)? = null
    var onCelebration: ((Int) -> Unit)? = null
    
    companion object {
        private const val MAX_BUBBLES = 30
        private const val MIN_BUBBLES = 8
        private const val GROWTH_RATE = 0.15f
        private const val GLOW_THRESHOLD = 0.33f
    }
    
    init {
        highlightPaint.style = Paint.Style.FILL
        glowPaint.style = Paint.Style.FILL
        particlePaint.style = Paint.Style.FILL
        baroquePaint.style = Paint.Style.STROKE
        baroquePaint.strokeWidth = 2f
        baroquePaint.color = Color.argb(60, 218, 165, 32)
        
        // Initialize baroque ornaments
        initBaroqueOrnaments()
    }
    
    private val glowThreshold: Float
        get() = screenWidth * GLOW_THRESHOLD
    
    private fun initBaroqueOrnaments() {
        // Pre-generate some ornamental positions
        repeat(12) {
            baroqueOrnaments.add(BaroqueOrnament(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 20f + 10f,
                rotation = Random.nextFloat() * 360f,
                alpha = Random.nextFloat() * 0.3f + 0.1f
            ))
        }
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w
        screenHeight = h
        
        if (!totalScoreLoaded) {
            totalScore = settingsManager.totalScore
            totalScoreLoaded = true
            onTotalScoreChanged?.invoke(totalScore)
        }
        
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
        
        // Draw baroque decorations
        drawBaroqueDecorations(canvas)
        
        // Draw edge glow
        drawEdgeGlow(canvas)
        
        if (!isGameOver) {
            // Update and draw bubbles
            bubbles.removeAll { it.isPopped && it.popProgress >= 1f }
            
            val shouldPauseGrowth = isPressing && pressedBubble != null
            
            for (bubble in bubbles) {
                if (bubble.isPopped) {
                    bubble.popProgress += 0.04f
                    drawPoppingBubble(canvas, bubble)
                } else if (bubble == pressedBubble && isPressing) {
                    // Draw pressed bubble with special effect
                    drawPressedBubble(canvas, bubble)
                } else {
                    if (!shouldPauseGrowth) {
                        updateBubble(bubble)
                    }
                    drawBubble(canvas, bubble)
                }
            }
            
            // Update press animation
            if (isPressing && pressedBubble != null) {
                pressProgress = min(1f, pressProgress + 0.03f)
                pressGlowAlpha = sin(pressProgress * PI / 2f).toFloat()
            }
            
            // Spawn new bubbles
            val now = System.currentTimeMillis()
            if (bubbles.count { !it.isPopped } < MIN_BUBBLES && now - lastSpawnTime > 500) {
                bubbles.add(Bubble.createRandom(screenWidth, screenHeight))
                lastSpawnTime = now
            }
            
            // Check game over
            val activeBubbles = bubbles.filter { !it.isPopped }
            if (activeBubbles.size >= MAX_BUBBLES && 
                activeBubbles.all { it.radius >= glowThreshold }) {
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
            "warm" -> Color.parseColor("#F5E6D3")
            "lavender" -> Color.parseColor("#E8D5E0")
            "sage" -> Color.parseColor("#D5E0D5")
            else -> Color.parseColor("#F0E4D7")
        }
        canvas.drawColor(bg)
    }
    
    private fun drawBaroqueDecorations(canvas: Canvas) {
        // Draw ornate border
        val borderWidth = 8f
        val goldColor = Color.argb(80, 218, 165, 32)
        
        paint.color = goldColor
        paint.strokeWidth = borderWidth
        paint.style = Paint.Style.STROKE
        
        // Outer border
        canvas.drawRect(
            borderWidth / 2, borderWidth / 2,
            screenWidth - borderWidth / 2, screenHeight - borderWidth / 2,
            paint
        )
        
        // Inner border
        paint.strokeWidth = 2f
        paint.alpha = 50
        canvas.drawRect(
            borderWidth + 4, borderWidth + 4,
            screenWidth - borderWidth - 4, screenHeight - borderWidth - 4,
            paint
        )
        
        // Corner ornaments
        drawCornerOrnament(canvas, borderWidth + 10, borderWidth + 10, 1f, 1f)
        drawCornerOrnament(canvas, screenWidth - borderWidth - 10, borderWidth + 10, -1f, 1f)
        drawCornerOrnament(canvas, borderWidth + 10, screenHeight - borderWidth - 10, 1f, -1f)
        drawCornerOrnament(canvas, screenWidth - borderWidth - 10, screenHeight - borderWidth - 10, -1f, -1f)
        
        // Scattered decorative swirls
        for (ornament in baroqueOrnaments) {
            val ox = ornament.x * screenWidth
            val oy = ornament.y * screenHeight
            drawSmallSwirl(canvas, ox, oy, ornament.size, ornament.rotation, ornament.alpha)
        }
        
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }
    
    private fun drawCornerOrnament(canvas: Canvas, x: Float, y: Float, scaleX: Float, scaleY: Float) {
        paint.color = Color.argb(100, 218, 165, 32)
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(scaleX, scaleY)
        
        // Curved flourish
        val path = android.graphics.Path()
        path.moveTo(0f, 0f)
        path.cubicTo(15f, 0f, 30f, 10f, 30f, 25f)
        path.cubicTo(30f, 40f, 15f, 45f, 0f, 45f)
        canvas.drawPath(path, paint)
        
        // Small circle
        paint.style = Paint.Style.FILL
        canvas.drawCircle(30f, 25f, 3f, paint)
        canvas.drawCircle(0f, 45f, 3f, paint)
        
        canvas.restore()
        paint.style = Paint.Style.FILL
    }
    
    private fun drawSmallSwirl(canvas: Canvas, x: Float, y: Float, size: Float, rotation: Float, alpha: Float) {
        paint.color = Color.argb((alpha * 60).toInt(), 184, 134, 11)
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(rotation)
        
        val path = android.graphics.Path()
        path.moveTo(0f, 0f)
        path.cubicTo(size / 2, -size / 3, size, 0f, size / 2, size / 2)
        canvas.drawPath(path, paint)
        
        canvas.restore()
        paint.style = Paint.Style.FILL
    }
    
    private fun drawEdgeGlow(canvas: Canvas) {
        if (bubbles.any { it.radius >= glowThreshold && !it.isPopped }) {
            edgeGlowAlpha = min(1f, edgeGlowAlpha + 0.02f * edgeGlowDirection)
            if (edgeGlowAlpha >= 1f) edgeGlowDirection = -1f
            if (edgeGlowAlpha <= 0.3f) edgeGlowDirection = 1f
            
            val glowColor = Color.argb((edgeGlowAlpha * 60).toInt(), 218, 165, 32)
            val gradientSize = 50f
            
            // Top edge
            paint.shader = LinearGradient(
                0f, 0f, 0f, gradientSize,
                glowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), gradientSize, paint)
            
            // Bottom edge
            paint.shader = LinearGradient(
                0f, screenHeight.toFloat(), 0f, screenHeight - gradientSize,
                glowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, screenHeight - gradientSize, screenWidth.toFloat(), screenHeight.toFloat(), paint)
            
            // Left edge
            paint.shader = LinearGradient(
                0f, 0f, gradientSize, 0f,
                glowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, gradientSize, screenHeight.toFloat(), paint)
            
            // Right edge
            paint.shader = LinearGradient(
                screenWidth.toFloat(), 0f, screenWidth - gradientSize, 0f,
                glowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawRect(screenWidth - gradientSize, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paint)
            
            paint.shader = null
        } else {
            edgeGlowAlpha = 0f
        }
    }
    
    private fun updateBubble(bubble: Bubble) {
        bubble.radius += GROWTH_RATE
        
        bubble.wobblePhase += bubble.wobbleSpeed
        val wobbleOffset = sin(bubble.wobblePhase * PI / 180f).toFloat() * bubble.wobbleAmplitude
        
        bubble.x += bubble.velocityX + wobbleOffset * 0.2f
        bubble.y += bubble.velocityY
        
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
        
        bubble.velocityY += 0.01f
        bubble.glowPhase += 2f
    }
    
    private fun drawBubble(canvas: Canvas, bubble: Bubble) {
        val isGlowing = bubble.radius >= glowThreshold
        
        if (isGlowing) {
            val breathe = (sin(bubble.glowPhase * PI / 180f).toFloat() + 1f) / 2f
            val glowRadius = bubble.radius * (1.3f + breathe * 0.3f)
            val glowAlpha = (60 + breathe * 40).toInt()
            
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
        
        val gradient = RadialGradient(
            bubble.x - bubble.radius * 0.3f,
            bubble.y - bubble.radius * 0.3f,
            bubble.radius,
            lightenColor(bubble.color, 60),
            bubble.color,
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        paint.alpha = (bubble.alpha * 200).toInt()
        
        canvas.drawCircle(bubble.x, bubble.y, bubble.radius, paint)
        
        highlightPaint.color = Color.WHITE
        highlightPaint.alpha = if (isGlowing) 80 else 50
        canvas.drawCircle(
            bubble.x - bubble.radius * 0.25f,
            bubble.y - bubble.radius * 0.25f,
            bubble.radius * 0.3f,
            highlightPaint
        )
        
        if (isGlowing) {
            val breathe = (sin(bubble.glowPhase * PI / 180f).toFloat() + 1f) / 2f
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f + breathe * 2f
            paint.alpha = (80 + breathe * 60).toInt()
            paint.color = lightenColor(bubble.color, 100)
            canvas.drawCircle(bubble.x, bubble.y, bubble.radius + 4f, paint)
            paint.style = Paint.Style.FILL
            paint.shader = null
        }
    }
    
    private fun drawPressedBubble(canvas: Canvas, bubble: Bubble) {
        // Bubble color glow at edges
        val breathe = (sin(System.currentTimeMillis() / 200.0).toFloat() + 1f) / 2f
        val glowRadius = bubble.radius * (1.5f + breathe * 0.4f)
        val glowAlpha = (100 + breathe * 80).toInt()
        
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
        
        // Pulsing ring
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.alpha = (150 + breathe * 80).toInt()
        paint.color = bubble.color
        canvas.drawCircle(bubble.x, bubble.y, bubble.radius + 8f + breathe * 5f, paint)
        
        // Inner glow
        val innerGrad = RadialGradient(
            bubble.x, bubble.y, bubble.radius,
            lightenColor(bubble.color, 80),
            bubble.color,
            Shader.TileMode.CLAMP
        )
        paint.shader = innerGrad
        paint.alpha = 220
        paint.style = Paint.Style.FILL
        canvas.drawCircle(bubble.x, bubble.y, bubble.radius, paint)
        paint.shader = null
        
        // Highlight
        highlightPaint.color = Color.WHITE
        highlightPaint.alpha = 100
        canvas.drawCircle(
            bubble.x - bubble.radius * 0.2f,
            bubble.y - bubble.radius * 0.2f,
            bubble.radius * 0.25f,
            highlightPaint
        )
        
        paint.style = Paint.Style.FILL
    }
    
    private fun drawPoppingBubble(canvas: Canvas, bubble: Bubble) {
        val progress = bubble.popProgress
        val expandedRadius = bubble.radius * (1 + progress * 1.5f)
        val alpha = (200 * (1 - progress)).toInt()
        
        paint.color = bubble.color
        paint.alpha = alpha
        canvas.drawCircle(bubble.x, bubble.y, expandedRadius, paint)
        
        val particleCount = if (bubble.radius >= glowThreshold) 16 else 8
        for (i in 0 until particleCount) {
            val angle = i * (360f / particleCount) + progress * 180f
            val rad = angle * PI / 180f
            val px = bubble.x + cos(rad).toFloat() * expandedRadius * 1.4f
            val py = bubble.y + sin(rad).toFloat() * expandedRadius * 1.4f
            val particleSize = bubble.radius * 0.15f * (1 - progress)
            
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
                    handleTouchDown(event.getX(event.actionIndex), event.getY(event.actionIndex))
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (isPressing && pressedBubble != null) {
                    handleTouchUp()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (isPressing && pressedBubble != null) {
                    isPressing = false
                    pressedBubble = null
                    pressProgress = 0f
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun handleTouchDown(x: Float, y: Float) {
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
        
        if (closestBubble != null) {
            pressedBubble = closestBubble
            isPressing = true
            pressProgress = 0f
            pressGlowAlpha = 0f
            
            // Light vibration feedback on press
            if (settingsManager.vibrationEnabled) {
                vibrate(15)
            }
        }
    }
    
    private fun handleTouchUp() {
        val bubble = pressedBubble
        if (bubble != null && !bubble.isPopped) {
            popBubble(bubble)
        }
        isPressing = false
        pressedBubble = null
        pressProgress = 0f
        pressGlowAlpha = 0f
    }
    
    private fun popBubble(bubble: Bubble) {
        bubble.isPopped = true
        bubble.popProgress = 0f
        
        val isBig = bubble.radius >= glowThreshold
        val scorePoints = calculateScore(bubble)
        score += scorePoints
        totalScore += scorePoints
        settingsManager.totalScore = totalScore
        onScoreChanged?.invoke(score)
        onTotalScoreChanged?.invoke(totalScore)
        
        if (settingsManager.soundEnabled) {
            soundManager.playPop(isBig)
        }
        
        if (settingsManager.vibrationEnabled) {
            val duration = if (isBig) 80 else 30
            vibrate(duration)
        }
        
        spawnPopParticles(bubble)
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
    
    private var lastMilestoneScore = 0 // 上次触发特效的分数
    
    private fun checkMilestones() {
        when {
            // 100分后每加100分触发终极特效
            score >= 100 && (score - lastMilestoneScore) >= 100 -> {
                lastMilestoneScore = score / 100 * 100
                celebrationMode = 3
                triggerCelebration()
            }
            // 50分触发金光特效
            score >= 50 && lastMilestoneScore < 50 -> {
                lastMilestoneScore = 50
                celebrationMode = 2
                triggerCelebration()
            }
            // 30分触发礼花特效
            score >= 30 && lastMilestoneScore < 30 -> {
                lastMilestoneScore = 30
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
        
        postDelayed({
            isPaused = false
            if (celebrationMode >= 3) {
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
        
        if (celebrationMode >= 2) {
            val breathe = (sin(celebrationProgress * PI * 4).toFloat() + 1f) / 2f
            val alpha = (30 + breathe * 40).toInt()
            paint.color = Color.argb(alpha, 255, 215, 0)
            canvas.drawColor(paint.color)
        }
        
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
            p.vy += 0.15f
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
        lastMilestoneScore = 0
        isGameOver = false
        isPaused = false
        celebrationMode = 0
        celebrationProgress = 0f
        edgeGlowAlpha = 0f
        isPressing = false
        pressedBubble = null
        pressProgress = 0f
        onScoreChanged?.invoke(0)
        
        repeat(MIN_BUBBLES) {
            bubbles.add(Bubble.createRandom(screenWidth, screenHeight))
        }
    }
    
    fun getScore(): Int = score
    fun getTotalScore(): Int = totalScore
    
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
    
    data class BaroqueOrnament(
        var x: Float,
        var y: Float,
        var size: Float,
        var rotation: Float,
        var alpha: Float
    )
}
