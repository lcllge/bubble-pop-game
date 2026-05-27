package com.bubblepop.game.model

import android.graphics.Color
import kotlin.random.Random

enum class BubbleShape {
    CIRCLE, ELLIPSE, IRREGULAR
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
    var irregularPoints: List<Float> = emptyList()
) {
    companion object {
        private val BUBBLE_COLORS = listOf(
            Color.parseColor("#FFB3A7"),
            Color.parseColor("#FFD4A5"),
            Color.parseColor("#FFF3C4"),
            Color.parseColor("#B8E6B8"),
            Color.parseColor("#A5C8E6"),
            Color.parseColor("#D4A5E6"),
            Color.parseColor("#FFB3D9"),
            Color.parseColor("#A5E6E6")
        )
        
        fun createRandom(screenWidth: Int, screenHeight: Int, minRadius: Float = 20f, maxRadius: Float = 60f): Bubble {
            val radius = Random.nextFloat() * (maxRadius - minRadius) + minRadius
            val margin = radius + 10f
            
            val shape = BubbleShape.entries.random()
            val ellipseRatio = if (shape == BubbleShape.ELLIPSE) Random.nextFloat() * 0.4f + 0.6f else 1f
            val ellipseAngle = if (shape == BubbleShape.ELLIPSE) Random.nextFloat() * 180f else 0f
            
            val irregularPoints = if (shape == BubbleShape.IRREGULAR) {
                List(8) { Random.nextFloat() * 0.3f + 0.85f }
            } else {
                emptyList()
            }
            
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
                irregularPoints = irregularPoints
            )
        }
    }
}
