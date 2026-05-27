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
        // 艺术配色：莫兰迪色系 + 宝石色系，强迫症友好
        private val BUBBLE_COLORS = listOf(
            Color.parseColor("#E8A87C"), // 珊瑚橙
            Color.parseColor("#D4A574"), // 焦糖棕
            Color.parseColor("#85CDCA"), // 薄荷绿
            Color.parseColor("#C38D9E"), // 玫瑰粉
            Color.parseColor("#A3D1C6"), // 翡翠绿
            Color.parseColor("#E27D60"), // 番茄红
            Color.parseColor("#E8B4B8"), // 樱花粉
            Color.parseColor("#A8D8EA"), // 天空蓝
            Color.parseColor("#C9B1FF"), // 紫罗兰
            Color.parseColor("#F6E6CB"), // 奶油黄
            Color.parseColor("#FFB7B2"), // 蜜桃粉
            Color.parseColor("#B5EAD7"), // 薄荷蓝
            Color.parseColor("#FFDAC1"), // 杏桃色
            Color.parseColor("#E2F0CB"), // 青柠绿
            Color.parseColor("#F3E8FF"), // 薰衣草紫
            Color.parseColor("#FF9AA2"), // 珊瑚红
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
