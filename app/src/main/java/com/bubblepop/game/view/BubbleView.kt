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
import com.bubblepop.game.model.BubbleShape
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
    private val bubblePath = Path()
    
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
        
        if (bubbles.isEmpty() && !isGameOver) {
            repeat(MIN_BUBBLES) {
                bubbles.add(Bubble.createRandom(w, h, 20f, maxRadius * 0.5f))
            }
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        drawBackground(canvas)
        drawBaroqueDecorations(canvas)
        drawFloatingGoldParticles(canvas)
        drawEdgeGlow(canvas)
        
        if (!isGameOver) {
            bubbles.removeAll { it.isPopped && it.popProgress >= 1f }
            
            val shouldPauseGrowth = isPressing && pressedBubble != null
            
            for (bubble in bubbles) {
                if (bubble.isPopped) {
                    bubble.popProgress += 0.04f
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
                pressProgress = min(1f, pressProgress + 0.03f)
                pressGlowAlpha = sin(pressProgress * PI / 2f).toFloat()
            }
            
            handleBubbleCollisions()
            
            val now = System.currentTimeMillis()
            if (bubbles.count { !it.isPopped } < MIN_BUBBLES && now - lastSpawnTime > 500) {
                bubbles.add(Bubble.createRandom(screenWidth, screenHeight, 20f, maxRadius * 0.5f))
                lastSpawnTime = now
            }
            
            val activeBubbles = bubbles.filter { !it.isPopped }
            if (activeBubbles.size >= MAX_BUBBLES && 
                activeBubbles.all { it.radius >= maxRadius * 0.8f }) {
                triggerGameOver()
            }
        }
        
        drawCelebration(canvas)
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
        val borderWidth = 10f
        val goldColor = Color.argb(100, 218, 165, 32)
        
        paint.color = goldColor
        paint.strokeWidth = borderWidth
        paint.style = Paint.Style.STROKE
        
        canvas.drawRect(
            borderWidth / 2, borderWidth / 2,
            screenWidth - borderWidth / 2, screenHeight - borderWidth / 2,
            paint
        )
        
        paint.strokeWidth = 3f
        paint.alpha = 60
        canvas.drawRect(
            borderWidth + 6, borderWidth + 6,
            screenWidth - borderWidth - 6, screenHeight - borderWidth - 6,
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
        paint.color = Color.argb(120, 218, 165, 32)
        paint.strokeWidth = 2.5f
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
            
            paint.color = Color.argb((alpha * 180).toInt(), 255, 215, 0)
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
            
            val glowColor = Color.argb((edgeGlowAlpha * 80).toInt(), 218, 165, 32)
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
    
    private fun drawBubble(canvas: Canvas, bubble: Bubble) {
        val isGlowing = bubble.radius >= maxRadius * 0.8f
        
        if (isGlowing) {
            val breathe = (sin(bubble.glowPhase * PI / 180f).toFloat() + 1f) / 2f
            val glowRadius = bubble.radius * (1.3f + breathe * 0.3f)
            val glowAlpha = (60 + breathe * 40).toInt()
            
            val glowGradient = RadialGradient(bubble.x, bubble.y, glowRadius, bubble.color, Color.TRANSPARENT, Shader.TileMode.CLAMP)
            glowPaint.shader = glowGradient
            glowPaint.alpha = glowAlpha
            canvas.drawCircle(bubble.x, bubble.y, glowRadius, glowPaint)
            glowPaint.shader = null
        }
        
        val gradient = RadialGradient(
            bubble.x - bubble.radius * 0.3f, bubble.y - bubble.radius * 0.3f,
            bubble.radius, lightenColor(bubble.color, 60), bubble.color, Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        paint.alpha = (bubble.alpha * 200).toInt()
        
        when (bubble.shape) {
            BubbleShape.CIRCLE -> canvas.drawCircle(bubble.x, bubble.y, bubble.radius, paint)
            BubbleShape.ELLIPSE -> drawEllipse(canvas, bubble)
            BubbleShape.IRREGULAR -> drawIrregularBubble(canvas, bubble)
        }
        
        highlightPaint.color = Color.WHITE
        highlightPaint.alpha = if (isGlowing) 80 else 50
        canvas.drawCircle(bubble.x - bubble.radius * 0.25f, bubble.y - bubble.radius * 0.25f, bubble.radius * 0.3f, highlightPaint)
        
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
    
    private fun drawEllipse(canvas: Canvas, bubble: Bubble) {
        canvas.save()
        canvas.translate(bubble.x, bubble.y)
        canvas.rotate(bubble.ellipseAngle)
        canvas.scale(1f, bubble.ellipseRatio)
        canvas.drawCircle(0f, 0f, bubble.radius, paint)
        canvas.restore()
    }
    
    private fun drawIrregularBubble(canvas: Canvas, bubble: Bubble) {
        bubblePath.reset()
        val points = bubble.irregularPoints
        val numPoints = points.size
        if (numPoints == 0) {
            canvas.drawCircle(bubble.x, bubble.y, bubble.radius, paint)
            return
        }
        
        for (i in 0 until numPoints) {
            val angle = i * (360f / numPoints)
            val rad = angle * PI / 180f
            val r = bubble.radius * points[i]
            val px = bubble.x + cos(rad).toFloat() * r
            val py = bubble.y + sin(rad).toFloat() * r
            
            if (i == 0) bubblePath.moveTo(px, py)
            else {
                val prevAngle = (i - 1) * (360f / numPoints)
                val prevRad = prevAngle * PI / 180f
                val prevR = bubble.radius * points[i - 1]
                val cpx = bubble.x + cos((prevAngle + angle) / 2 * PI / 180f).toFloat() * (prevR + r) / 2f * 1.1f
                val cpy = bubble.y + sin((prevAngle + angle) / 2 * PI / 180f).toFloat() * (prevR + r) / 2f * 1.1f
                bubblePath.quadTo(cpx, cpy, px, py)
            }
        }
        bubblePath.close()
        canvas.drawPath(bubblePath, paint)
    }
    
    private fun drawPressedBubble(canvas: Canvas, bubble: Bubble) {
        val breathe = (sin(System.currentTimeMillis() / 200.0).toFloat() + 1f) / 2f
        val glowRadius = bubble.radius * (1.5f + breathe * 0.4f)
        val glowAlpha = (100 + breathe * 80).toInt()
        
        val glowGradient = RadialGradient(bubble.x, bubble.y, glowRadius, bubble.color, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        glowPaint.shader = glowGradient
        glowPaint.alpha = glowAlpha
        canvas.drawCircle(bubble.x, bubble.y, glowRadius, glowPaint)
        glowPaint.shader = null
        
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.alpha = (150 + breathe * 80).toInt()
        paint.color = bubble.color
        canvas.drawCircle(bubble.x, bubble.y, bubble.radius + 8f + breathe * 5f, paint)
        
        val innerGrad = RadialGradient(bubble.x, bubble.y, bubble.radius, lightenColor(bubble.color, 80), bubble.color, Shader.TileMode.CLAMP)
        paint.shader = innerGrad
        paint.alpha = 220
        paint.style = Paint.Style.FILL
        
        when (bubble.shape) {
            BubbleShape.CIRCLE -> canvas.drawCircle(bubble.x, bubble.y, bubble.radius, paint)
            BubbleShape.ELLIPSE -> drawEllipse(canvas, bubble)
            BubbleShape.IRREGULAR -> drawIrregularBubble(canvas, bubble)
        }
        
        paint.shader = null
        highlightPaint.color = Color.WHITE
        highlightPaint.alpha = 100
        canvas.drawCircle(bubble.x - bubble.radius * 0.2f, bubble.y - bubble.radius * 0.2f, bubble.radius * 0.25f, highlightPaint)
        paint.style = Paint.Style.FILL
    }
    
    private fun drawPoppingBubble(canvas: Canvas, bubble: Bubble) {
        val progress = bubble.popProgress
        val expandedRadius = bubble.radius * (1 + progress * 1.5f)
        val alpha = (200 * (1 - progress)).toInt()
        
        paint.color = bubble.color
        paint.alpha = alpha
        canvas.drawCircle(bubble.x, bubble.y, expandedRadius, paint)
        
        val particleCount = if (bubble.radius >= maxRadius * 0.8f) 16 else 8
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
        
        val isBig = bubble.radius >= maxRadius * 0.8f
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
            val duration = if (isBig) 80L else 30L
            vibrate(duration)
        }
        
        spawnPopParticles(bubble)
        checkMilestones()
    }
    
    private fun calculateScore(bubble: Bubble): Int {
        return when {
            bubble.radius >= maxRadius -> 3
            bubble.radius >= maxRadius * 0.8f -> 1
            else -> 0
        }
    }
    
    private fun checkMilestones() {
        when {
            score >= 100 && (score - lastMilestoneScore) >= 100 -> {
                lastMilestoneScore = score / 100 * 100
                celebrationMode = 3
                triggerCelebration()
            }
            score >= 50 && lastMilestoneScore < 50 -> {
                lastMilestoneScore = 50
                celebrationMode = 2
                triggerCelebration()
            }
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
                celebrationProgress = it.animatedValue as? Float ?: 0f
                spawnCelebrationParticles()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isPaused = false
                    celebrationMode = 0
                }
            })
            start()
        }
        
        if (celebrationMode >= 2) {
            repeat(12) {
                goldenRays.add(GoldenRay(
                    x = screenWidth / 2f,
                    y = screenHeight / 2f,
                    angle = it * 30f,
                    length = 0f,
                    alpha = 1f,
                    decay = 0.008f
                ))
            }
        }
        
        if (settingsManager.soundEnabled) {
            if (celebrationMode >= 2) soundManager.playFirework()
            if (celebrationMode >= 3) soundManager.playCheer()
        }
    }
    
    private fun spawnCelebrationParticles() {
        if (Random.nextFloat() > 0.3f) return
        
        val colors = listOf(
            Color.parseColor("#FFD700"),
            Color.parseColor("#FF69B4"),
            Color.parseColor("#00BFFF"),
            Color.parseColor("#FF6347"),
            Color.parseColor("#7FFF00"),
            Color.parseColor("#FF4500"),
            Color.parseColor("#9370DB")
        )
        
        particles.add(Particle(
            x = Random.nextFloat() * screenWidth,
            y = screenHeight + 10f,
            vx = Random.nextFloat() * 6f - 3f,
            vy = -(Random.nextFloat() * 12f + 8f),
            color = colors.random(),
            size = Random.nextFloat() * 6f + 2f,
            life = 1f,
            decay = Random.nextFloat() * 0.015f + 0.008f
        ))
    }
    
    private fun spawnPopParticles(bubble: Bubble) {
        val count = if (bubble.radius >= maxRadius * 0.8f) 20 else 10
        repeat(count) {
            val angle = Random.nextFloat() * 360f
            val rad = angle * PI / 180f
            val speed = Random.nextFloat() * 8f + 3f
            particles.add(Particle(
                x = bubble.x,
                y = bubble.y,
                vx = cos(rad).toFloat() * speed,
                vy = sin(rad).toFloat() * speed,
                color = bubble.color,
                size = Random.nextFloat() * 5f + 2f,
                life = 1f,
                decay = Random.nextFloat() * 0.02f + 0.015f
            ))
        }
    }
    
    private fun drawCelebration(canvas: Canvas) {
        if (celebrationMode == 0) return
        
        val progress = celebrationProgress
        
        if (celebrationMode >= 2) {
            for (ray in goldenRays) {
                ray.length = progress * screenWidth * 0.8f
                ray.alpha = 1f - progress * 0.8f
                
                val rad = ray.angle * PI / 180f
                val endX = ray.x + cos(rad).toFloat() * ray.length
                val endY = ray.y + sin(rad).toFloat() * ray.length
                
                paint.color = Color.argb((ray.alpha * 120).toInt(), 255, 215, 0)
                paint.strokeWidth = 3f + progress * 5f
                paint.style = Paint.Style.STROKE
                canvas.drawLine(ray.x, ray.y, endX, endY, paint)
            }
        }
        
        if (celebrationMode >= 3) {
            val breathe = (sin(progress * PI * 4f).toFloat() + 1f) / 2f
            paint.color = Color.argb((breathe * 40).toInt(), 255, 215, 0)
            paint.style = Paint.Style.FILL
            canvas.drawColor(paint.color)
        }
        
        paint.style = Paint.Style.FILL
    }
    
    private fun drawParticles(canvas: Canvas) {
        particles.removeAll { it.life <= 0 }
        
        for (p in particles) {
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.15f
            p.life -= p.decay
            
            paint.color = p.color
            paint.alpha = (p.life * 200).toInt()
            canvas.drawCircle(p.x, p.y, p.size * p.life, paint)
        }
        
        goldenRays.removeAll { it.alpha <= 0 }
        for (ray in goldenRays) {
            ray.alpha -= ray.decay
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
            bubbles.add(Bubble.createRandom(screenWidth, screenHeight, 20f, maxRadius * 0.5f))
        }
    }
    
    fun getScore(): Int = score
    fun getTotalScore(): Int = totalScore
    fun getHighScores(): List<Int> = settingsManager.getHighScores()
    
    data class Particle(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var color: Int, var size: Float, var life: Float, var decay: Float
    )
    
    data class GoldenRay(
        var x: Float, var y: Float, var angle: Float,
        var length: Float, var alpha: Float, var decay: Float
    )
    
    data class BaroqueOrnament(
        var x: Float, var y: Float, var size: Float,
        var rotation: Float, var alpha: Float, var type: Int
    )
    
    data class FloatingGoldParticle(
        var x: Float, var y: Float, var speed: Float,
        var size: Float, var alpha: Float, var phase: Float
    )
}
