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
import android.view.MotionEvent
import android.view.View
import com.bubblepop.game.manager.SettingsManager
import com.bubblepop.game.manager.SoundManager
import com.bubblepop.game.model.Bubble
import com.bubblepop.game.model.BubbleShape
import com.bubblepop.game.model.ExplosionType
import com.bubblepop.game.model.NEON_TEXTS
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
    private val blindBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
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
    
    // Press interaction state
    private var pressedBubble: Bubble? = null
    private var isPressing = false
    private var pressProgress = 0f
    
    // Effects state
    private var celebrationMode = 0
    private var celebrationProgress = 0f
    private var celebrationAnimator: ValueAnimator? = null
    private val particles = mutableListOf<Particle>()
    private val goldenRays = mutableListOf<GoldenRay>()
    private val shockwaves = mutableListOf<Shockwave>()
    private val neonTextEffects = mutableListOf<NeonTextEffect>()
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
        drawShockwaves(canvas)
        drawNeonTextEffects(canvas)
        
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
        
        // 属性光晕 - 根据爆炸属性显示不同颜色
        val breathe = (sin(bubble.glowPhase * PI / 180f).toFloat() + 1f) / 2f
        val attrGlowRadius = bubble.radius * (1.6f + breathe * 0.3f)
        val attrGlowAlpha = if (bubble.isHiddenRare) (120 + breathe * 80).toInt() else (70 + breathe * 40).toInt()
        val attrColor = bubble.explosionType.primaryColor
        val attrGlow = RadialGradient(drawX, drawY, attrGlowRadius, intArrayOf(attrGlowColor, Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        glowPaint.shader = attrGlow
        glowPaint.alpha = attrGlowAlpha
        canvas.drawCircle(drawX, drawY, attrGlowRadius, glowPaint)
        glowPaint.shader = null
        
        // 外层霓虹光晕
        val outerGlowRadius = bubble.radius * (1.5f + breathe * 0.3f)
        val outerGlowAlpha = (80 + breathe * 50).toInt()
        val outerGlow = RadialGradient(drawX, drawY, outerGlowRadius, intArrayOf(bubble.color, Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        glowPaint.shader = outerGlow
        glowPaint.alpha = outerGlowAlpha
        canvas.drawCircle(drawX, drawY, outerGlowRadius, glowPaint)
        glowPaint.shader = null
        
        // 发光球额外加强
        if (isGlowing) {
            val glowRadius = bubble.radius * (1.8f + breathe * 0.4f)
            val glowAlpha = (100 + breathe * 60).toInt()
            val glowGradient = RadialGradient(drawX, drawY, glowRadius, intArrayOf(lightenColor(bubble.color, 60), Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            glowPaint.shader = glowGradient
            glowPaint.alpha = glowAlpha
            canvas.drawCircle(drawX, drawY, glowRadius, glowPaint)
            glowPaint.shader = null
        }
        
        // 隐藏款旋转光环
        if (bubble.isHiddenRare) {
            canvas.save()
            canvas.translate(drawX, drawY)
            canvas.rotate(bubble.baroqueRotation * 2)
            blindBoxPaint.color = Color.argb(180, 255, 215, 0)
            blindBoxPaint.strokeWidth = 2f
            canvas.drawCircle(0f, 0f, bubble.radius + 8f, blindBoxPaint)
            canvas.restore()
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
        
        when (bubble.shape) {
            BubbleShape.CIRCLE -> canvas.drawCircle(drawX, drawY, bubble.radius, paint)
            BubbleShape.ELLIPSE -> {
                canvas.save()
                canvas.translate(drawX - bubble.x, drawY - bubble.y)
                drawEllipse(canvas, bubble)
                canvas.restore()
            }
        }
        
        // 巴洛克内部装饰
        canvas.save()
        canvas.translate(drawX - bubble.x, drawY - bubble.y)
        drawBaroqueOnBubble(canvas, bubble)
        canvas.restore()
        
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
            
            // 盲盒虚线框
            blindBoxPaint.color = Color.argb(qAlpha, 255, 215, 0)
            blindBoxPaint.strokeWidth = 2f
            blindBoxPaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f, 6f), 0f)
            canvas.drawCircle(drawX, drawY, bubble.radius + 5f, blindBoxPaint)
            blindBoxPaint.pathEffect = null
        }
        
        // 发光边框
        if (isGlowing) {
            val breathe2 = (sin(bubble.glowPhase * PI / 180f).toFloat() + 1f) / 2f
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f + breathe2 * 2f
            paint.alpha = (100 + breathe2 * 60).toInt()
            paint.color = Color.WHITE
            paint.shader = null
            canvas.drawCircle(drawX, drawY, bubble.radius + 3f, paint)
            paint.style = Paint.Style.FILL
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
        
        // 多层光环
        for (i in 3 downTo 0) {
            val ringRadius = bubble.radius * (1.3f + i * 0.25f + breathe * 0.15f)
            val ringAlpha = ((1f - i * 0.2f) * (80 + breathe * 60) * p).toInt()
            val ringColor = lightenColor(bubble.color, 40 + i * 20)
            
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f - i * 0.5f
            paint.alpha = ringAlpha
            paint.color = ringColor
            paint.shader = null
            canvas.drawCircle(bubble.x, bubble.y, ringRadius, paint)
        }
        
        // 旋转光晕
        val glowRadius = bubble.radius * (1.8f + breathe * 0.5f)
        val glowGradient = RadialGradient(
            bubble.x, bubble.y, glowRadius,
            intArrayOf(lightenColor(bubble.color, 100), bubble.color, Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        glowPaint.shader = glowGradient
        glowPaint.alpha = (120 + breathe * 80).toInt()
        canvas.drawCircle(bubble.x, bubble.y, glowRadius, glowPaint)
        glowPaint.shader = null
        
        // 脉冲波纹
        val pulsePhase = (System.currentTimeMillis() % 800) / 800f
        val pulseRadius = bubble.radius * (1f + pulsePhase * 1.2f)
        val pulseAlpha = ((1f - pulsePhase) * 100).toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.alpha = pulseAlpha
        paint.color = Color.WHITE
        canvas.drawCircle(bubble.x, bubble.y, pulseRadius, paint)
        
        // 球体本身 - 压缩变形
        val compressScale = 1f - p * 0.15f
        canvas.save()
        canvas.translate(bubble.x, bubble.y)
        canvas.scale(1f / compressScale, compressScale)
        
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
        
        // 内部巴洛克装饰
        canvas.restore()
        drawBaroqueOnBubble(canvas, bubble)
        
        // 高光
        highlightPaint.color = Color.WHITE
        highlightPaint.alpha = 120
        canvas.drawOval(
            bubble.x - bubble.radius * 0.3f,
            bubble.y - bubble.radius * 0.45f,
            bubble.x,
            bubble.y - bubble.radius * 0.15f,
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
        
        // 冲击波
        if (progress < 0.5f) {
            val shockAlpha = ((1f - progress * 2) * 150).toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f * (1f - progress)
            paint.alpha = shockAlpha
            paint.color = Color.WHITE
            canvas.drawCircle(bubble.x, bubble.y, expandedRadius * 1.5f, paint)
        }
        
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
        // 盲盒球检查 - 未达到目标大小不爆炸
        if (bubble.isBlindBox && bubble.radius < bubble.blindBoxTargetRadius) {
            // 提示效果：闪烁+震动
            if (settingsManager.vibrationEnabled) vibrate(20L)
            spawnBlindBoxHint(bubble)
            return
        }
        
        bubble.isPopped = true
        bubble.popProgress = 0f
        
        val isBig = bubble.radius >= maxRadius * 0.8f
        val isRare = bubble.isHiddenRare
        
        if (settingsManager.soundEnabled) {
            soundManager.playPop(isBig || isRare)
        }
        
        if (settingsManager.vibrationEnabled) {
            val duration = if (isRare) 120L else if (isBig) 80L else 30L
            vibrate(duration)
        }
        
        // 触发对应属性爆炸特效
        spawnExplosionParticles(bubble)
        spawnShockwave(bubble)
        
        // 霓虹文字特效
        spawnNeonTextEffect(bubble)
        
        checkMilestones()
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
        
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = when (celebrationMode) {
            1 -> 2500L
            2 -> 4000L
            3 -> 6000L
            else -> 2000L
        }
        animator.addUpdateListener { anim ->
            celebrationProgress = anim.animatedValue as Float
            spawnCelebrationParticles()
        }
        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                isPaused = false
                celebrationMode = 0
            }
        })
        animator.start()
        celebrationAnimator = animator
        
        if (celebrationMode >= 2) {
            repeat(24) {
                goldenRays.add(GoldenRay(
                    x = screenWidth / 2f,
                    y = screenHeight / 2f,
                    angle = it * 15f,
                    length = 0f,
                    alpha = 1f,
                    decay = 0.006f
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
            Color.parseColor("#FF1744"),
            Color.parseColor("#FF6D00"),
            Color.parseColor("#FFEA00"),
            Color.parseColor("#76FF03"),
            Color.parseColor("#00E5FF"),
            Color.parseColor("#2979FF"),
            Color.parseColor("#D500F9"),
            Color.parseColor("#FF4081"),
            Color.parseColor("#E040FB"),
            Color.parseColor("#00CED1")
        )
        
        particles.add(Particle(
            x = Random.nextFloat() * screenWidth,
            y = screenHeight + 10f,
            vx = Random.nextFloat() * 8f - 4f,
            vy = -(Random.nextFloat() * 15f + 10f),
            color = colors.random(),
            size = Random.nextFloat() * 8f + 3f,
            life = 1f,
            decay = Random.nextFloat() * 0.012f + 0.006f,
            type = Random.nextInt(3)
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
        
        val progress = celebrationProgress
        
        if (celebrationMode >= 2) {
            for (ray in goldenRays) {
                ray.length = progress * screenWidth * 0.9f
                ray.alpha = 1f - progress * 0.7f
                
                val rad = ray.angle * PI / 180f
                val endX = ray.x + cos(rad).toFloat() * ray.length
                val endY = ray.y + sin(rad).toFloat() * ray.length
                
                paint.color = Color.argb((ray.alpha * 200).toInt(), 255, 255, 255)
                paint.strokeWidth = 4f + progress * 6f
                paint.style = Paint.Style.STROKE
                canvas.drawLine(ray.x, ray.y, endX, endY, paint)
            }
        }
        
        if (celebrationMode >= 3) {
            val breathe = (sin(progress * PI * 4f).toFloat() + 1f) / 2f
            paint.color = Color.argb((breathe * 60).toInt(), 255, 100, 200)
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
        for (ray in goldenRays) {
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
        
        for (sw in shockwaves) {
            val progress = 1f - sw.alpha
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
        
        neonTextEffects.add(NeonTextEffect(
            text = text,
            x = bubble.x,
            y = bubble.y,
            color = color,
            life = 1f,
            decay = if (isRare) 0.008f else 0.012f,
            textSize = if (isRare) 48f else 36f,
            isRare = isRare,
            scale = 0.3f,
            targetScale = if (isRare) 2.5f else 1.8f
        ))
        
        // 隐藏款额外加一个全屏特写
        if (isRare) {
            neonTextEffects.add(NeonTextEffect(
                text = "隐藏款!",
                x = screenWidth / 2f,
                y = screenHeight / 2f,
                color = Color.parseColor("#FFD700"),
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
        
        for (effect in neonTextEffects) {
            effect.life -= effect.decay
            effect.scale += (effect.targetScale - effect.scale) * 0.15f
            
            val alpha = (effect.life * 255).toInt()
            val currentSize = effect.textSize * effect.scale
            
            neonTextPaint.textSize = currentSize
            neonTextPaint.alpha = alpha
            
            // 霓虹描边效果
            neonTextPaint.style = Paint.Style.STROKE
            neonTextPaint.strokeWidth = currentSize * 0.08f
            neonTextPaint.color = Color.WHITE
            canvas.drawText(effect.text, effect.x, effect.y, neonTextPaint)
            
            // 内部填充
            neonTextPaint.style = Paint.Style.FILL
            neonTextPaint.color = effect.color
            canvas.drawText(effect.text, effect.x, effect.y, neonTextPaint)
            
            // 外发光
            neonTextPaint.style = Paint.Style.STROKE
            neonTextPaint.strokeWidth = currentSize * 0.15f
            neonTextPaint.alpha = alpha / 3
            neonTextPaint.color = effect.color
            canvas.drawText(effect.text, effect.x, effect.y, neonTextPaint)
            
            neonTextPaint.style = Paint.Style.FILL
            neonTextPaint.alpha = 255
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
        shockwaves.clear()
        neonTextEffects.clear()
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
        var life: Float, var decay: Float,
        var textSize: Float,
        var isRare: Boolean = false,
        var scale: Float = 0.3f,
        var targetScale: Float = 1.8f
    )
}
