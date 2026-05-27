package com.bubblepop.game.model

import android.graphics.Color
import kotlin.random.Random

enum class BubbleShape {
    CIRCLE, ELLIPSE
}

data class Bubble(
    var x: Float,
    var y: Float,
    var radius: Float,
    val initialRadius: Float,
    var color: Int,
    var velocityX: Float = 0f,
    var velocityY: Float = 0f,
    var alpha: Float = 1f,
    var isPopped: Boolean = false,
    var popProgress: Float = 0f,
    var wobblePhase: Float = Random.nextFloat() * 360f,
    var wobbleSpeed: Float = Random.nextFloat() * 2f + 1f,
    var wobbleAmplitude: Float = Random.nextFloat() * 3f + 1f,
    var glowPhase: Float = Random.nextFloat() * 360f,
    var birthTime: Long = System.currentTimeMillis(),
    var shape: BubbleShape = BubbleShape.CIRCLE,
    var ellipseRatio: Float = 1f,
    var ellipseAngle: Float = 0f,
    var baroqueRotation: Float = Random.nextFloat() * 360f,
    var baroquePattern: Int = Random.nextInt(4)
) {
    companion object {
        // 涂鸦风格：高饱和度霓虹色系，强色彩冲击力
        private val BUBBLE_COLORS = listOf(
            Color.parseColor("#FF1744"), // 霓虹红
            Color.parseColor("#FF6D00"), // 活力橙
            Color.parseColor("#FFEA00"), // 荧光黄
            Color.parseColor("#76FF03"), // 荧光绿
            Color.parseColor("#00E5FF"), // 电光蓝
            Color.parseColor("#2979FF"), // 宝石蓝
            Color.parseColor("#D500F9"), // 霓虹紫
            Color.parseColor("#FF4081"), // 荧光粉
            Color.parseColor("#F50057"), // 玫红
            Color.parseColor("#00E676"), // 翠绿
            Color.parseColor("#FFAB00"), // 琥珀橙
            Color.parseColor("#651FFF"), // 电光紫
            Color.parseColor("#00BFA5"), // 青碧
            Color.parseColor("#FF3D00"), // 火焰红
            Color.parseColor("#E040FB"), // 亮紫
            Color.parseColor("#00B0FF"), // 天蓝
        )
        
        fun createRandom(screenWidth: Int, screenHeight: Int, minRadius: Float = 20f, maxRadius: Float = 60f): Bubble {
            val radius = Random.nextFloat() * (maxRadius - minRadius) + minRadius
            val margin = radius + 10f
            
            val shape = if (Random.nextFloat() > 0.5f) BubbleShape.CIRCLE else BubbleShape.ELLIPSE
            val ellipseRatio = if (shape == BubbleShape.ELLIPSE) Random.nextFloat() * 0.35f + 0.65f else 1f
            val ellipseAngle = if (shape == BubbleShape.ELLIPSE) Random.nextFloat() * 180f else 0f
            
            return Bubble(
                x = Random.nextFloat() * (screenWidth - 2 * margin) + margin,
                y = Random.nextFloat() * (screenHeight - 2 * margin) + margin,
                radius = radius,
                initialRadius = radius,
                color = BUBBLE_COLORS.random(),
                velocityX = Random.nextFloat() * 1f - 0.5f,
                velocityY = Random.nextFloat() * 0.6f - 0.3f,
                shape = shape,
                ellipseRatio = ellipseRatio,
                ellipseAngle = ellipseAngle,
                baroqueRotation = Random.nextFloat() * 360f,
                baroquePattern = Random.nextInt(4)
            )
        }
    }
}
