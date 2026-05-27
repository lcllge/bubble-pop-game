package com.bubblepop.game.model

import android.graphics.Color
import kotlin.random.Random

enum class BubbleShape {
    CIRCLE, ELLIPSE
}

// 爆炸属性类型
enum class ExplosionType(val label: String, val primaryColor: Int, val secondaryColor: Int, val particleColor: Int) {
    FIRE("火焰", Color.parseColor("#FF1744"), Color.parseColor("#FF6D00"), Color.parseColor("#FFEA00")),
    LIGHTNING("雷电", Color.parseColor("#FFEA00"), Color.parseColor("#FFFFFF"), Color.parseColor("#00E5FF")),
    THUNDER("雷霆", Color.parseColor("#7C4DFF"), Color.parseColor("#FFFFFF"), Color.parseColor("#E040FB")),
    WIND("风暴", Color.parseColor("#00E676"), Color.parseColor("#69F0AE"), Color.parseColor("#B9F6CA")),
    RAIN("暴雨", Color.parseColor("#2979FF"), Color.parseColor("#448AFF"), Color.parseColor("#82B1FF")),
    DARK("暗影", Color.parseColor("#651FFF"), Color.parseColor("#304FFE"), Color.parseColor("#1A237E")),
    LIGHT("圣光", Color.parseColor("#FFFFFF"), Color.parseColor("#FFF8E1"), Color.parseColor("#FFD54F")),
    ICE("冰霜", Color.parseColor("#00E5FF"), Color.parseColor("#84FFFF"), Color.parseColor("#E0F7FA")),
    VOID("虚空", Color.parseColor("#212121"), Color.parseColor("#424242"), Color.parseColor("#757575")),
    STAR("星辰", Color.parseColor("#FFD700"), Color.parseColor("#FF69B4"), Color.parseColor("#00BFFF"));

    companion object {
        fun random(): ExplosionType = values().random()
    }
}

// 霓虹文字特效
val NEON_TEXTS = listOf(
    "太带派了\n老铁!",
    "绝绝子!",
    "泰酷辣!",
    "遥遥领先!",
    "赢麻了!",
    "666!",
    "炸裂!",
    "封神了!",
    "逆天!",
    "爽爆了!",
    "YYDS!",
    "绝杀!",
    "暴击!",
    "超神!",
    "无敌!"
)

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
    var baroquePattern: Int = Random.nextInt(4),
    // 爆炸属性
    var explosionType: ExplosionType = ExplosionType.FIRE,
    var isBlindBox: Boolean = false,
    var blindBoxTargetRadius: Float = 0f,
    var isHiddenRare: Boolean = false,
    var shakePhase: Float = 0f,
    var neonText: String = ""
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
            
            // 15%概率生成盲盒球
            val isBlindBox = Random.nextFloat() < 0.15f
            val blindBoxTarget = if (isBlindBox) {
                // 盲盒球需要长到随机目标大小才爆炸
                Random.nextFloat() * (maxRadius - radius) + radius
            } else 0f
            
            // 5%概率生成隐藏款
            val isHiddenRare = Random.nextFloat() < 0.05f
            
            val explosionType = if (isHiddenRare) {
                // 隐藏款必定是稀有属性
                listOf(ExplosionType.VOID, ExplosionType.STAR, ExplosionType.THUNDER).random()
            } else {
                ExplosionType.random()
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
                baroqueRotation = Random.nextFloat() * 360f,
                baroquePattern = Random.nextInt(4),
                explosionType = explosionType,
                isBlindBox = isBlindBox,
                blindBoxTargetRadius = blindBoxTarget,
                isHiddenRare = isHiddenRare,
                shakePhase = Random.nextFloat() * 360f,
                neonText = NEON_TEXTS.random()
            )
        }
    }
}
