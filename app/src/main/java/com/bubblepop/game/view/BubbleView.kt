package com.bubblepop.game.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.LinearGradient
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.bubblepop.game.manager.SettingsManager
import com.bubblepop.game.manager.SoundManager
import com.bubblepop.game.model.Bubble
import com.bubblepop.game.model.BubbleShape
import com.bubblepop.game.model.ExplosionType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
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
    private val bubblePath = Path()
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val neonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val celebrationParticlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val blindBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val celebrationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
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
    private var totalScore = 0
    private var isGameOver = false
    private var isPaused = false
    private var lastSpawnTime = 0L
    private var totalScoreLoaded = false
    private var lastMilestoneScore = 0
    
    // 气泡排序缓存，优化性能
    private var sortedBubblesCache: List<Bubble>? = null
    private var needsResort = true
    
    // Press interaction state
    private var pressedBubble: Bubble? = null
    private var isPressing = false
    private var pressProgress = 0f
    
    // Effects state
    var debugMode = false
    private var celebrationMode = 0
    private var celebrationProgress = 0f
    private var celebrationAnimator: ValueAnimator? = null
    private var celebrationText = ""
    private val particles = mutableListOf<Particle>()
    private val fireworks = mutableListOf<Firework>()
    private val goldenRays = mutableListOf<GoldenRay>()
    private val shockwaves = mutableListOf<Shockwave>()
    private val neonTextEffects = mutableListOf<NeonTextEffect>()
    private val celebrationParticles = mutableListOf<CelebrationParticle>()
    private val confettiParticles = mutableListOf<ConfettiParticle>()
    private val spiralParticles = mutableListOf<SpiralParticle>()
    private val goldenRain = mutableListOf<GoldenRainDrop>()
    private val ringExplosions = mutableListOf<RingExplosion>()
    private var screenShakeX = 0f
    private var screenShakeY = 0f
    private var screenFlashAlpha = 0f
    private var screenFlashColor = Color.WHITE
    private var edgeGlowAlpha = 0f
    private var edgeGlowDirection = 1f
    
    // Baroque decorative elements
    private val baroqueOrnaments = mutableListOf<BaroqueOrnament>()
    private val floatingGoldParticles = mutableListOf<FloatingGoldParticle>()
    
    // Physics constants
    private val BOUNCE_DAMPING = 0.75f
    private val COLLISION_DAMPING = 0.85f
    private val GRAVITY = 0.008f
    private val FRICTION = 0.998f
    
    // Callbacks
    var onScoreChanged: ((Int) -> Unit)? = null
    var onTotalScoreChanged: ((Int) -> Unit)? = null
    var onGameOver: (() -> Unit)? = null
    var onCelebration: ((Int) -> Unit)? = null
    
    companion object {
        private const val TAG = "BubbleView"
        private const val MAX_BUBBLES = 30
        private const val MIN_BUBBLES = 8
        private const val GROWTH_RATE = 0.12f
        private const val GLOW_THRESHOLD = 0.33f
    }
    
    init {
        highlightPaint.style = Paint.Style.FILL
        glowPaint.style = Paint.Style.FILL
        particlePaint.style = Paint.Style.FILL
        baroquePaint.style = Paint.Style.STROKE
        baroquePaint.strokeWidth = 2f
        baroquePaint.color = Color.argb(60, 218, 165, 32)
        
        initBaroqueOrnaments()
        initFloatingGoldParticles()
    }
    
    private val maxRadius: Float
        get() = screenWidth * GLOW_THRESHOLD
    
    private fun initBaroqueOrnaments() {
        repeat(16) {
            baroqueOrnaments.add(BaroqueOrnament(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 25f + 12f,
                rotation = Random.nextFloat() * 360f,
                alpha = Random.nextFloat() * 0.35f + 0.1f,
                type = Random.nextInt(3)
            ))
        }
    }
    
    private fun initFloatingGoldParticles() {
        repeat(20) {
            floatingGoldParticles.add(FloatingGoldParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = Random.nextFloat() * 0.0003f + 0.0001f,
                size = Random.nextFloat() * 3f + 1f,
                alpha = Random.nextFloat() * 0.4f + 0.1f,
                phase = Random.nextFloat() * 360f
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
        
        if (bubbles.isEmpty() && !isGameOver && !debugMode) {
            repeat(MIN_BUBBLES) {
                bubbles.add(Bubble.createRandom(w, h, 20f, maxRadius * 0.5f))
            }
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        canvas.save()
        canvas.translate(screenShakeX, screenShakeY)
        
        try {
            drawBackground(canvas)
            drawFloatingGoldParticles(canvas)
        } catch (e: Exception) {
            Log.e(TAG, "绘制背景/装饰时异常", e)
        }
        
        if (!isGameOver && !debugMode) {
            try {
                bubbles.removeAll { it.isPopped && it.popProgress >= 1f }
                needsResort = true
                
                val shouldPauseGrowth = (isPressing && pressedBubble != null) || isPaused
                
                if (needsResort) {
                    sortedBubblesCache = bubbles.sortedBy { it.radius }
                    needsResort = false
                }
                
                for (bubble in sortedBubblesCache ?: bubbles) {
                    if (bubble.isPopped) {
                        bubble.popProgress += 0.03f
                        drawPoppingBubble(canvas, bubble)
                    } else if (bubble == pressedBubble && isPressing) {
                        drawPressedBubble(canvas, bubble)
                    } else {
                        if (!shouldPauseGrowth) {
                            updateBubble(bubble)
                        }
                        drawBubble(canvas, bubble)
                    }
                }
                
                if (isPressing && pressedBubble != null) {
                    pressProgress = min(1f, pressProgress + 0.025f)
                }
                
                if (!isPaused) {
                    handleBubbleCollisions()
                    
                    val now = System.currentTimeMillis()
                    if (bubbles.count { !it.isPopped } < MIN_BUBBLES && now - lastSpawnTime > 500) {
                        bubbles.add(Bubble.createRandom(screenWidth, screenHeight, 20f, maxRadius * 0.5f))
                        needsResort = true
                        lastSpawnTime = now
                    }
                    
                    val activeBubbles = bubbles.filter { !it.isPopped }
                    if (activeBubbles.size >= MAX_BUBBLES && 
                        activeBubbles.all { it.radius >= maxRadius * 0.8f }) {
                        triggerGameOver()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "绘制气泡时异常", e)
            }
        }
        
        try {
            drawRingExplosions(canvas)
        } catch (e: Exception) {
            Log.e(TAG, "drawRingExplosions 异常", e)
        }
        
        try {
            drawSpiralParticles(canvas)
        } catch (e: Exception) {
            Log.e(TAG, "drawSpiralParticles 异常", e)
        }
        
        try {
            drawCelebrationParticles(canvas)
        } catch (e: Exception) {
            Log.e(TAG, "drawCelebrationParticles 异常", e)
        }
        
        try {
            drawConfetti(canvas)
        } catch (e: Exception) {
            Log.e(TAG, "drawConfetti 异常", e)
        }
        
        try {
            drawGoldenRain(canvas)
        } catch (e: Exception) {
            Log.e(TAG, "drawGoldenRain 异常", e)
        }
        
        try {
            drawShockwaves(canvas)
        } catch (e: Exception) {
            Log.e(TAG, "drawShockwaves 异常", e)
        }
        
        try {
            drawCelebration(canvas)
        } catch (e: Exception) {
            Log.e(TAG, "drawCelebration 异常", e)
        }
        
        try {
            drawParticles(canvas)
        } catch (e: Exception) {
            Log.e(TAG, "drawParticles 异常", e)
        }
        
        try {
            drawNeonTextEffects(canvas)
        } catch (e: Exception) {
            Log.e(TAG, "drawNeonTextEffects 异常", e)
        }
        
        try {
            drawScreenFlash(canvas)
        } catch (e: Exception) {
            Log.e(TAG, "drawScreenFlash 异常", e)
        }
        
        canvas.restore()
        
        invalidate()
    }
    
    private fun drawBackground(canvas: Canvas) {
        val bg = when (settingsManager.background) {
            "warm" -> Color.parseColor("#1A1A2E")      // 深蓝黑
            "lavender" -> Color.parseColor("#16213E")   // 午夜蓝
            "sage" -> Color.parseColor("#0F3460")       // 深海蓝
            else -> Color.parseColor("#1B1B2F")         // 暗黑紫
        }
        canvas.drawColor(bg)
    }
    
    private fun drawBaroqueDecorations(canvas: Canvas) {
        val borderWidth = 6f
        val neonColor = Color.argb(180, 255, 255, 255)
        
        paint.color = neonColor
        paint.strokeWidth = borderWidth
        paint.style = Paint.Style.STROKE
        
        canvas.drawRect(
            borderWidth / 2, borderWidth / 2,
            screenWidth - borderWidth / 2, screenHeight - borderWidth / 2,
            paint
        )
        
        paint.strokeWidth = 2f
        paint.alpha = 80
        canvas.drawRect(
            borderWidth + 4, borderWidth + 4,
            screenWidth - borderWidth - 4, screenHeight - borderWidth - 4,
            paint
        )
        
        paint.strokeWidth = 1.5f
        paint.alpha = 40
        canvas.drawRect(
            borderWidth + 14, borderWidth + 14,
            screenWidth - borderWidth - 14, screenHeight - borderWidth - 14,
            paint
        )
        
        drawCornerOrnament(canvas, borderWidth + 12, borderWidth + 12, 1f, 1f)
        drawCornerOrnament(canvas, screenWidth - borderWidth - 12, borderWidth + 12, -1f, 1f)
        drawCornerOrnament(canvas, borderWidth + 12, screenHeight - borderWidth - 12, 1f, -1f)
        drawCornerOrnament(canvas, screenWidth - borderWidth - 12, screenHeight - borderWidth - 12, -1f, -1f)
        
        for (ornament in baroqueOrnaments) {
            val ox = ornament.x * screenWidth
            val oy = ornament.y * screenHeight
            when (ornament.type) {
                0 -> drawSmallSwirl(canvas, ox, oy, ornament.size, ornament.rotation, ornament.alpha)
                1 -> drawFleurDeLis(canvas, ox, oy, ornament.size, ornament.rotation, ornament.alpha)
                2 -> drawScrollWork(canvas, ox, oy, ornament.size, ornament.rotation, ornament.alpha)
            }
        }
        
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }
    
    private fun drawCornerOrnament(canvas: Canvas, x: Float, y: Float, scaleX: Float, scaleY: Float) {
        paint.color = Color.argb(150, 255, 255, 255)
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(scaleX, scaleY)
        
        val path = Path()
        path.moveTo(0f, 0f)
        path.cubicTo(20f, 0f, 40f, 12f, 40f, 30f)
        path.cubicTo(40f, 48f, 20f, 55f, 0f, 55f)
        canvas.drawPath(path, paint)
        
        path.reset()
        path.moveTo(5f, 5f)
        path.cubicTo(18f, 5f, 30f, 15f, 30f, 28f)
        canvas.drawPath(path, paint)
        
        paint.style = Paint.Style.FILL
        canvas.drawCircle(40f, 30f, 4f, paint)
        canvas.drawCircle(0f, 55f, 4f, paint)
        canvas.drawCircle(20f, 15f, 2.5f, paint)
        
        canvas.restore()
        paint.style = Paint.Style.FILL
    }
    
    private fun drawSmallSwirl(canvas: Canvas, x: Float, y: Float, size: Float, rotation: Float, alpha: Float) {
        paint.color = Color.argb((alpha * 70).toInt(), 184, 134, 11)
        paint.strokeWidth = 1.5f
        paint.style = Paint.Style.STROKE
        
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(rotation)
        
        val path = Path()
        path.moveTo(0f, 0f)
        path.cubicTo(size / 2, -size / 3, size, 0f, size / 2, size / 2)
        path.cubicTo(0f, size, -size / 3, size / 2, 0f, 0f)
        canvas.drawPath(path, paint)
        
        canvas.restore()
        paint.style = Paint.Style.FILL
    }
    
    private fun drawFleurDeLis(canvas: Canvas, x: Float, y: Float, size: Float, rotation: Float, alpha: Float) {
        paint.color = Color.argb((alpha * 50).toInt(), 218, 165, 32)
        paint.strokeWidth = 1.5f
        paint.style = Paint.Style.STROKE
        
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(rotation)
        
        val path = Path()
        path.moveTo(0f, -size)
        path.cubicTo(size * 0.3f, -size * 0.5f, size * 0.5f, 0f, 0f, size * 0.3f)
        path.cubicTo(-size * 0.5f, 0f, -size * 0.3f, -size * 0.5f, 0f, -size)
        canvas.drawPath(path, paint)
        
        path.reset()
        path.moveTo(-size * 0.6f, 0f)
        path.cubicTo(-size * 0.3f, -size * 0.2f, 0f, -size * 0.1f, 0f, size * 0.3f)
        path.cubicTo(0f, size * 0.5f, -size * 0.4f, size * 0.4f, -size * 0.6f, 0f)
        canvas.drawPath(path, paint)
        
        path.reset()
        path.moveTo(size * 0.6f, 0f)
        path.cubicTo(size * 0.3f, -size * 0.2f, 0f, -size * 0.1f, 0f, size * 0.3f)
        path.cubicTo(0f, size * 0.5f, size * 0.4f, size * 0.4f, size * 0.6f, 0f)
        canvas.drawPath(path, paint)
        
        canvas.restore()
        paint.style = Paint.Style.FILL
    }
    
    private fun drawScrollWork(canvas: Canvas, x: Float, y: Float, size: Float, rotation: Float, alpha: Float) {
        paint.color = Color.argb((alpha * 50).toInt(), 184, 134, 11)
        paint.strokeWidth = 1.5f
        paint.style = Paint.Style.STROKE
        
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(rotation)
        
        val path = Path()
        path.moveTo(-size, 0f)
        path.cubicTo(-size * 0.5f, -size * 0.8f, size * 0.5f, -size * 0.8f, size, 0f)
        path.cubicTo(size * 0.5f, size * 0.8f, -size * 0.5f, size * 0.8f, -size, 0f)
        canvas.drawPath(path, paint)
        
        path.reset()
        path.moveTo(0f, -size * 0.5f)
        path.cubicTo(size * 0.3f, 0f, size * 0.3f, size * 0.5f, 0f, size * 0.5f)
        canvas.drawPath(path, paint)
        
        canvas.restore()
        paint.style = Paint.Style.FILL
    }
    
    private fun drawFloatingGoldParticles(canvas: Canvas) {
        val time = System.currentTimeMillis()
        for (p in floatingGoldParticles) {
            p.phase += 0.5f
            val alpha = (sin(p.phase * PI / 180f).toFloat() + 1f) / 2f * p.alpha
            
            paint.color = Color.argb((alpha * 220).toInt(), 255, 255, 255)
            paint.style = Paint.Style.FILL
            
            val px = p.x * screenWidth + sin(time * p.speed * 0.01f).toFloat() * 10f
            val py = (p.y * screenHeight + cos(time * p.speed * 0.008f).toFloat() * 15f) % screenHeight
            
            canvas.drawCircle(px, py, p.size, paint)
        }
    }
    
    private fun drawEdgeGlow(canvas: Canvas) {
        if (bubbles.any { it.radius >= maxRadius * 0.8f && !it.isPopped }) {
            edgeGlowAlpha = min(1f, edgeGlowAlpha + 0.02f * edgeGlowDirection)
            if (edgeGlowAlpha >= 1f) edgeGlowDirection = -1f
            if (edgeGlowAlpha <= 0.3f) edgeGlowDirection = 1f
            
            val glowColor = Color.argb((edgeGlowAlpha * 120).toInt(), 255, 100, 200)
            val gradientSize = 60f
            
            paint.shader = LinearGradient(0f, 0f, 0f, gradientSize, glowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), gradientSize, paint)
            
            paint.shader = LinearGradient(0f, screenHeight.toFloat(), 0f, screenHeight - gradientSize, glowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, screenHeight - gradientSize, screenWidth.toFloat(), screenHeight.toFloat(), paint)
            
            paint.shader = LinearGradient(0f, 0f, gradientSize, 0f, glowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, gradientSize, screenHeight.toFloat(), paint)
            
            paint.shader = LinearGradient(screenWidth.toFloat(), 0f, screenWidth - gradientSize, 0f, glowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawRect(screenWidth - gradientSize, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paint)
            
            paint.shader = null
        } else {
            edgeGlowAlpha = 0f
        }
    }
    
    private fun updateBubble(bubble: Bubble) {
        bubble.radius = min(bubble.radius + GROWTH_RATE, maxRadius)
        
        bubble.wobblePhase += bubble.wobbleSpeed
        val wobbleOffset = sin(bubble.wobblePhase * PI / 180f).toFloat() * bubble.wobbleAmplitude
        
        bubble.x += bubble.velocityX + wobbleOffset * 0.15f
        bubble.y += bubble.velocityY
        bubble.velocityY += GRAVITY
        
        bubble.velocityX *= FRICTION
        bubble.velocityY *= FRICTION
        
        if (bubble.x - bubble.radius < 0) {
            bubble.x = bubble.radius
            bubble.velocityX = kotlin.math.abs(bubble.velocityX) * BOUNCE_DAMPING
        }
        if (bubble.x + bubble.radius > screenWidth) {
            bubble.x = screenWidth - bubble.radius
            bubble.velocityX = -kotlin.math.abs(bubble.velocityX) * BOUNCE_DAMPING
        }
        if (bubble.y - bubble.radius < 0) {
            bubble.y = bubble.radius
            bubble.velocityY = kotlin.math.abs(bubble.velocityY) * BOUNCE_DAMPING
        }
        if (bubble.y + bubble.radius > screenHeight) {
            bubble.y = screenHeight - bubble.radius
            bubble.velocityY = -kotlin.math.abs(bubble.velocityY) * BOUNCE_DAMPING
        }
        
        bubble.glowPhase += 2f
        bubble.baroqueRotation += 0.3f
        
        // 盲盒球抖动效果
        if (bubble.isBlindBox && !bubble.isPopped) {
            bubble.shakePhase += 5f
        }
    }
    
    private fun handleBubbleCollisions() {
        val active = bubbles.filter { !it.isPopped && it != pressedBubble }
        for (i in active.indices) {
            for (j in i + 1 until active.size) {
                val a = active[i]
                val b = active[j]
                
                val dx = b.x - a.x
                val dy = b.y - a.y
                val dist = sqrt(dx * dx + dy * dy)
                val minDist = a.radius + b.radius
                
                if (dist < minDist && dist > 0) {
                    val nx = dx / dist
                    val ny = dy / dist
                    
                    val overlap = minDist - dist
                    a.x -= nx * overlap / 2f
                    a.y -= ny * overlap / 2f
                    b.x += nx * overlap / 2f
                    b.y += ny * overlap / 2f
                    
                    val dvx = a.velocityX - b.velocityX
                    val dvy = a.velocityY - b.velocityY
                    val dvDotN = dvx * nx + dvy * ny
                    
                    if (dvDotN > 0) {
                        val impulse = dvDotN * COLLISION_DAMPING
                        a.velocityX -= impulse * nx
                        a.velocityY -= impulse * ny
                        b.velocityX += impulse * nx
                        b.velocityY += impulse * ny
                    }
                }
            }
        }
    }
    
    // ====== 绘制小球（含巴洛克内部装饰） ======
    private fun drawBubble(canvas: Canvas, bubble: Bubble) {
        val isGlowing = bubble.radius >= maxRadius * 0.8f
        
        // 盲盒球抖动偏移
        var drawX = bubble.x
        var drawY = bubble.y
        if (bubble.isBlindBox && !bubble.isPopped) {
            val shakeX = sin(bubble.shakePhase * PI / 180f).toFloat() * 3f
            val shakeY = cos(bubble.shakePhase * 1.3f * PI / 180f).toFloat() * 2f
            drawX += shakeX
            drawY += shakeY
        }
        
        // 属性光晕 - 柔和效果
        val breathe = (sin(bubble.glowPhase * PI / 180f).toFloat() + 1f) / 2f
        val attrGlowRadius = bubble.radius * (1.2f + breathe * 0.15f)
        val attrGlowAlpha = if (bubble.isHiddenRare) (60 + breathe * 30).toInt() else (35 + breathe * 15).toInt()
        val attrColor = bubble.explosionType.primaryColor
        val attrGlow = RadialGradient(drawX, drawY, attrGlowRadius, intArrayOf(attrColor, Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        glowPaint.shader = attrGlow
        glowPaint.alpha = attrGlowAlpha
        canvas.drawCircle(drawX, drawY, attrGlowRadius, glowPaint)
        glowPaint.shader = null
        
        // 外层光晕 - 柔和
        val outerGlowRadius = bubble.radius * (1.15f + breathe * 0.1f)
        val outerGlowAlpha = (40 + breathe * 20).toInt()
        val outerGlow = RadialGradient(drawX, drawY, outerGlowRadius, intArrayOf(bubble.color, Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        glowPaint.shader = outerGlow
        glowPaint.alpha = outerGlowAlpha
        canvas.drawCircle(drawX, drawY, outerGlowRadius, glowPaint)
        glowPaint.shader = null
        
        // 发光球额外加强 - 柔和
        if (isGlowing) {
            val glowRadius = bubble.radius * (1.3f + breathe * 0.15f)
            val glowAlpha = (50 + breathe * 25).toInt()
            val glowGradient = RadialGradient(drawX, drawY, glowRadius, intArrayOf(lightenColor(bubble.color, 30), Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            glowPaint.shader = glowGradient
            glowPaint.alpha = glowAlpha
            canvas.drawCircle(drawX, drawY, glowRadius, glowPaint)
            glowPaint.shader = null
        }
        
        // 主体渐变 - 高饱和度霓虹感
        val gradient = RadialGradient(
            drawX - bubble.radius * 0.3f, drawY - bubble.radius * 0.3f,
            bubble.radius * 1.1f,
            intArrayOf(Color.WHITE, lightenColor(bubble.color, 40), bubble.color),
            floatArrayOf(0f, 0.3f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        paint.alpha = (bubble.alpha * 240).toInt()
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        
        when (bubble.shape) {
            BubbleShape.CIRCLE -> canvas.drawCircle(drawX, drawY, bubble.radius, paint)
            BubbleShape.ELLIPSE -> {
                canvas.save()
                canvas.translate(drawX - bubble.x, drawY - bubble.y)
                drawEllipse(canvas, bubble)
                canvas.restore()
            }
        }
        
        // 高光 - 逼真感
        highlightPaint.color = Color.WHITE
        highlightPaint.alpha = if (isGlowing) 100 else 70
        canvas.drawOval(
            drawX - bubble.radius * 0.35f,
            drawY - bubble.radius * 0.4f,
            drawX - bubble.radius * 0.05f,
            drawY - bubble.radius * 0.1f,
            highlightPaint
        )
        
        // 底部反光
        highlightPaint.alpha = 30
        canvas.drawOval(
            drawX - bubble.radius * 0.2f,
            drawY + bubble.radius * 0.15f,
            drawX + bubble.radius * 0.2f,
            drawY + bubble.radius * 0.35f,
            highlightPaint
        )
        
        // 盲盒球标记 - 问号
        if (bubble.isBlindBox && !bubble.isPopped) {
            val qAlpha = (150 + breathe * 80).toInt()
            neonTextPaint.color = Color.argb(qAlpha, 255, 255, 255)
            neonTextPaint.textSize = bubble.radius * 0.9f
            canvas.drawText("?", drawX, drawY + bubble.radius * 0.3f, neonTextPaint)
        }
        
        paint.shader = null
    }
    
    // 巴洛克装饰画在小球内部
    private fun drawBaroqueOnBubble(canvas: Canvas, bubble: Bubble) {
        val r = bubble.radius
        if (r < 25f) return // 太小的球不画装饰
        
        canvas.save()
        canvas.translate(bubble.x, bubble.y)
        canvas.rotate(bubble.baroqueRotation)
        
        val alpha = min(180, (r / maxRadius * 180).toInt())
        baroquePaint.color = Color.argb(alpha, 255, 255, 255)
        baroquePaint.strokeWidth = max(1.5f, r * 0.05f)
        baroquePaint.style = Paint.Style.STROKE
        
        when (bubble.baroquePattern) {
            0 -> {
                // 鸢尾花纹
                val path = Path()
                path.moveTo(0f, -r * 0.5f)
                path.cubicTo(r * 0.2f, -r * 0.2f, r * 0.3f, 0f, 0f, r * 0.2f)
                path.cubicTo(-r * 0.3f, 0f, -r * 0.2f, -r * 0.2f, 0f, -r * 0.5f)
                canvas.drawPath(path, baroquePaint)
                
                path.reset()
                path.moveTo(-r * 0.35f, 0f)
                path.cubicTo(-r * 0.15f, -r * 0.1f, 0f, -r * 0.05f, 0f, r * 0.15f)
                canvas.drawPath(path, baroquePaint)
                
                path.reset()
                path.moveTo(r * 0.35f, 0f)
                path.cubicTo(r * 0.15f, -r * 0.1f, 0f, -r * 0.05f, 0f, r * 0.15f)
                canvas.drawPath(path, baroquePaint)
            }
            1 -> {
                // 漩涡纹
                val path = Path()
                path.moveTo(0f, 0f)
                for (i in 1..36) {
                    val angle = i * 10f * PI / 180f
                    val dist = i * r * 0.012f
                    path.lineTo(cos(angle).toFloat() * dist, sin(angle).toFloat() * dist)
                }
                canvas.drawPath(path, baroquePaint)
            }
            2 -> {
                // 皇冠纹
                val path = Path()
                val pts = 5
                for (i in 0 until pts * 2) {
                    val angle = (i * 360f / (pts * 2) - 90f) * PI / 180f
                    val dist = if (i % 2 == 0) r * 0.45f else r * 0.25f
                    val px = cos(angle).toFloat() * dist
                    val py = sin(angle).toFloat() * dist
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, baroquePaint)
            }
            3 -> {
                // 藤蔓纹
                val path = Path()
                path.moveTo(-r * 0.4f, r * 0.3f)
                path.cubicTo(-r * 0.2f, -r * 0.3f, r * 0.2f, -r * 0.3f, r * 0.4f, r * 0.3f)
                canvas.drawPath(path, baroquePaint)
                
                path.reset()
                path.moveTo(-r * 0.3f, 0f)
                path.cubicTo(-r * 0.1f, r * 0.2f, r * 0.1f, r * 0.2f, r * 0.3f, 0f)
                canvas.drawPath(path, baroquePaint)
                
                baroquePaint.style = Paint.Style.FILL
                canvas.drawCircle(0f, -r * 0.15f, r * 0.04f, baroquePaint)
                canvas.drawCircle(-r * 0.25f, r * 0.15f, r * 0.03f, baroquePaint)
                canvas.drawCircle(r * 0.25f, r * 0.15f, r * 0.03f, baroquePaint)
            }
        }
        
        canvas.restore()
        baroquePaint.style = Paint.Style.STROKE
    }
    
    private fun drawEllipse(canvas: Canvas, bubble: Bubble) {
        canvas.save()
        canvas.translate(bubble.x, bubble.y)
        canvas.rotate(bubble.ellipseAngle)
        canvas.scale(1f, bubble.ellipseRatio)
        canvas.drawCircle(0f, 0f, bubble.radius, paint)
        canvas.restore()
    }
    
    // ====== 炫酷按压效果 ======
    private fun drawPressedBubble(canvas: Canvas, bubble: Bubble) {
        val breathe = (sin(System.currentTimeMillis() / 150.0).toFloat() + 1f) / 2f
        val p = pressProgress
        
        // 辉光呼吸效果
        val glowRadius = bubble.radius * (2f + breathe * 0.8f + p * 0.5f)
        val glowAlpha = (100 + breathe * 80 + p * 100).toInt()
        val glowGradient = RadialGradient(
            bubble.x, bubble.y, glowRadius,
            intArrayOf(lightenColor(bubble.color, 120), bubble.color, Color.TRANSPARENT),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP
        )
        glowPaint.shader = glowGradient
        glowPaint.alpha = glowAlpha
        canvas.drawCircle(bubble.x, bubble.y, glowRadius, glowPaint)
        glowPaint.shader = null
        
        // 第二层呼吸辉光
        val glow2Radius = bubble.radius * (1.5f + breathe * 0.6f + p * 0.3f)
        val glow2Alpha = (80 + breathe * 60 + p * 80).toInt()
        val glow2Gradient = RadialGradient(
            bubble.x, bubble.y, glow2Radius,
            intArrayOf(Color.WHITE, lightenColor(bubble.color, 60), Color.TRANSPARENT),
            floatArrayOf(0f, 0.3f, 1f),
            Shader.TileMode.CLAMP
        )
        glowPaint.shader = glow2Gradient
        glowPaint.alpha = glow2Alpha
        canvas.drawCircle(bubble.x, bubble.y, glow2Radius, glowPaint)
        glowPaint.shader = null
        
        // 等比缩小 + 抖动效果
        val shrinkFactor = 1f - p * 0.3f
        val shakeX = if (p > 0.2f) sin(System.currentTimeMillis() / 30.0).toFloat() * p * 3f else 0f
        val shakeY = if (p > 0.2f) cos(System.currentTimeMillis() / 25.0).toFloat() * p * 3f else 0f
        
        canvas.save()
        canvas.translate(bubble.x + shakeX, bubble.y + shakeY)
        canvas.scale(shrinkFactor, shrinkFactor)
        
        val innerGrad = RadialGradient(
            0f, -bubble.radius * 0.3f, bubble.radius,
            intArrayOf(lightenColor(bubble.color, 100), bubble.color),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = innerGrad
        paint.alpha = 240
        paint.style = Paint.Style.FILL
        canvas.drawCircle(0f, 0f, bubble.radius, paint)
        paint.shader = null
        
        canvas.restore()
        
        // 高光
        highlightPaint.color = Color.WHITE
        highlightPaint.alpha = 120
        canvas.drawOval(
            bubble.x + shakeX - bubble.radius * 0.3f * shrinkFactor,
            bubble.y + shakeY - bubble.radius * 0.45f * shrinkFactor,
            bubble.x + shakeX,
            bubble.y + shakeY - bubble.radius * 0.15f * shrinkFactor,
            highlightPaint
        )
        
        // 分数提示
        if (p > 0.3f) {
            val scoreAlpha = ((p - 0.3f) / 0.7f * 200).toInt()
            paint.style = Paint.Style.FILL
            paint.alpha = scoreAlpha
            paint.color = Color.WHITE
            paint.textSize = bubble.radius * 0.5f
            paint.textAlign = Paint.Align.CENTER
            val scoreText = "+${calculateScore(bubble)}"
            canvas.drawText(scoreText, bubble.x, bubble.y - bubble.radius * 1.5f - p * 20f, paint)
        }
        
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }
    
    // ====== 夸张爆炸效果 ======
    private fun drawPoppingBubble(canvas: Canvas, bubble: Bubble) {
        val progress = bubble.popProgress
        val expandedRadius = bubble.radius * (1 + progress * 2.5f)
        val alpha = (220 * (1 - progress * progress)).toInt()
        
        // 主爆炸体
        val gradient = RadialGradient(
            bubble.x, bubble.y, expandedRadius,
            intArrayOf(lightenColor(bubble.color, 120), bubble.color, Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        paint.alpha = alpha
        paint.style = Paint.Style.FILL
        canvas.drawCircle(bubble.x, bubble.y, expandedRadius, paint)
        paint.shader = null
        
        // 多层碎片粒子
        val particleCount = if (bubble.radius >= maxRadius * 0.8f) 24 else 16
        for (i in 0 until particleCount) {
            val angle = i * (360f / particleCount) + progress * 270f
            val rad = angle * PI / 180f
            val dist = expandedRadius * (1.2f + progress * 0.8f)
            val px = bubble.x + cos(rad).toFloat() * dist
            val py = bubble.y + sin(rad).toFloat() * dist
            val particleSize = bubble.radius * 0.12f * (1 - progress * progress)
            
            paint.alpha = alpha
            paint.style = Paint.Style.FILL
            paint.color = if (i % 3 == 0) Color.WHITE else bubble.color
            canvas.drawCircle(px, py, particleSize, paint)
        }
        
        // 金色闪光碎片
        if (progress < 0.6f) {
            for (i in 0 until 8) {
                val angle = i * 45f + progress * 180f
                val rad = angle * PI / 180f
                val px = bubble.x + cos(rad).toFloat() * expandedRadius * 1.8f
                val py = bubble.y + sin(rad).toFloat() * expandedRadius * 1.8f
                val sparkleSize = bubble.radius * 0.08f * (1 - progress)
                
                paint.color = Color.argb(alpha, 255, 215, 0)
                canvas.drawCircle(px, py, sparkleSize, paint)
            }
        }
        
        paint.style = Paint.Style.FILL
        paint.alpha = 255
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
        
        Log.d(TAG, "点击坐标: ($x, $y), 找到气泡: ${closestBubble != null}")
        
        if (closestBubble != null) {
            pressedBubble = closestBubble
            isPressing = true
            pressProgress = 0f
            
            // 点击即加分
            val scorePoints = calculateScore(closestBubble)
            if (scorePoints > 0) {
                score += scorePoints
                totalScore += scorePoints
                settingsManager.totalScore = totalScore
                onScoreChanged?.invoke(score)
                onTotalScoreChanged?.invoke(totalScore)
                Log.d(TAG, "得分: +$scorePoints, 当前分数: $score")
            }
            
            if (settingsManager.vibrationEnabled) {
                vibrate(15L)
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
    }
    
    private fun popBubble(bubble: Bubble) {
        try {
            // 盲盒球检查 - 未达到目标大小不爆炸
            if (bubble.isBlindBox && bubble.radius < bubble.blindBoxTargetRadius) {
                // 提示效果：闪烁+震动
                if (settingsManager.vibrationEnabled) vibrate(20L)
                spawnBlindBoxHint(bubble)
                Log.d(TAG, "盲盒球未达到目标大小，提示效果")
                return
            }
            
            Log.d(TAG, "消除气泡: radius=${bubble.radius}, 属性=${bubble.explosionType}, isRare=${bubble.isHiddenRare}")
            
            bubble.isPopped = true
            bubble.popProgress = 0f
            
            val isBig = bubble.radius >= maxRadius * 0.8f
            val isRare = bubble.isHiddenRare
            val hasAttribute = bubble.explosionType != ExplosionType.NONE
            
            Log.d(TAG, "isBig=$isBig, isRare=$isRare, hasAttribute=$hasAttribute")
            
            if (settingsManager.soundEnabled) {
                try {
                    Log.d(TAG, "播放音效: ${bubble.explosionType}")
                    soundManager.playExplosion(bubble.explosionType, isRare)
                    Log.d(TAG, "音效播放完成")
                } catch (e: Exception) {
                    Log.e(TAG, "播放音效时发生异常", e)
                }
            }
            
            if (settingsManager.vibrationEnabled) {
                val duration = if (isRare) 120L else if (isBig) 80L else 30L
                vibrate(duration)
            }
            
            // 只有特殊属性球才触发特效
            if (hasAttribute) {
                try {
                    Log.d(TAG, "开始生成特效: ${bubble.explosionType}")
                    spawnExplosionParticles(bubble)
                    Log.d(TAG, "spawnExplosionParticles 完成")
                } catch (e: Exception) {
                    Log.e(TAG, "spawnExplosionParticles 异常", e)
                }
                try {
                    spawnShockwave(bubble)
                    Log.d(TAG, "spawnShockwave 完成")
                } catch (e: Exception) {
                    Log.e(TAG, "spawnShockwave 异常", e)
                }
                try {
                    spawnNeonTextEffect(bubble)
                    Log.d(TAG, "spawnNeonTextEffect 完成")
                } catch (e: Exception) {
                    Log.e(TAG, "spawnNeonTextEffect 异常", e)
                }
            }
            
            checkMilestones()
        } catch (e: Exception) {
            Log.e(TAG, "消除气泡时发生异常", e)
        }
    }
    
    private fun calculateScore(bubble: Bubble): Int {
        val ratio = bubble.radius / maxRadius
        return when {
            ratio >= 1f -> 3
            ratio >= 0.8f -> 1
            else -> 1 // 点击就有分
        }
    }
    
    private fun checkMilestones() {
        val milestone = score / 10 * 10
        if (milestone > 0 && milestone > lastMilestoneScore) {
            val isFirstMilestone = (milestone == 10)
            lastMilestoneScore = milestone
            
            val mode = when {
                milestone >= 100 -> 4
                milestone >= 70 -> 3
                milestone >= 40 -> 2
                else -> 1
            }
            
            val texts = listOf(
                "不错哦！",
                "继续加油！",
                "太棒了！",
                "厉害！",
                "超神！",
                "无敌！",
                "大神！",
                "传奇！",
                "至尊！",
                "超神了！",
                "太厉害了！"
            )
            
            val textIndex = (milestone / 10 - 1).coerceAtMost(texts.size - 1)
            celebrationMode = mode
            celebrationText = texts[textIndex]
            
            Log.d(TAG, "里程碑: $celebrationText score=$score, milestone=$milestone, isFirst=$isFirstMilestone")
            triggerCelebration(isFirstMilestone)
        }
    }
    
    private fun triggerCelebration(isFirstMilestone: Boolean) {
        Log.d(TAG, "触发庆祝特效: mode=$celebrationMode, text=$celebrationText, screen=${screenWidth}x$screenHeight, isFirst=$isFirstMilestone")
        
        if (screenWidth <= 0 || screenHeight <= 0) {
            Log.e(TAG, "屏幕尺寸未初始化，跳过庆祝特效")
            return
        }
        
        isPaused = true
        celebrationProgress = 0f
        fireworks.clear()
        celebrationParticles.clear()
        confettiParticles.clear()
        spiralParticles.clear()
        goldenRain.clear()
        ringExplosions.clear()
        screenFlashAlpha = 1f
        screenFlashColor = when (celebrationMode) {
            1 -> Color.parseColor("#4FC3F7")
            2 -> Color.parseColor("#FF7043")
            3 -> Color.parseColor("#FF4081")
            4 -> Color.parseColor("#FFD700")
            else -> Color.WHITE
        }
        screenShakeX = 0f
        screenShakeY = 0f
        
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 1500L
        animator.addUpdateListener { anim ->
            celebrationProgress = anim.animatedValue as Float
            
            val shakeDuration = 0.25f
            val shakeIntensity = 18f
            if (celebrationProgress < shakeDuration) {
                val intensity = (1f - celebrationProgress / shakeDuration) * shakeIntensity
                screenShakeX = (Random.nextFloat() - 0.5f) * intensity
                screenShakeY = (Random.nextFloat() - 0.5f) * intensity
            } else {
                screenShakeX *= 0.85f
                screenShakeY *= 0.85f
            }
            
            screenFlashAlpha *= 0.88f
            
            if (Random.nextFloat() > 0.5f) {
                spawnConfettiBurst(isFirstMilestone)
            }
            if (Random.nextFloat() > 0.4f) {
                spawnGoldenRain()
            }
            
            if (!isFirstMilestone) {
                val effectType = Random.nextInt(4)
                when (effectType) {
                    0 -> if (Random.nextFloat() > 0.3f) launchFirework()
                    1 -> if (Random.nextFloat() > 0.2f) spawnCelebrationBurst()
                    2 -> if (Random.nextFloat() > 0.5f) spawnSpiralBurst()
                    3 -> if (Random.nextFloat() > 0.7f) spawnRingExplosion()
                }
            }
        }
        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                isPaused = false
                celebrationMode = 0
                fireworks.clear()
                screenShakeX = 0f
                screenShakeY = 0f
                screenFlashAlpha = 0f
            }
        })
        animator.start()
        celebrationAnimator = animator
        
        // 初始彩带爆发（始终有）
        repeat((100 + celebrationMode * 60) / 3) {
            spawnConfetti(isFirstMilestone, sizeMultiplier = 2.5f)
        }
        
        val goldenCount = 80 + celebrationMode * 50
        val goldenLargeCount = goldenCount / 10
        val goldenNormalCount = goldenCount - goldenLargeCount
        repeat(goldenNormalCount) {
            val x = gaussianRandom(screenWidth * 0.5f, screenWidth * 0.3f)
            spawnGoldenRainDrop(isFirstMilestone, Random.nextFloat() * 1f + 0.5f, x)
        }
        repeat(goldenLargeCount) {
            val x = gaussianRandom(screenWidth * 0.5f, screenWidth * 0.3f)
            spawnGoldenRainDrop(isFirstMilestone, Random.nextFloat() * 0.5f + 1.5f, x)
        }
        
        // 非第一次触发时，随机叠加一种额外特效
        if (!isFirstMilestone) {
            when (Random.nextInt(3)) {
                0 -> repeat(8 + celebrationMode * 4) { launchFirework() }
                1 -> repeat(150 + celebrationMode * 80) { spawnCelebrationParticle() }
                2 -> repeat(3 + celebrationMode) { spawnRingExplosion() }
            }
        }
        
        if (settingsManager.soundEnabled) {
            if (celebrationMode >= 2) soundManager.playFirework()
            if (celebrationMode >= 3) soundManager.playCheer()
        }
    }
    
    private fun spawnCelebrationBurst(sizeMultiplier: Float = 1f) {
        repeat(15 + celebrationMode * 10) {
            spawnCelebrationParticle(sizeMultiplier)
        }
    }
    
    private fun spawnCelebrationParticle(sizeMultiplier: Float = 1f) {
        if (screenWidth <= 0 || screenHeight <= 0) return
        
        val colors = listOf(
            Color.parseColor("#FF1744"),
            Color.parseColor("#FF6D00"),
            Color.parseColor("#FFEA00"),
            Color.parseColor("#76FF03"),
            Color.parseColor("#00E5FF"),
            Color.parseColor("#2979FF"),
            Color.parseColor("#D500F9"),
            Color.parseColor("#FF4081"),
            Color.parseColor("#E040FB"),
            Color.parseColor("#FFD700"),
            Color.parseColor("#FFFFFF")
        )
        
        val textCenterX = screenWidth / 2f
        val textCenterY = screenHeight / 2f
        val offsetX = Random.nextFloat() * 120f + 30f
        val offsetY = Random.nextFloat() * 80f - 60f
        val startX = textCenterX + offsetX
        val startY = textCenterY + offsetY
        
        val angle = Random.nextFloat() * 360f
        val rad = angle * PI / 180f
        val speed = Random.nextFloat() * 20f + 8f
        
        celebrationParticles.add(CelebrationParticle(
            x = startX,
            y = startY,
            vx = cos(rad).toFloat() * speed,
            vy = sin(rad).toFloat() * speed - 5f,
            size = (Random.nextFloat() * 14f + 6f) * sizeMultiplier,
            color = colors.random(),
            life = 1f,
            decay = Random.nextFloat() * 0.01f + 0.006f,
            type = Random.nextInt(3)
        ))
    }
    
    private fun spawnConfettiBurst(isFirstMilestone: Boolean = false, sizeMultiplier: Float = 1f) {
        repeat((20 + celebrationMode * 15) / 3) {
            spawnConfetti(isFirstMilestone, sizeMultiplier * 2.5f)
        }
    }
    
    private fun spawnConfetti(isFirstMilestone: Boolean = false, sizeMultiplier: Float = 1f) {
        if (screenWidth <= 0 || screenHeight <= 0) return
        
        val colors = listOf(
            Color.parseColor("#FF1744"),
            Color.parseColor("#FF6D00"),
            Color.parseColor("#FFEA00"),
            Color.parseColor("#76FF03"),
            Color.parseColor("#00E5FF"),
            Color.parseColor("#2979FF"),
            Color.parseColor("#D500F9"),
            Color.parseColor("#FF4081"),
            Color.parseColor("#E040FB"),
            Color.parseColor("#FFD700")
        )
        
        val decayMultiplier = if (isFirstMilestone) 1.8f else 1f
        
        confettiParticles.add(ConfettiParticle(
            x = Random.nextFloat() * screenWidth,
            y = -20f - Random.nextFloat() * 100f,
            vx = (Random.nextFloat() - 0.5f) * 4f,
            vy = Random.nextFloat() * 3f + 2f,
            width = (Random.nextFloat() * 12f + 6f) * sizeMultiplier,
            height = (Random.nextFloat() * 8f + 4f) * sizeMultiplier,
            color = colors.random(),
            life = 1f,
            decay = (Random.nextFloat() * 0.006f + 0.004f) * decayMultiplier,
            rotation = Random.nextFloat() * 360f,
            rotSpeed = (Random.nextFloat() - 0.5f) * 15f,
            delay = Random.nextFloat() * 0.3f
        ))
    }
    
    private fun spawnSpiralBurst(sizeMultiplier: Float = 1f) {
        repeat(5 + celebrationMode * 3) {
            spawnSpiral(sizeMultiplier)
        }
    }
    
    private fun spawnSpiral(sizeMultiplier: Float = 1f) {
        if (screenWidth <= 0 || screenHeight <= 0) return
        
        val colors = listOf(
            Color.parseColor("#FF1744"),
            Color.parseColor("#FFEA00"),
            Color.parseColor("#00E5FF"),
            Color.parseColor("#D500F9"),
            Color.parseColor("#FFD700"),
            Color.parseColor("#76FF03")
        )
        
        val cx = Random.nextFloat() * screenWidth * 0.6f + screenWidth * 0.2f
        val cy = Random.nextFloat() * screenHeight * 0.6f + screenHeight * 0.2f
        
        repeat(40 + celebrationMode * 20) { i ->
            spiralParticles.add(SpiralParticle(
                centerX = cx,
                centerY = cy,
                angle = i * 9f,
                radius = 5f,
                radiusSpeed = Random.nextFloat() * 3f + 2f,
                angleSpeed = (if (Random.nextFloat() > 0.5f) 1f else -1f) * (Random.nextFloat() * 5f + 3f),
                size = (Random.nextFloat() * 6f + 3f) * sizeMultiplier,
                color = colors[i % colors.size],
                life = 1f,
                decay = Random.nextFloat() * 0.008f + 0.005f
            ))
        }
    }
    
    private fun spawnGoldenRain() {
        val count = (15 + celebrationMode * 10) / 3
        val largeCount = count / 10
        val normalCount = count - largeCount
        
        repeat(normalCount) {
            val x = gaussianRandom(screenWidth * 0.5f, screenWidth * 0.3f)
            spawnGoldenRainDrop(false, Random.nextFloat() * 1f + 0.5f, x)
        }
        repeat(largeCount) {
            val x = gaussianRandom(screenWidth * 0.5f, screenWidth * 0.3f)
            spawnGoldenRainDrop(false, Random.nextFloat() * 0.5f + 1.5f, x)
        }
    }
    
    private fun spawnGoldenRainDrop(@Suppress("UNUSED_PARAMETER") isFirstMilestone: Boolean = false, sizeMultiplier: Float = 1f, x: Float? = null) {
        if (screenWidth <= 0 || screenHeight <= 0) return
        
        val initialVy = Random.nextFloat() * 4f + 3f
        
        goldenRain.add(GoldenRainDrop(
            x = x ?: Random.nextFloat() * screenWidth,
            y = -10f - Random.nextFloat() * 50f,
            vy = initialVy,
            size = (Random.nextFloat() * 5f + 2f) * sizeMultiplier,
            life = 1f,
            decay = Random.nextFloat() * 0.008f + 0.005f,
            shimmer = Random.nextFloat() * 360f,
            shimmerSpeed = Random.nextFloat() * 10f + 5f,
            initialVy = initialVy,
            delay = Random.nextFloat() * 0.8f
        ))
    }
    
    private fun gaussianRandom(mean: Float, stdDev: Float): Float {
        val u1 = Random.nextDouble().toFloat()
        val u2 = Random.nextDouble().toFloat()
        val z = kotlin.math.sqrt(-2.0f * kotlin.math.ln(u1)) * kotlin.math.cos(2.0f * PI.toFloat() * u2)
        return (mean + z * stdDev).coerceIn(0f, screenWidth.toFloat())
    }
    
    private fun spawnRingExplosion(sizeMultiplier: Float = 1f) {
        if (screenWidth <= 0 || screenHeight <= 0) return
        
        val colors = listOf(
            Color.parseColor("#FF1744"),
            Color.parseColor("#FFEA00"),
            Color.parseColor("#00E5FF"),
            Color.parseColor("#D500F9"),
            Color.parseColor("#FFD700"),
            Color.parseColor("#76FF03"),
            Color.parseColor("#FF4081")
        )
        
        ringExplosions.add(RingExplosion(
            x = Random.nextFloat() * screenWidth * 0.6f + screenWidth * 0.2f,
            y = Random.nextFloat() * screenHeight * 0.6f + screenHeight * 0.2f,
            radius = 0f,
            maxRadius = (Random.nextFloat() * 200f + 100f) * sizeMultiplier,
            alpha = 1f,
            color = colors.random(),
            lineWidth = (Random.nextFloat() * 6f + 3f) * sizeMultiplier,
            ringCount = Random.nextInt(3) + 2
        ))
    }
    
    private fun launchFirework(sizeMultiplier: Float = 1f) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            Log.w(TAG, "launchFirework: 屏幕尺寸未初始化，跳过")
            return
        }
        
        val colors = listOf(
            Color.parseColor("#FF1744"),
            Color.parseColor("#FF6D00"),
            Color.parseColor("#FFEA00"),
            Color.parseColor("#76FF03"),
            Color.parseColor("#00E5FF"),
            Color.parseColor("#2979FF"),
            Color.parseColor("#D500F9"),
            Color.parseColor("#FF4081"),
            Color.parseColor("#E040FB"),
            Color.parseColor("#FFD700")
        )
        val mainColor = colors.random()
        val startX = Random.nextFloat() * screenWidth * 0.6f + screenWidth * 0.2f
        val startY = screenHeight + 20f
        val targetY = Random.nextFloat() * screenHeight * 0.4f + screenHeight * 0.1f
        
        fireworks.add(Firework(
            x = startX,
            y = startY,
            targetY = targetY,
            color = mainColor,
            state = FireworkState.RISING,
            progress = 0f,
            particles = mutableListOf(),
            sizeMultiplier = sizeMultiplier
        ))
    }
    
    private fun spawnPopParticles(bubble: Bubble) {
        val count = if (bubble.radius >= maxRadius * 0.8f) 30 else 16
        repeat(count) {
            val angle = Random.nextFloat() * 360f
            val rad = angle * PI / 180f
            val speed = Random.nextFloat() * 12f + 4f
            particles.add(Particle(
                x = bubble.x,
                y = bubble.y,
                vx = cos(rad).toFloat() * speed,
                vy = sin(rad).toFloat() * speed,
                color = bubble.color,
                size = Random.nextFloat() * 7f + 2f,
                life = 1f,
                decay = Random.nextFloat() * 0.018f + 0.01f,
                type = Random.nextInt(3)
            ))
        }
        
        // 额外金色闪光
        repeat(8) {
            val angle = Random.nextFloat() * 360f
            val rad = angle * PI / 180f
            val speed = Random.nextFloat() * 6f + 2f
            particles.add(Particle(
                x = bubble.x,
                y = bubble.y,
                vx = cos(rad).toFloat() * speed,
                vy = sin(rad).toFloat() * speed,
                color = Color.parseColor("#FFD700"),
                size = Random.nextFloat() * 4f + 1f,
                life = 1f,
                decay = Random.nextFloat() * 0.025f + 0.015f,
                type = 2
            ))
        }
    }
    
    private fun spawnShockwave(bubble: Bubble) {
        shockwaves.add(Shockwave(
            x = bubble.x,
            y = bubble.y,
            radius = bubble.radius,
            maxRadius = bubble.radius * 4f,
            alpha = 1f,
            color = bubble.color
        ))
    }
    
    private fun drawCelebration(canvas: Canvas) {
        if (celebrationMode == 0) return
        if (screenWidth <= 0 || screenHeight <= 0) return
        
        val progress = celebrationProgress
        
        // 更新和绘制礼花
        updateAndDrawFireworks(canvas)
        
        // 庆祝文字 - 夸张涂鸦艺术风格
        if (progress > 0.1f && celebrationText.isNotEmpty()) {
            val textAlpha = ((progress - 0.1f) / 0.3f).coerceIn(0f, 1f)
            val textSize = when (celebrationMode) {
                1 -> 140f
                2 -> 180f
                3 -> 240f
                4 -> 320f
                else -> 140f
            }
            
            val centerX = screenWidth / 2f
            val centerY = screenHeight / 2f
            
            // 缩放动画 + 弹性效果
            val scale = if (progress < 0.2f) {
                progress / 0.2f
            } else {
                1f + 0.08f * sin((progress - 0.2f) * 18f).toFloat() * (1f - (progress - 0.2f).coerceAtMost(1f))
            }
            
            // 旋转抖动
            val rotation = sin((progress - 0.2f) * 12f).toFloat() * 4f * (1f - (progress - 0.2f).coerceAtMost(1f))
            
            canvas.save()
            canvas.translate(centerX, centerY)
            canvas.scale(scale, scale)
            canvas.rotate(rotation)
            
            celebrationPaint.textSize = textSize
            celebrationPaint.textAlign = Paint.Align.CENTER
            celebrationPaint.alpha = (textAlpha * 255).toInt()
            
            // 获取文字宽度用于装饰定位
            celebrationPaint.textSize = textSize
            val textWidth = celebrationPaint.measureText(celebrationText)
            val halfTextWidth = textWidth / 2f
            val halfTextHeight = textSize * 0.5f
            
            // 涂鸦风格装饰 - 爆炸星形
            celebrationPaint.style = Paint.Style.FILL
            val starColors = listOf(
                Color.parseColor("#FF1744"),
                Color.parseColor("#FFEA00"),
                Color.parseColor("#00E5FF"),
                Color.parseColor("#76FF03"),
                Color.parseColor("#D500F9"),
                Color.parseColor("#FF6D00"),
                Color.parseColor("#FF4081")
            )
            
            celebrationPaint.alpha = (textAlpha * 200).toInt()
            repeat(20 + celebrationMode * 8) {
                val angle = it * 18f + progress * 80f
                val rad = angle * PI / 180f
                val dist = halfTextWidth + textSize * 0.3f + Random.nextFloat() * textSize * 0.6f
                val sx = cos(rad).toFloat() * dist
                val sy = sin(rad).toFloat() * dist * 0.6f
                val sSize = Random.nextFloat() * 10f + 4f
                
                celebrationPaint.color = starColors[it % starColors.size]
                canvas.drawCircle(sx, sy, sSize, celebrationPaint)
            }
            
            // 涂鸦风格装饰 - 闪电/折线
            celebrationPaint.style = Paint.Style.STROKE
            celebrationPaint.strokeWidth = 5f
            celebrationPaint.color = Color.parseColor("#FFEA00")
            celebrationPaint.alpha = (textAlpha * 220).toInt()
            
            repeat(6) {
                val baseAngle = it * 60f + 30f
                val rad = baseAngle * PI / 180f
                val startX = cos(rad).toFloat() * (halfTextWidth + textSize * 0.2f)
                val startY = sin(rad).toFloat() * (halfTextHeight * 0.5f)
                
                val lightningPath = Path()
                lightningPath.moveTo(startX, startY)
                lightningPath.lineTo(startX + 25f, startY - 20f)
                lightningPath.lineTo(startX + 15f, startY - 35f)
                lightningPath.lineTo(startX + 40f, startY - 55f)
                canvas.drawPath(lightningPath, celebrationPaint)
            }
            
            // 涂鸦风格装饰 - 波浪线
            celebrationPaint.style = Paint.Style.STROKE
            celebrationPaint.strokeWidth = 3f
            celebrationPaint.color = Color.parseColor("#00E5FF")
            celebrationPaint.alpha = (textAlpha * 180).toInt()
            
            repeat(4) {
                val waveY = -halfTextHeight - textSize * 0.3f + it * textSize * 0.15f
                val wavePath = Path()
                wavePath.moveTo(-halfTextWidth - 30f, waveY)
                for (i in 0..8) {
                    val wx = -halfTextWidth - 30f + i * (textWidth + 60f) / 8f
                    val wy = waveY + sin(i * 0.8f + progress * 10f) * 10f
                    wavePath.lineTo(wx, wy)
                }
                canvas.drawPath(wavePath, celebrationPaint)
            }
            
            // 第1层：超粗黑色描边（涂鸦风格核心）
            celebrationPaint.style = Paint.Style.STROKE
            celebrationPaint.strokeWidth = textSize * 0.18f
            celebrationPaint.strokeJoin = Paint.Join.ROUND
            celebrationPaint.strokeCap = Paint.Cap.ROUND
            celebrationPaint.color = Color.BLACK
            celebrationPaint.alpha = (textAlpha * 255).toInt()
            canvas.drawText(celebrationText, 0f, textSize * 0.35f, celebrationPaint)
            
            // 第2层：彩色外描边
            celebrationPaint.strokeWidth = textSize * 0.12f
            celebrationPaint.color = when (celebrationMode) {
                1 -> Color.parseColor("#00BCD4")
                2 -> Color.parseColor("#FF5722")
                3 -> Color.parseColor("#E91E63")
                4 -> Color.parseColor("#FF6F00")
                else -> Color.parseColor("#00BCD4")
            }
            canvas.drawText(celebrationText, 0f, textSize * 0.35f, celebrationPaint)
            
            // 第3层：渐变填充效果（用多层模拟）
            val gradientColors = when (celebrationMode) {
                1 -> listOf(Color.parseColor("#E0F7FA"), Color.parseColor("#4FC3F7"), Color.parseColor("#0288D1"))
                2 -> listOf(Color.parseColor("#FFF8E1"), Color.parseColor("#FFB74D"), Color.parseColor("#E65100"))
                3 -> listOf(Color.parseColor("#FCE4EC"), Color.parseColor("#F06292"), Color.parseColor("#AD1457"))
                4 -> listOf(Color.parseColor("#FFFDE7"), Color.parseColor("#FFD54F"), Color.parseColor("#F57F17"))
                else -> listOf(Color.WHITE, Color.parseColor("#4FC3F7"), Color.parseColor("#0288D1"))
            }
            
            // 顶部高光
            celebrationPaint.style = Paint.Style.FILL
            celebrationPaint.color = gradientColors[0]
            celebrationPaint.alpha = (textAlpha * 255).toInt()
            canvas.drawText(celebrationText, 0f, textSize * 0.35f - 4f, celebrationPaint)
            
            // 中间主色
            celebrationPaint.color = gradientColors[1]
            celebrationPaint.alpha = (textAlpha * 230).toInt()
            canvas.drawText(celebrationText, 0f, textSize * 0.35f, celebrationPaint)
            
            // 底部阴影
            celebrationPaint.color = gradientColors[2]
            celebrationPaint.alpha = (textAlpha * 190).toInt()
            canvas.drawText(celebrationText, 0f, textSize * 0.35f + 4f, celebrationPaint)
            
            // 第4层：白色高光（涂鸦风格亮点）
            celebrationPaint.style = Paint.Style.FILL
            celebrationPaint.color = Color.WHITE
            celebrationPaint.alpha = (textAlpha * 220).toInt()
            canvas.drawText(celebrationText, -3f, textSize * 0.35f - 3f, celebrationPaint)
            
            // 第5层：内部细描边（增强立体感）
            celebrationPaint.style = Paint.Style.STROKE
            celebrationPaint.strokeWidth = 3f
            celebrationPaint.color = Color.WHITE
            celebrationPaint.alpha = (textAlpha * 160).toInt()
            canvas.drawText(celebrationText, 0f, textSize * 0.35f, celebrationPaint)
            
            // 第6层：文字内部装饰点（模拟涂鸦高光点）
            celebrationPaint.style = Paint.Style.FILL
            celebrationPaint.color = Color.WHITE
            celebrationPaint.alpha = (textAlpha * 180).toInt()
            repeat(celebrationText.length * 2) {
                val charIndex = it % celebrationText.length
                val charX = -halfTextWidth + (charIndex + 0.5f) * (textWidth / celebrationText.length)
                val charY = textSize * 0.35f - textSize * 0.15f + Random.nextFloat() * textSize * 0.2f
                canvas.drawCircle(charX, charY, 3f + Random.nextFloat() * 3f, celebrationPaint)
            }
            
            // 分数显示 - 涂鸦风格
            val scoreText = "得分: $score"
            celebrationPaint.textSize = textSize * 0.28f
            celebrationPaint.style = Paint.Style.STROKE
            celebrationPaint.strokeWidth = 5f
            celebrationPaint.color = Color.BLACK
            celebrationPaint.alpha = (textAlpha * 255).toInt()
            canvas.drawText(scoreText, 0f, textSize * 0.85f, celebrationPaint)
            
            celebrationPaint.style = Paint.Style.FILL
            celebrationPaint.color = Color.parseColor("#FFD700")
            canvas.drawText(scoreText, 0f, textSize * 0.85f, celebrationPaint)
            
            canvas.restore()
        }
        
        paint.style = Paint.Style.FILL
    }
    
    private fun updateAndDrawFireworks(canvas: Canvas) {
        // 先清理，避免遍历时修改列表
        val toRemove = fireworks.filter { it.state == FireworkState.EXPLODED && it.particles.isEmpty() }
        fireworks.removeAll(toRemove)
        
        for (fw in fireworks.toList()) {
            when (fw.state) {
                FireworkState.RISING -> {
                    fw.y += (fw.y - fw.targetY) * -0.12f
                    fw.progress += 0.03f
                    
                    // 上升拖尾
                    trailPaint.color = fw.color
                    trailPaint.alpha = 180
                    canvas.drawCircle(fw.x, fw.y, 3f, trailPaint)
                    
                    if (fw.y <= fw.targetY) {
                        fw.state = FireworkState.EXPLODING
                        fw.progress = 0f
                        val particleCount = 40 + celebrationMode * 15
                        for (i in 0 until particleCount) {
                            val angle = i * (360f / particleCount) + Random.nextFloat() * 20f
                            val rad = angle * PI / 180f
                            val speed = Random.nextFloat() * 6f + 2f
                            fw.particles.add(FireworkParticle(
                                x = fw.x,
                                y = fw.y,
                                vx = cos(rad).toFloat() * speed,
                                vy = sin(rad).toFloat() * speed,
                                color = fw.color,
                                life = 1f,
                                decay = Random.nextFloat() * 0.015f + 0.008f,
                                size = (Random.nextFloat() * 4f + 2f) * fw.sizeMultiplier
                            ))
                        }
                        for (i in 0 until 15) {
                            val angle = i * (360f / 15f)
                            val rad = angle * PI / 180f
                            val speed = Random.nextFloat() * 3f + 1f
                            fw.particles.add(FireworkParticle(
                                x = fw.x,
                                y = fw.y,
                                vx = cos(rad).toFloat() * speed,
                                vy = sin(rad).toFloat() * speed,
                                color = Color.WHITE,
                                life = 1f,
                                decay = 0.02f,
                                size = (Random.nextFloat() * 3f + 1f) * fw.sizeMultiplier
                            ))
                        }
                    }
                }
                FireworkState.EXPLODING -> {
                    fw.progress += 0.02f
                    fw.particles.removeAll { it.life <= 0 }
                    
                    for (p in fw.particles.toList()) {
                        p.x += p.vx
                        p.y += p.vy
                        p.vy += 0.06f  // 重力
                        p.vx *= 0.98f
                        p.life -= p.decay
                        
                        paint.color = p.color
                        paint.alpha = (p.life * 220).toInt()
                        paint.style = Paint.Style.FILL
                        canvas.drawCircle(p.x, p.y, p.size * p.life, paint)
                        
                        // 闪光效果
                        if (p.life > 0.5f) {
                            val sparkle = sin(p.life * PI * 6f).toFloat() * 0.5f + 0.5f
                            paint.alpha = (p.life * sparkle * 100).toInt()
                            canvas.drawCircle(p.x, p.y, p.size * p.life * 1.5f, paint)
                        }
                    }
                }
                FireworkState.EXPLODED -> {
                    fw.particles.removeAll { it.life <= 0 }
                    for (p in fw.particles.toList()) {
                        p.x += p.vx
                        p.y += p.vy
                        p.vy += 0.06f
                        p.life -= p.decay
                        
                        paint.color = p.color
                        paint.alpha = (p.life * 180).toInt()
                        paint.style = Paint.Style.FILL
                        canvas.drawCircle(p.x, p.y, p.size * p.life, paint)
                    }
                }
            }
        }
        
        paint.style = Paint.Style.FILL
    }
    
    private fun drawParticles(canvas: Canvas) {
        particles.removeAll { it.life <= 0 }
        
        for (p in particles.toList()) {
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.12f
            p.vx *= 0.99f
            p.life -= p.decay
            
            when (p.type) {
                0 -> {
                    // 圆形粒子
                    paint.color = p.color
                    paint.alpha = (p.life * 220).toInt()
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(p.x, p.y, p.size * p.life, paint)
                }
                1 -> {
                    // 星形粒子
                    paint.color = p.color
                    paint.alpha = (p.life * 200).toInt()
                    paint.style = Paint.Style.FILL
                    drawStar(canvas, p.x, p.y, p.size * p.life)
                }
                2 -> {
                    // 闪光粒子
                    val sparkle = sin(p.life * PI * 8f).toFloat() * 0.5f + 0.5f
                    paint.color = p.color
                    paint.alpha = (p.life * sparkle * 255).toInt()
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(p.x, p.y, p.size * p.life * sparkle, paint)
                }
            }
        }
        
        goldenRays.removeAll { it.alpha <= 0 }
        for (ray in goldenRays.toList()) {
            ray.alpha -= ray.decay
        }
        
        paint.style = Paint.Style.FILL
    }
    
    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val points = 5
        val outerR = size
        val innerR = size * 0.4f
        
        bubblePath.reset()
        for (i in 0 until points * 2) {
            val angle = (i * 360f / (points * 2) - 90f) * PI / 180f
            val r = if (i % 2 == 0) outerR else innerR
            val px = cx + cos(angle).toFloat() * r
            val py = cy + sin(angle).toFloat() * r
            if (i == 0) bubblePath.moveTo(px, py) else bubblePath.lineTo(px, py)
        }
        bubblePath.close()
        canvas.drawPath(bubblePath, paint)
    }
    
    private fun drawShockwaves(canvas: Canvas) {
        shockwaves.removeAll { it.alpha <= 0 }
        
        for (sw in shockwaves.toList()) {
            sw.radius += (sw.maxRadius - sw.radius) * 0.15f
            sw.alpha -= 0.04f
            
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f * sw.alpha
            paint.alpha = (sw.alpha * 180).toInt()
            paint.color = sw.color
            canvas.drawCircle(sw.x, sw.y, sw.radius, paint)
            
            // 内层冲击波
            paint.alpha = (sw.alpha * 80).toInt()
            paint.color = Color.WHITE
            canvas.drawCircle(sw.x, sw.y, sw.radius * 0.7f, paint)
        }
        
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }
    
    // ====== 属性爆炸粒子系统 ======
    private fun spawnExplosionParticles(bubble: Bubble) {
        val type = bubble.explosionType
        val isRare = bubble.isHiddenRare
        val count = if (isRare) 40 else if (bubble.radius >= maxRadius * 0.8f) 30 else 20
        val speed = if (isRare) 12f else 8f
        
        when (type) {
            ExplosionType.NONE -> {}
            ExplosionType.FIRE -> spawnFireExplosion(bubble, count, speed)
            ExplosionType.LIGHTNING -> spawnLightningExplosion(bubble, count, speed)
            ExplosionType.THUNDER -> spawnThunderExplosion(bubble, count, speed)
            ExplosionType.WIND -> spawnWindExplosion(bubble, count, speed)
            ExplosionType.RAIN -> spawnRainExplosion(bubble, count, speed)
            ExplosionType.DARK -> spawnDarkExplosion(bubble, count, speed)
            ExplosionType.LIGHT -> spawnLightExplosion(bubble, count, speed)
            ExplosionType.ICE -> spawnIceExplosion(bubble, count, speed)
            ExplosionType.VOID -> spawnVoidExplosion(bubble, count, speed)
            ExplosionType.STAR -> spawnStarExplosion(bubble, count, speed)
        }
    }
    
    private fun spawnFireExplosion(bubble: Bubble, count: Int, speed: Float) {
        for (i in 0 until count) {
            val angle = i * (360f / count) + Random.nextFloat() * 20f
            val rad = angle * PI / 180f
            val spd = Random.nextFloat() * speed + 3f
            val colors = listOf(bubble.explosionType.primaryColor, bubble.explosionType.secondaryColor, bubble.explosionType.particleColor)
            particles.add(Particle(
                x = bubble.x, y = bubble.y,
                vx = cos(rad).toFloat() * spd,
                vy = sin(rad).toFloat() * spd - 2f,
                size = Random.nextFloat() * bubble.radius * 0.2f + 4f,
                color = colors.random(),
                life = 1f, decay = Random.nextFloat() * 0.015f + 0.01f,
                type = 0
            ))
        }
        // 火焰上升粒子
        repeat(15) {
            particles.add(Particle(
                x = bubble.x + Random.nextFloat() * bubble.radius - bubble.radius / 2,
                y = bubble.y,
                vx = Random.nextFloat() * 2f - 1f,
                vy = -Random.nextFloat() * 6f - 3f,
                size = Random.nextFloat() * bubble.radius * 0.15f + 3f,
                color = bubble.explosionType.particleColor,
                life = 1f, decay = 0.02f, type = 0
            ))
        }
    }
    
    private fun spawnLightningExplosion(bubble: Bubble, count: Int, speed: Float) {
        // 闪电链
        for (i in 0 until count) {
            val angle = i * (360f / count)
            val rad = angle * PI / 180f
            val spd = Random.nextFloat() * speed + 4f
            particles.add(Particle(
                x = bubble.x, y = bubble.y,
                vx = cos(rad).toFloat() * spd,
                vy = sin(rad).toFloat() * spd,
                size = Random.nextFloat() * 3f + 2f,
                color = bubble.explosionType.primaryColor,
                life = 1f, decay = 0.025f, type = 2
            ))
        }
        // 闪电分支
        repeat(8) {
            var px = bubble.x
            var py = bubble.y
            repeat(12) { step ->
                val angle = Random.nextFloat() * 360f
                val rad = angle * PI / 180f
                val dist = Random.nextFloat() * 20f + 10f
                px += cos(rad).toFloat() * dist
                py += sin(rad).toFloat() * dist
                particles.add(Particle(
                    x = px, y = py,
                    vx = 0f, vy = 0f,
                    size = max(1f, 5f - step * 0.4f),
                    color = Color.WHITE,
                    life = 1f, decay = 0.04f, type = 2
                ))
            }
        }
    }
    
    private fun spawnThunderExplosion(bubble: Bubble, count: Int, speed: Float) {
        spawnLightningExplosion(bubble, count, speed)
        // 额外紫色冲击波
        for (i in 0 until count / 2) {
            val angle = i * (360f / (count / 2))
            val rad = angle * PI / 180f
            particles.add(Particle(
                x = bubble.x, y = bubble.y,
                vx = cos(rad).toFloat() * speed * 1.5f,
                vy = sin(rad).toFloat() * speed * 1.5f,
                size = bubble.radius * 0.15f,
                color = bubble.explosionType.particleColor,
                life = 1f, decay = 0.015f, type = 0
            ))
        }
    }
    
    private fun spawnWindExplosion(bubble: Bubble, count: Int, speed: Float) {
        for (i in 0 until count * 2) {
            val angle = Random.nextFloat() * 360f
            val rad = angle * PI / 180f
            val spd = Random.nextFloat() * speed * 1.5f + 2f
            particles.add(Particle(
                x = bubble.x, y = bubble.y,
                vx = cos(rad).toFloat() * spd + 3f,
                vy = sin(rad).toFloat() * spd * 0.5f,
                size = Random.nextFloat() * 4f + 1f,
                color = bubble.explosionType.particleColor,
                life = 1f, decay = 0.012f, type = 0
            ))
        }
        // 旋风粒子
        repeat(20) {
            val angle = it * 18f
            val rad = angle * PI / 180f
            particles.add(Particle(
                x = bubble.x + cos(rad).toFloat() * bubble.radius,
                y = bubble.y + sin(rad).toFloat() * bubble.radius,
                vx = cos(rad + PI / 2f).toFloat() * 5f,
                vy = sin(rad + PI / 2f).toFloat() * 5f,
                size = 3f,
                color = bubble.explosionType.secondaryColor,
                life = 1f, decay = 0.02f, type = 1
            ))
        }
    }
    
    private fun spawnRainExplosion(bubble: Bubble, count: Int, speed: Float) {
        for (i in 0 until count) {
            val angle = i * (360f / count)
            val rad = angle * PI / 180f
            particles.add(Particle(
                x = bubble.x, y = bubble.y,
                vx = cos(rad).toFloat() * speed * 0.5f,
                vy = sin(rad).toFloat() * speed * 0.5f,
                size = Random.nextFloat() * 3f + 2f,
                color = bubble.explosionType.particleColor,
                life = 1f, decay = 0.015f, type = 0
            ))
        }
        // 雨滴下落
        repeat(30) {
            particles.add(Particle(
                x = bubble.x + Random.nextFloat() * bubble.radius * 3 - bubble.radius * 1.5f,
                y = bubble.y - bubble.radius * 2,
                vx = Random.nextFloat() * 2f - 1f,
                vy = Random.nextFloat() * 8f + 4f,
                size = Random.nextFloat() * 2f + 1f,
                color = bubble.explosionType.secondaryColor,
                life = 1f, decay = 0.01f, type = 0
            ))
        }
    }
    
    private fun spawnDarkExplosion(bubble: Bubble, count: Int, speed: Float) {
        // 暗影吞噬效果 - 向内收缩再向外爆发
        for (i in 0 until count) {
            val angle = i * (360f / count)
            val rad = angle * PI / 180f
            val spd = Random.nextFloat() * speed + 2f
            particles.add(Particle(
                x = bubble.x, y = bubble.y,
                vx = cos(rad).toFloat() * spd,
                vy = sin(rad).toFloat() * spd,
                size = Random.nextFloat() * bubble.radius * 0.25f + 5f,
                color = bubble.explosionType.primaryColor,
                life = 1f, decay = 0.01f, type = 0
            ))
        }
        // 暗影漩涡
        repeat(24) {
            val angle = it * 15f
            val rad = angle * PI / 180f
            particles.add(Particle(
                x = bubble.x + cos(rad).toFloat() * bubble.radius * 0.5f,
                y = bubble.y + sin(rad).toFloat() * bubble.radius * 0.5f,
                vx = cos(rad + PI / 2f).toFloat() * 6f,
                vy = sin(rad + PI / 2f).toFloat() * 6f,
                size = 4f,
                color = bubble.explosionType.secondaryColor,
                life = 1f, decay = 0.018f, type = 0
            ))
        }
    }
    
    private fun spawnLightExplosion(bubble: Bubble, count: Int, speed: Float) {
        // 圣光爆发 - 全屏闪光 + 金色粒子
        for (i in 0 until count) {
            val angle = i * (360f / count)
            val rad = angle * PI / 180f
            val spd = Random.nextFloat() * speed * 1.5f + 3f
            particles.add(Particle(
                x = bubble.x, y = bubble.y,
                vx = cos(rad).toFloat() * spd,
                vy = sin(rad).toFloat() * spd,
                size = Random.nextFloat() * bubble.radius * 0.2f + 4f,
                color = bubble.explosionType.particleColor,
                life = 1f, decay = 0.012f, type = 2
            ))
        }
        // 光柱
        repeat(12) {
            val angle = it * 30f
            val rad = angle * PI / 180f
            repeat(8) { step ->
                particles.add(Particle(
                    x = bubble.x + cos(rad).toFloat() * step * 15f,
                    y = bubble.y + sin(rad).toFloat() * step * 15f,
                    vx = 0f, vy = 0f,
                    size = 6f - step * 0.5f,
                    color = Color.WHITE,
                    life = 1f, decay = 0.03f, type = 2
                ))
            }
        }
    }
    
    private fun spawnIceExplosion(bubble: Bubble, count: Int, speed: Float) {
        // 冰霜碎裂
        for (i in 0 until count) {
            val angle = i * (360f / count)
            val rad = angle * PI / 180f
            val spd = Random.nextFloat() * speed + 2f
            particles.add(Particle(
                x = bubble.x, y = bubble.y,
                vx = cos(rad).toFloat() * spd,
                vy = sin(rad).toFloat() * spd,
                size = Random.nextFloat() * 5f + 3f,
                color = bubble.explosionType.particleColor,
                life = 1f, decay = 0.015f, type = 1
            ))
        }
        // 冰晶碎片
        repeat(16) {
            val angle = Random.nextFloat() * 360f
            val rad = angle * PI / 180f
            particles.add(Particle(
                x = bubble.x, y = bubble.y,
                vx = cos(rad).toFloat() * speed * 0.8f,
                vy = sin(rad).toFloat() * speed * 0.8f,
                size = Random.nextFloat() * 6f + 4f,
                color = Color.WHITE,
                life = 1f, decay = 0.01f, type = 1
            ))
        }
    }
    
    private fun spawnVoidExplosion(bubble: Bubble, count: Int, speed: Float) {
        // 虚空吞噬 - 黑洞效果
        for (i in 0 until count * 2) {
            val angle = i * (360f / (count * 2))
            val rad = angle * PI / 180f
            val spd = Random.nextFloat() * speed + 3f
            particles.add(Particle(
                x = bubble.x, y = bubble.y,
                vx = cos(rad).toFloat() * spd,
                vy = sin(rad).toFloat() * spd,
                size = Random.nextFloat() * 8f + 4f,
                color = bubble.explosionType.primaryColor,
                life = 1f, decay = 0.008f, type = 0
            ))
        }
        // 虚空裂缝
        repeat(12) {
            val angle = it * 30f
            val rad = angle * PI / 180f
            particles.add(Particle(
                x = bubble.x + cos(rad).toFloat() * bubble.radius,
                y = bubble.y + sin(rad).toFloat() * bubble.radius,
                vx = cos(rad).toFloat() * 10f,
                vy = sin(rad).toFloat() * 10f,
                size = 8f,
                color = bubble.explosionType.secondaryColor,
                life = 1f, decay = 0.015f, type = 2
            ))
        }
    }
    
    private fun spawnStarExplosion(bubble: Bubble, count: Int, speed: Float) {
        // 星辰陨落
        for (i in 0 until count) {
            val angle = i * (360f / count)
            val rad = angle * PI / 180f
            val spd = Random.nextFloat() * speed + 3f
            particles.add(Particle(
                x = bubble.x, y = bubble.y,
                vx = cos(rad).toFloat() * spd,
                vy = sin(rad).toFloat() * spd - 3f,
                size = Random.nextFloat() * 6f + 3f,
                color = listOf(bubble.explosionType.primaryColor, bubble.explosionType.secondaryColor, bubble.explosionType.particleColor).random(),
                life = 1f, decay = 0.01f, type = 2
            ))
        }
        // 流星轨迹
        repeat(10) {
            val angle = Random.nextFloat() * 360f
            val rad = angle * PI / 180f
            repeat(6) { step ->
                particles.add(Particle(
                    x = bubble.x + cos(rad).toFloat() * step * 12f,
                    y = bubble.y + sin(rad).toFloat() * step * 12f,
                    vx = cos(rad).toFloat() * 3f,
                    vy = sin(rad).toFloat() * 3f,
                    size = 5f - step * 0.6f,
                    color = Color.WHITE,
                    life = 1f, decay = 0.025f, type = 2
                ))
            }
        }
    }
    
    // ====== 盲盒提示特效 ======
    private fun spawnBlindBoxHint(bubble: Bubble) {
        val hintColor = Color.argb(200, 255, 215, 0)
        for (i in 0 until 8) {
            val angle = i * 45f
            val rad = angle * PI / 180f
            particles.add(Particle(
                x = bubble.x, y = bubble.y,
                vx = cos(rad).toFloat() * 3f,
                vy = sin(rad).toFloat() * 3f,
                size = 3f,
                color = hintColor,
                life = 1f, decay = 0.04f, type = 2
            ))
        }
        // 显示提示文字
        neonTextEffects.add(NeonTextEffect(
            text = "还差一点!",
            x = bubble.x,
            y = bubble.y - bubble.radius - 30f,
            color = hintColor,
            life = 1f,
            decay = 0.02f,
            textSize = bubble.radius * 0.6f
        ))
    }
    
    // ====== 霓虹文字特效 ======
    private fun spawnNeonTextEffect(bubble: Bubble) {
        val type = bubble.explosionType
        val isRare = bubble.isHiddenRare
        val text = if (isRare) "★ ${bubble.neonText} ★" else bubble.neonText
        val color = type.primaryColor
        val borderColor = bubble.color
        
        // 根据球球大小决定文字效果
        val sizeRatio = bubble.radius / maxRadius
        
        if (screenWidth <= 0 || screenHeight <= 0) {
            Log.w(TAG, "spawnNeonTextEffect: 屏幕尺寸未初始化，跳过")
            return
        }
        
        if (sizeRatio > 0.25f) {
            // 大于屏幕1/4的球球：全屏文字特写
            neonTextEffects.add(NeonTextEffect(
                text = text,
                x = screenWidth / 2f,
                y = screenHeight / 2f,
                color = color,
                borderColor = borderColor,
                life = 1f,
                decay = if (isRare) 0.006f else 0.009f,
                textSize = min(screenWidth * 0.15f, 80f),
                isRare = isRare || sizeRatio > 0.4f,
                scale = 0.1f,
                targetScale = if (sizeRatio > 0.4f) 4f else 3f
            ))
            
            // 额外加一个属性名称
            neonTextEffects.add(NeonTextEffect(
                text = type.label,
                x = screenWidth / 2f,
                y = screenHeight / 2f + 120f,
                color = borderColor,
                borderColor = color,
                life = 1f,
                decay = 0.01f,
                textSize = 40f,
                isRare = false,
                scale = 0.2f,
                targetScale = 2f
            ))
        } else {
            // 普通大小球球：文字与球球大小成正比
            val baseSize = bubble.radius * 1.2f
            neonTextEffects.add(NeonTextEffect(
                text = text,
                x = bubble.x,
                y = bubble.y,
                color = color,
                borderColor = borderColor,
                life = 1f,
                decay = if (isRare) 0.008f else 0.012f,
                textSize = baseSize,
                isRare = isRare,
                scale = 0.3f,
                targetScale = if (isRare) 2.5f else 1.8f
            ))
        }
        
        // 隐藏款额外加一个全屏特写
        if (isRare && sizeRatio <= 0.25f) {
            neonTextEffects.add(NeonTextEffect(
                text = "隐藏款!",
                x = screenWidth / 2f,
                y = screenHeight / 2f,
                color = Color.parseColor("#FFD700"),
                borderColor = bubble.color,
                life = 1f,
                decay = 0.01f,
                textSize = 60f,
                isRare = true,
                scale = 0.1f,
                targetScale = 3f
            ))
        }
    }
    
    private fun drawNeonTextEffects(canvas: Canvas) {
        neonTextEffects.removeAll { it.life <= 0 }
        
        for (effect in neonTextEffects.toList()) {
            effect.life -= effect.decay
            effect.scale += (effect.targetScale - effect.scale) * 0.15f
            
            val alpha = (effect.life * 255).toInt()
            val currentSize = effect.textSize * effect.scale
            
            // 计算文字宽度，确保不超出屏幕
            neonTextPaint.textSize = currentSize
            neonTextPaint.textAlign = Paint.Align.CENTER
            val textWidth = neonTextPaint.measureText(effect.text)
            
            // 修复：当文字宽度超过屏幕时，缩小文字尺寸
            val maxAllowedWidth = screenWidth - 40f
            val adjustedSize = if (textWidth > maxAllowedWidth) {
                currentSize * (maxAllowedWidth / textWidth)
            } else {
                currentSize
            }
            
            // 重新计算
            neonTextPaint.textSize = adjustedSize
            val adjustedTextWidth = neonTextPaint.measureText(effect.text)
            val adjustedHalfWidth = adjustedTextWidth / 2f
            
            // 限制x坐标在屏幕范围内
            val minX = adjustedHalfWidth + 20f
            val maxX = screenWidth - adjustedHalfWidth - 20f
            val clampedX = if (minX <= maxX) effect.x.coerceIn(minX, maxX) else screenWidth / 2f
            // 限制y坐标
            val clampedY = effect.y.coerceIn(adjustedSize + 20f, screenHeight - 20f)
            
            // 外层霓虹光晕（大尺寸模糊）
            neonTextPaint.style = Paint.Style.STROKE
            neonTextPaint.strokeWidth = adjustedSize * 0.2f
            neonTextPaint.alpha = alpha / 4
            neonTextPaint.color = effect.borderColor
            canvas.drawText(effect.text, clampedX, clampedY, neonTextPaint)
            
            // 中层霓虹描边
            neonTextPaint.style = Paint.Style.STROKE
            neonTextPaint.strokeWidth = adjustedSize * 0.12f
            neonTextPaint.alpha = (alpha * 0.7f).toInt()
            neonTextPaint.color = effect.borderColor
            canvas.drawText(effect.text, clampedX, clampedY, neonTextPaint)
            
            // 内部细描边（白色增强对比）
            neonTextPaint.style = Paint.Style.STROKE
            neonTextPaint.strokeWidth = adjustedSize * 0.04f
            neonTextPaint.alpha = alpha
            neonTextPaint.color = Color.WHITE
            canvas.drawText(effect.text, clampedX, clampedY, neonTextPaint)
            
            // 文字填充（主色）
            neonTextPaint.style = Paint.Style.FILL
            neonTextPaint.alpha = alpha
            neonTextPaint.color = effect.color
            canvas.drawText(effect.text, clampedX, clampedY, neonTextPaint)
            
            neonTextPaint.style = Paint.Style.FILL
            neonTextPaint.alpha = 255
        }
    }
    
    private fun drawCelebrationParticles(canvas: Canvas) {
        celebrationParticles.removeAll { it.life <= 0 }
        
        for (p in celebrationParticles.toList()) {
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.12f
            p.vx *= 0.99f
            p.life -= p.decay
            
            val rawAlpha = (p.life * 255).toInt()
            val fadeAlpha = if (p.life > 0.3f) {
                255
            } else {
                (p.life / 0.3f * 255).toInt()
            }
            val alpha = minOf(rawAlpha, fadeAlpha)
            
            when (p.type) {
                0 -> {
                    celebrationParticlePaint.color = p.color
                    celebrationParticlePaint.alpha = alpha / 4
                    celebrationParticlePaint.style = Paint.Style.FILL
                    canvas.drawCircle(p.x, p.y, p.size * p.life * 3f, celebrationParticlePaint)
                    
                    celebrationParticlePaint.alpha = alpha / 2
                    canvas.drawCircle(p.x, p.y, p.size * p.life * 1.8f, celebrationParticlePaint)
                    
                    celebrationParticlePaint.alpha = alpha
                    canvas.drawCircle(p.x, p.y, p.size * p.life, celebrationParticlePaint)
                    
                    celebrationParticlePaint.alpha = alpha / 2
                    canvas.drawCircle(p.x, p.y, p.size * p.life * 0.4f, celebrationParticlePaint)
                }
                1 -> {
                    celebrationParticlePaint.color = p.color
                    celebrationParticlePaint.alpha = alpha / 3
                    celebrationParticlePaint.style = Paint.Style.FILL
                    val size = p.size * p.life * 2f
                    canvas.drawRect(p.x - size, p.y - size, p.x + size, p.y + size, celebrationParticlePaint)
                    
                    celebrationParticlePaint.alpha = alpha
                    val innerSize = p.size * p.life
                    canvas.drawRect(p.x - innerSize, p.y - innerSize, p.x + innerSize, p.y + innerSize, celebrationParticlePaint)
                }
                2 -> {
                    celebrationParticlePaint.color = p.color
                    celebrationParticlePaint.alpha = alpha / 4
                    celebrationParticlePaint.style = Paint.Style.STROKE
                    celebrationParticlePaint.strokeWidth = 3f
                    val size = p.size * p.life * 2.5f
                    canvas.drawCircle(p.x, p.y, size, celebrationParticlePaint)
                    
                    celebrationParticlePaint.alpha = alpha
                    celebrationParticlePaint.strokeWidth = 2f
                    val innerSize = p.size * p.life * 1.2f
                    canvas.drawCircle(p.x, p.y, innerSize, celebrationParticlePaint)
                    
                    celebrationParticlePaint.alpha = alpha / 2
                    celebrationParticlePaint.style = Paint.Style.FILL
                    canvas.drawCircle(p.x, p.y, p.size * p.life * 0.3f, celebrationParticlePaint)
                }
            }
        }
        
        celebrationParticlePaint.alpha = 255
        celebrationParticlePaint.style = Paint.Style.FILL
    }
    
    private fun drawConfetti(canvas: Canvas) {
        confettiParticles.removeAll { it.life <= 0 }
        
        for (p in confettiParticles.toList()) {
            if (p.delay > 0) {
                p.delay -= 0.016f
                continue
            }
            
            val totalDistance = screenHeight - (-20f)
            val currentProgress = (p.y - (-20f)) / totalDistance
            
            if (currentProgress < 0.33f) {
                p.vy += 0.08f
            } else if (currentProgress < 0.66f) {
                p.vy += 0.01f
            } else {
                p.vy *= 0.97f
            }
            
            p.x += p.vx
            p.y += p.vy
            p.vx *= 0.99f
            p.rotation += p.rotSpeed
            p.life -= p.decay
            
            val alpha = if (p.life > 0.3f) {
                255
            } else {
                (p.life / 0.3f * 255).toInt()
            }
            
            celebrationParticlePaint.color = p.color
            celebrationParticlePaint.alpha = alpha
            celebrationParticlePaint.style = Paint.Style.FILL
            
            canvas.save()
            canvas.translate(p.x, p.y)
            canvas.rotate(p.rotation)
            canvas.drawRect(-p.width / 2, -p.height / 2, p.width / 2, p.height / 2, celebrationParticlePaint)
            canvas.restore()
        }
        
        celebrationParticlePaint.alpha = 255
    }
    
    private fun drawSpiralParticles(canvas: Canvas) {
        spiralParticles.removeAll { it.life <= 0 }
        
        for (p in spiralParticles.toList()) {
            p.angle += p.angleSpeed
            p.radius += p.radiusSpeed
            p.life -= p.decay
            
            val alpha = (p.life * 255).toInt()
            
            val x = p.centerX + cos(p.angle * PI / 180f).toFloat() * p.radius
            val y = p.centerY + sin(p.angle * PI / 180f).toFloat() * p.radius
            
            celebrationParticlePaint.color = p.color
            celebrationParticlePaint.alpha = alpha / 3
            celebrationParticlePaint.style = Paint.Style.FILL
            canvas.drawCircle(x, y, p.size * 2.5f, celebrationParticlePaint)
            
            celebrationParticlePaint.alpha = alpha / 2
            canvas.drawCircle(x, y, p.size * 1.5f, celebrationParticlePaint)
            
            celebrationParticlePaint.alpha = alpha
            canvas.drawCircle(x, y, p.size, celebrationParticlePaint)
            
            celebrationParticlePaint.alpha = alpha / 2
            canvas.drawCircle(x, y, p.size * 0.4f, celebrationParticlePaint)
        }
        
        celebrationParticlePaint.alpha = 255
    }
    
    private fun drawGoldenRain(canvas: Canvas) {
        goldenRain.removeAll { it.life <= 0 || it.y > screenHeight + 50f }
        
        for (p in goldenRain.toList()) {
            if (p.delay > 0) {
                p.delay -= 0.016f
                continue
            }
            
            val totalDistance = screenHeight - (-10f)
            val currentProgress = (p.y - (-10f)) / totalDistance
            
            if (currentProgress < 0.33f) {
                p.vy += 0.12f
            } else if (currentProgress < 0.66f) {
                p.vy += 0.02f
            }
            
            p.y += p.vy
            p.shimmer += p.shimmerSpeed
            p.life -= p.decay
            
            val rawAlpha = (p.life * 255).toInt()
            val fadeAlpha = if (p.life > 0.3f) {
                255
            } else {
                (p.life / 0.3f * 255).toInt()
            }
            val alpha = minOf(rawAlpha, fadeAlpha)
            
            val baseRadius = p.size * 1.5f
            
            celebrationParticlePaint.style = Paint.Style.FILL
            
            celebrationParticlePaint.color = Color.parseColor("#FFD700")
            celebrationParticlePaint.alpha = (alpha * 0.2f).toInt()
            canvas.drawCircle(p.x, p.y, baseRadius * 2.5f, celebrationParticlePaint)
            
            celebrationParticlePaint.color = Color.parseColor("#FFC107")
            celebrationParticlePaint.alpha = (alpha * 0.4f).toInt()
            canvas.drawCircle(p.x, p.y, baseRadius * 1.5f, celebrationParticlePaint)
            
            celebrationParticlePaint.color = Color.parseColor("#FFD700")
            celebrationParticlePaint.alpha = (alpha * 0.7f).toInt()
            canvas.drawCircle(p.x, p.y, baseRadius, celebrationParticlePaint)
            
            celebrationParticlePaint.color = Color.parseColor("#FFFFFF")
            celebrationParticlePaint.alpha = (alpha * 0.9f).toInt()
            canvas.drawCircle(p.x, p.y, baseRadius * 0.3f, celebrationParticlePaint)
        }
        
        celebrationParticlePaint.alpha = 255
    }
    
    private fun drawRingExplosions(canvas: Canvas) {
        ringExplosions.removeAll { it.alpha <= 0 }
        
        for (ring in ringExplosions.toList()) {
            ring.radius += (ring.maxRadius - ring.radius) * 0.08f
            ring.alpha -= 0.015f
            
            val alpha = (ring.alpha * 255).toInt()
            
            for (i in 0 until ring.ringCount) {
                val ringRadius = ring.radius - i * 15f
                if (ringRadius > 0) {
                    celebrationParticlePaint.color = ring.color
                    celebrationParticlePaint.alpha = (alpha * (1f - i * 0.2f)).toInt()
                    celebrationParticlePaint.style = Paint.Style.STROKE
                    celebrationParticlePaint.strokeWidth = ring.lineWidth * (1f - i * 0.25f)
                    canvas.drawCircle(ring.x, ring.y, ringRadius, celebrationParticlePaint)
                }
            }
        }
        
        celebrationParticlePaint.alpha = 255
        celebrationParticlePaint.style = Paint.Style.FILL
    }
    
    private fun drawScreenFlash(canvas: Canvas) {
        if (screenFlashAlpha > 0.01f) {
            celebrationParticlePaint.color = screenFlashColor
            celebrationParticlePaint.alpha = (screenFlashAlpha * 180).toInt()
            celebrationParticlePaint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), celebrationParticlePaint)
            celebrationParticlePaint.alpha = 255
        }
    }
    
    private fun triggerGameOver() {
        isGameOver = true
        settingsManager.saveHighScore(totalScore)
        onGameOver?.invoke()
    }
    
    private fun vibrate(duration: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(duration)
            }
        } catch (_: Exception) {}
    }
    
    fun resetGame() {
        bubbles.clear()
        particles.clear()
        goldenRays.clear()
        fireworks.clear()
        shockwaves.clear()
        neonTextEffects.clear()
        celebrationParticles.clear()
        confettiParticles.clear()
        spiralParticles.clear()
        goldenRain.clear()
        ringExplosions.clear()
        score = 0
        lastMilestoneScore = 0
        isGameOver = false
        isPaused = false
        celebrationMode = 0
        celebrationProgress = 0f
        celebrationText = ""
        edgeGlowAlpha = 0f
        isPressing = false
        pressedBubble = null
        pressProgress = 0f
        onScoreChanged?.invoke(0)
        
        if (!debugMode) {
            repeat(MIN_BUBBLES) {
                bubbles.add(Bubble.createRandom(screenWidth, screenHeight, 20f, maxRadius * 0.5f))
            }
        }
    }
    
    fun getScore(): Int = score
    fun getTotalScore(): Int = totalScore
    fun getHighScores(): List<Int> = settingsManager.getHighScores()
    
    data class DebugEffectConfig(
        val mode: Int = 1,
        val duration: Long = 3000,
        val text: String = "太棒了！",
        val enableConfetti: Boolean = true,
        val confettiDuration: Long = 3000,
        val confettiSize: Float = 1f,
        val enableGoldenRain: Boolean = true,
        val goldenRainDuration: Long = 3000,
        val goldenRainSize: Float = 1f,
        val enableScreenShake: Boolean = true,
        val screenShakeDuration: Long = 3000,
        val enableScreenFlash: Boolean = true,
        val screenFlashDuration: Long = 3000,
        val enableFirework: Boolean = false,
        val fireworkDuration: Long = 3000,
        val fireworkSize: Float = 1f,
        val enableCelebrationParticles: Boolean = false,
        val celebrationParticlesDuration: Long = 3000,
        val celebrationParticlesSize: Float = 1f,
        val enableSpiral: Boolean = false,
        val spiralDuration: Long = 3000,
        val spiralSize: Float = 1f,
        val enableRingExplosion: Boolean = false,
        val ringExplosionDuration: Long = 3000,
        val ringExplosionSize: Float = 1f,
        val enableShockwave: Boolean = false,
        val shockwaveDuration: Long = 3000,
        val enableCelebrationText: Boolean = true
    )
    
    fun triggerDebugEffect(config: DebugEffectConfig) {
        if (screenWidth <= 0 || screenHeight <= 0) return
        
        isPaused = true
        celebrationProgress = 0f
        fireworks.clear()
        celebrationParticles.clear()
        confettiParticles.clear()
        spiralParticles.clear()
        goldenRain.clear()
        ringExplosions.clear()
        
        celebrationMode = config.mode
        celebrationText = config.text
        
        if (config.enableScreenFlash) {
            screenFlashAlpha = 1f
            screenFlashColor = when (config.mode) {
                1 -> Color.parseColor("#4FC3F7")
                2 -> Color.parseColor("#FF7043")
                3 -> Color.parseColor("#FF4081")
                4 -> Color.parseColor("#FFD700")
                else -> Color.WHITE
            }
        }
        if (config.enableScreenShake) {
            screenShakeX = 0f
            screenShakeY = 0f
        }
        
        val maxDuration = listOfNotNull(
            config.duration,
            if (config.enableConfetti) config.confettiDuration else null,
            if (config.enableGoldenRain) config.goldenRainDuration else null,
            if (config.enableScreenShake) config.screenShakeDuration else null,
            if (config.enableScreenFlash) config.screenFlashDuration else null,
            if (config.enableFirework) config.fireworkDuration else null,
            if (config.enableCelebrationParticles) config.celebrationParticlesDuration else null,
            if (config.enableSpiral) config.spiralDuration else null,
            if (config.enableRingExplosion) config.ringExplosionDuration else null,
            if (config.enableShockwave) config.shockwaveDuration else null
        ).maxOrNull() ?: config.duration
        
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = maxDuration
        animator.addUpdateListener { anim ->
            val elapsed = anim.currentPlayTime.toFloat()
            celebrationProgress = anim.animatedValue as Float
            
            if (config.enableScreenShake) {
                val shakeProgress = elapsed / config.screenShakeDuration.toFloat()
                if (shakeProgress < 0.3f) {
                    val shakeIntensity = (1f - shakeProgress / 0.3f) * 15f
                    screenShakeX = (Random.nextFloat() - 0.5f) * shakeIntensity
                    screenShakeY = (Random.nextFloat() - 0.5f) * shakeIntensity
                } else if (shakeProgress < 1f) {
                    screenShakeX *= 0.9f
                    screenShakeY *= 0.9f
                }
            }
            
            if (config.enableScreenFlash) {
                val flashProgress = elapsed / config.screenFlashDuration.toFloat()
                if (flashProgress < 1f) {
                    screenFlashAlpha *= 0.92f
                } else {
                    screenFlashAlpha = 0f
                }
            }
            
            if (config.enableConfetti) {
                val confettiProgress = elapsed / config.confettiDuration.toFloat()
                if (confettiProgress < 1f && Random.nextFloat() > 0.4f) {
                    spawnConfettiBurst(sizeMultiplier = config.confettiSize)
                }
            }
            if (config.enableGoldenRain) {
                val goldenProgress = elapsed / config.goldenRainDuration.toFloat()
                if (goldenProgress < 1f && Random.nextFloat() > 0.3f) {
                    spawnGoldenRain()
                }
            }
            if (config.enableFirework) {
                val fireworkProgress = elapsed / config.fireworkDuration.toFloat()
                if (fireworkProgress < 1f && Random.nextFloat() > 0.3f) {
                    launchFirework(sizeMultiplier = config.fireworkSize)
                }
            }
            if (config.enableCelebrationParticles) {
                val particleProgress = elapsed / config.celebrationParticlesDuration.toFloat()
                if (particleProgress < 1f && Random.nextFloat() > 0.2f) {
                    spawnCelebrationBurst(sizeMultiplier = config.celebrationParticlesSize)
                }
            }
            if (config.enableSpiral) {
                val spiralProgress = elapsed / config.spiralDuration.toFloat()
                if (spiralProgress < 1f && Random.nextFloat() > 0.5f) {
                    spawnSpiralBurst(sizeMultiplier = config.spiralSize)
                }
            }
            if (config.enableRingExplosion) {
                val ringProgress = elapsed / config.ringExplosionDuration.toFloat()
                if (ringProgress < 1f && Random.nextFloat() > 0.7f) {
                    spawnRingExplosion(sizeMultiplier = config.ringExplosionSize)
                }
            }
        }
        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                isPaused = false
                celebrationMode = 0
                fireworks.clear()
                screenShakeX = 0f
                screenShakeY = 0f
                screenFlashAlpha = 0f
            }
        })
        animator.start()
        celebrationAnimator = animator
        
        if (config.enableConfetti) {
            repeat(100 + config.mode * 60) { spawnConfetti(sizeMultiplier = config.confettiSize) }
        }
        if (config.enableGoldenRain) {
            repeat(80 + config.mode * 50) { spawnGoldenRainDrop(sizeMultiplier = config.goldenRainSize) }
        }
        if (config.enableFirework) {
            repeat(8 + config.mode * 4) { launchFirework(sizeMultiplier = config.fireworkSize) }
        }
        if (config.enableCelebrationParticles) {
            repeat(150 + config.mode * 80) { spawnCelebrationParticle(sizeMultiplier = config.celebrationParticlesSize) }
        }
        if (config.enableSpiral) {
            repeat(30 + config.mode * 20) { spawnSpiral(sizeMultiplier = config.spiralSize) }
        }
        if (config.enableRingExplosion) {
            repeat(3 + config.mode) { spawnRingExplosion(sizeMultiplier = config.ringExplosionSize) }
        }
        if (config.enableShockwave) {
            repeat(5 + config.mode) {
                shockwaves.add(Shockwave(
                    x = screenWidth / 2f + (Random.nextFloat() - 0.5f) * screenWidth * 0.3f,
                    y = screenHeight / 2f + (Random.nextFloat() - 0.5f) * screenHeight * 0.3f,
                    radius = 0f,
                    maxRadius = max(screenWidth, screenHeight) * (0.6f + Random.nextFloat() * 0.4f),
                    alpha = 1f,
                    color = listOf(
                        Color.parseColor("#FF1744"),
                        Color.parseColor("#FFEA00"),
                        Color.parseColor("#00E5FF"),
                        Color.parseColor("#D500F9"),
                        Color.parseColor("#FFD700"),
                        Color.parseColor("#76FF03"),
                        Color.parseColor("#FF6D00")
                    ).random()
                ))
            }
        }
        
        if (settingsManager.soundEnabled && config.mode >= 2) {
            soundManager.playFirework()
        }
    }
    
    data class Particle(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var color: Int, var size: Float, var life: Float, var decay: Float,
        var type: Int = 0
    )
    
    data class GoldenRay(
        var x: Float, var y: Float, var angle: Float,
        var length: Float, var alpha: Float, var decay: Float
    )
    
    data class Shockwave(
        var x: Float, var y: Float, var radius: Float,
        var maxRadius: Float, var alpha: Float, var color: Int
    )
    
    data class BaroqueOrnament(
        var x: Float, var y: Float, var size: Float,
        var rotation: Float, var alpha: Float, var type: Int
    )
    
    data class FloatingGoldParticle(
        var x: Float, var y: Float, var speed: Float,
        var size: Float, var alpha: Float, var phase: Float
    )
    
    data class NeonTextEffect(
        var text: String,
        var x: Float, var y: Float,
        var color: Int,
        var borderColor: Int = Color.WHITE,
        var life: Float, var decay: Float,
        var textSize: Float,
        var isRare: Boolean = false,
        var scale: Float = 0.3f,
        var targetScale: Float = 1.8f
    )
    
    data class CelebrationParticle(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var size: Float, var color: Int, var life: Float, var decay: Float,
        var type: Int = 0
    )
    
    data class ConfettiParticle(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var width: Float, var height: Float, var color: Int,
        var life: Float, var decay: Float, var rotation: Float, var rotSpeed: Float,
        var delay: Float = 0f
    )
    
    data class SpiralParticle(
        var centerX: Float, var centerY: Float,
        var angle: Float, var radius: Float, var radiusSpeed: Float,
        var angleSpeed: Float, var size: Float, var color: Int,
        var life: Float, var decay: Float
    )
    
    data class GoldenRainDrop(
        var x: Float, var y: Float, var vy: Float,
        var size: Float, var life: Float, var decay: Float,
        var shimmer: Float, var shimmerSpeed: Float,
        var initialVy: Float = 0f,
        var delay: Float = 0f
    )
    
    data class RingExplosion(
        var x: Float, var y: Float, var radius: Float,
        var maxRadius: Float, var alpha: Float, var color: Int,
        var lineWidth: Float, var ringCount: Int
    )
    
    enum class FireworkState { RISING, EXPLODING, EXPLODED }
    
    data class Firework(
        var x: Float, var y: Float, var targetY: Float,
        var color: Int,
        var state: FireworkState,
        var progress: Float,
        val particles: MutableList<FireworkParticle>,
        var sizeMultiplier: Float = 1f
    )
    
    data class FireworkParticle(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var color: Int, var life: Float, var decay: Float, var size: Float
    )
}
