package com.bubblepop.game.model

import android.graphics.Color
import kotlin.random.Random

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
    var birthTime: Long = System.currentTimeMillis()
) {
    companion object {
        private val BUBBLE_COLORS = listOf(
            Color.parseColor("#FF6B6B"),
            Color.parseColor("#FFA500"),
            Color.parseColor("#FFD93D"),
            Color.parseColor("#6BCB77"),
            Color.parseColor("#4D96FF"),
            Color.parseColor("#9B59B6"),
            Color.parseColor("#FF69B4"),
            Color.parseColor("#00BCD4"),
            Color.parseColor("#E91E63"),
            Color.parseColor("#8BC34A")
        )
        
        fun createRandom(screenWidth: Int, screenHeight: Int, minRadius: Float = 20f, maxRadius: Float = 60f): Bubble {
            val radius = Random.nextFloat() * (maxRadius - minRadius) + minRadius
            val margin = radius + 10f
            return Bubble(
                x = Random.nextFloat() * (screenWidth - 2 * margin) + margin,
                y = Random.nextFloat() * (screenHeight - 2 * margin) + margin,
                radius = radius,
                initialRadius = radius,
                color = BUBBLE_COLORS.random(),
                velocityX = Random.nextFloat() * 1f - 0.5f,
                velocityY = Random.nextFloat() * 0.6f - 0.3f
            )
        }
    }
}
