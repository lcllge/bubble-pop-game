package com.bubblepop.game.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.bubblepop.game.model.ExplosionType
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class SoundManager(private val context: Context) {
    
    private val soundPool: SoundPool
    private var popSoundId: Int = -1
    private var bigPopSoundId: Int = -1
    private var fireworkSoundId: Int = -1
    private var cheerSoundId: Int = -1
    private var loaded = false
    
    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()
        
        popSoundId = soundPool.load(generatePopSound(), 1)
        bigPopSoundId = soundPool.load(generateBigPopSound(), 1)
        fireworkSoundId = soundPool.load(generateFireworkSound(), 1)
        cheerSoundId = soundPool.load(generateCheerSound(), 1)
        
        soundPool.setOnLoadCompleteListener { _, _, status ->
            loaded = status == 0
        }
    }
    
    private fun generatePopSound(): String {
        val sampleRate = 22050
        val duration = 0.08f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        
        for (i in samples.indices) {
            val t = i.toFloat() / sampleRate
            val freq = 800f - t * 6000f
            val envelope = exp(-t * 60).toFloat()
            val noise = (Random.nextFloat() * 2f - 1f) * 0.3f
            val value = (sin(2 * PI * freq * t.toDouble()).toFloat() + noise) * envelope
            samples[i] = (value * Short.MAX_VALUE).toInt().toShort()
        }
        
        return writeWav(samples, sampleRate)
    }
    
    private fun generateBigPopSound(): String {
        val sampleRate = 22050
        val duration = 0.25f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        
        for (i in samples.indices) {
            val t = i.toFloat() / sampleRate
            val freq = 400f - t * 2000f
            val envelope = exp(-t * 20).toFloat()
            val harmonic = sin(2 * PI * freq * 1.5f * t.toDouble()).toFloat() * 0.5f
            val noise = (Random.nextFloat() * 2f - 1f) * 0.2f
            val value = (sin(2 * PI * freq * t.toDouble()).toFloat() + harmonic + noise) * envelope
            samples[i] = (value * Short.MAX_VALUE * 0.8f).toInt().toShort()
        }
        
        return writeWav(samples, sampleRate)
    }
    
    private fun generateFireworkSound(): String {
        val sampleRate = 22050
        val duration = 0.6f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        
        for (i in samples.indices) {
            val t = i.toFloat() / sampleRate
            val envelope = exp(-t * 8).toFloat()
            val crackle = (Random.nextFloat() * 2f - 1f) * 0.6f
            val boom = sin(2 * PI * 150 * t.toDouble()).toFloat() * exp(-t * 15).toFloat()
            val sparkle = sin(2 * PI * 2000 * t.toDouble() + Random.nextFloat() * PI * 2).toFloat() * 0.1f * exp(-t * 30).toFloat()
            val value = (crackle + boom + sparkle) * envelope
            samples[i] = (value * Short.MAX_VALUE * 0.7f).toInt().toShort()
        }
        
        return writeWav(samples, sampleRate)
    }
    
    private fun generateCheerSound(): String {
        val sampleRate = 22050
        val duration = 1.5f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        
        val notes = listOf(523f, 659f, 784f, 1047f, 784f, 1047f, 1319f)
        val noteDuration = duration / notes.size
        
        for (i in samples.indices) {
            val t = i.toFloat() / sampleRate
            val noteIndex = (t / noteDuration).toInt().coerceIn(0, notes.size - 1)
            val noteTime = t - noteIndex * noteDuration
            val freq = notes[noteIndex]
            val envelope = exp(-noteTime * 10).toFloat()
            
            val fundamental = sin(2 * PI * freq * t.toDouble()).toFloat()
            val harmonic = sin(2 * PI * freq * 2 * t.toDouble()).toFloat() * 0.3f
            val noise = (Random.nextFloat() * 2f - 1f) * 0.1f * envelope
            val value = (fundamental + harmonic) * envelope + noise
            samples[i] = (value * Short.MAX_VALUE * 0.5f).toInt().toShort()
        }
        
        return writeWav(samples, sampleRate)
    }
    
    private fun writeWav(samples: ShortArray, sampleRate: Int): String {
        val numSamples = samples.size
        val file = java.io.File.createTempFile("sound_", ".wav", context.cacheDir)
        file.deleteOnExit()
        
        file.outputStream().use { out ->
            val byteRate = sampleRate * 2
            val blockAlign = 2
            val dataSize = numSamples * 2
            val totalSize = 36 + dataSize
            
            out.write("RIFF".toByteArray())
            out.write(intToLittleEndian(totalSize))
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray())
            out.write(intToLittleEndian(16))
            out.write(shortToLittleEndian(1))
            out.write(shortToLittleEndian(1))
            out.write(intToLittleEndian(sampleRate))
            out.write(intToLittleEndian(byteRate))
            out.write(shortToLittleEndian(blockAlign.toShort()))
            out.write(shortToLittleEndian(16))
            out.write("data".toByteArray())
            out.write(intToLittleEndian(dataSize))
            
            for (sample in samples) {
                out.write(shortToLittleEndian(sample))
            }
        }
        
        return file.absolutePath
    }
    
    private fun intToLittleEndian(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }
    
    private fun shortToLittleEndian(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }
    
    fun playPop(isBig: Boolean = false) {
        if (loaded) {
            val soundId = if (isBig) bigPopSoundId else popSoundId
            if (soundId > 0) {
                soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
            }
        }
    }
    
    fun playFirework() {
        if (loaded && fireworkSoundId > 0) {
            soundPool.play(fireworkSoundId, 1f, 1f, 0, 0, 1f)
        }
    }
    
    fun playCheer() {
        if (loaded && cheerSoundId > 0) {
            soundPool.play(cheerSoundId, 1f, 1f, 0, 0, 1f)
        }
    }
    
    fun playExplosion(type: ExplosionType, isRare: Boolean = false) {
        if (!loaded) return
        when (type) {
            ExplosionType.NONE -> soundPool.play(popSoundId, 1f, 1f, 0, 0, 1f)
            ExplosionType.FIRE -> generateAndPlayFireSound(isRare)
            ExplosionType.LIGHTNING -> generateAndPlayLightningSound(isRare)
            ExplosionType.THUNDER -> generateAndPlayThunderSound(isRare)
            ExplosionType.WIND -> generateAndPlayWindSound(isRare)
            ExplosionType.RAIN -> generateAndPlayRainSound(isRare)
            ExplosionType.DARK -> generateAndPlayDarkSound(isRare)
            ExplosionType.LIGHT -> generateAndPlayLightSound(isRare)
            ExplosionType.ICE -> generateAndPlayIceSound(isRare)
            ExplosionType.VOID -> generateAndPlayVoidSound(isRare)
            ExplosionType.STAR -> generateAndPlayStarSound(isRare)
        }
    }
    
    // 火属性 - 噼里啪啦爆裂声
    private fun generateAndPlayFireSound(isRare: Boolean): Int {
        val sampleRate = 22050
        val duration = if (isRare) 0.8f else 0.5f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        
        var rand = Random(seed = System.nanoTime().toLong())
        for (i in samples.indices) {
            val t = i.toFloat() / sampleRate
            val envelope = exp(-t * 6).toFloat()
            // 噼啪爆裂 - 多个短促脉冲
            var crackle = 0f
            val pulsePhase = (t * 20 * PI).toFloat()
            if (sin(pulsePhase) > 0.7f) {
                crackle = (rand.nextFloat() * 2f - 1f) * 0.8f * exp(-t * 8).toFloat()
            }
            // 低频火焰轰鸣
            val boom = sin(2 * PI * 150 * t.toDouble()).toFloat() * exp(-t * 5).toFloat() * 0.4f
            // 高频嘶嘶声
            val hiss = (rand.nextFloat() * 2f - 1f) * 0.2f * exp(-t * 4).toFloat()
            val value = (crackle + boom + hiss) * envelope
            samples[i] = (value * Short.MAX_VALUE * 0.6f).toInt().toShort()
        }
        
        val path = writeWav(samples, sampleRate)
        val soundId = soundPool.load(path, 1)
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
        return soundId
    }
    
    // 雷属性 - 滋滋电流声
    private fun generateAndPlayLightningSound(isRare: Boolean): Int {
        val sampleRate = 22050
        val duration = if (isRare) 0.6f else 0.35f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        
        for (i in samples.indices) {
            val t = i.toFloat() / sampleRate
            val envelope = exp(-t * 12).toFloat()
            // 滋滋电流 - 方波效果
            val zapFreq = 2000f + sin(t * 50) * 1000f
            val zap = if (sin(2 * PI * zapFreq * t.toDouble()) > 0) 0.6f else -0.6f
            // 电弧爆裂
            val arc = sin(2 * PI * 500 * t.toDouble()).toFloat() * exp(-t * 30).toFloat() * 0.5f
            // 高频嘶鸣
            val buzz = (Random.nextFloat() * 2f - 1f) * 0.15f * exp(-t * 20).toFloat()
            val value = (zap + arc + buzz) * envelope
            samples[i] = (value * Short.MAX_VALUE * 0.5f).toInt().toShort()
        }
        
        val path = writeWav(samples, sampleRate)
        val soundId = soundPool.load(path, 1)
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
        return soundId
    }
    
    // 雷霆属性 - 轰隆隆雷声
    private fun generateAndPlayThunderSound(isRare: Boolean): Int {
        val sampleRate = 22050
        val duration = if (isRare) 1.2f else 0.8f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        
        for (i in samples.indices) {
            val t = i.toFloat() / sampleRate
            val envelope = exp(-t * 3).toFloat()
            // 轰隆隆 - 低频滚动
            val rumble1 = sin(2 * PI * 50 * t.toDouble()).toFloat() * 0.5f
            val rumble2 = sin(2 * PI * 70 * t.toDouble() + sin(t * 3) * PI).toFloat() * 0.3f
            val rumble3 = sin(2 * PI * 35 * t.toDouble()).toFloat() * 0.2f * exp(-t * 2).toFloat()
            // 雷声爆裂
            val crash = (Random.nextFloat() * 2f - 1f) * 0.3f * exp(-t * 8).toFloat()
            val value = (rumble1 + rumble2 + rumble3 + crash) * envelope
            samples[i] = (value * Short.MAX_VALUE * 0.7f).toInt().toShort()
        }
        
        val path = writeWav(samples, sampleRate)
        val soundId = soundPool.load(path, 1)
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
        return soundId
    }
    
    // 风属性 - 呼呼风声
    private fun generateAndPlayWindSound(isRare: Boolean): Int {
        val sampleRate = 22050
        val duration = if (isRare) 0.9f else 0.6f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        
        for (i in samples.indices) {
            val t = i.toFloat() / sampleRate
            val envelope = exp(-t * 4).toFloat()
            // 呼呼风声 - 频率调制
            val windFreq = 200f + sin(t * 8) * 100f + sin(t * 13) * 50f
            val whoosh = sin(2 * PI * windFreq * t.toDouble()).toFloat() * 0.5f
            // 风噪
            val noise = (Random.nextFloat() * 2f - 1f) * 0.3f * exp(-t * 3).toFloat()
            // 呼啸
            val whistle = sin(2 * PI * (800 + sin(t * 6) * 300) * t.toDouble()).toFloat() * 0.15f * exp(-t * 5).toFloat()
            val value = (whoosh + noise + whistle) * envelope
            samples[i] = (value * Short.MAX_VALUE * 0.45f).toInt().toShort()
        }
        
        val path = writeWav(samples, sampleRate)
        val soundId = soundPool.load(path, 1)
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
        return soundId
    }
    
    // 水属性 - 哗啦啦水声
    private fun generateAndPlayRainSound(isRare: Boolean): Int {
        val sampleRate = 22050
        val duration = if (isRare) 0.8f else 0.5f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        
        for (i in samples.indices) {
            val t = i.toFloat() / sampleRate
            val envelope = exp(-t * 5).toFloat()
            // 哗啦啦 - 水滴溅落
            var splash = 0f
            for (d in 0..4) {
                val dropTime = d * 0.08f
                val dropEnv = exp(-(t - dropTime) * 30).toFloat().coerceAtLeast(0f)
                val dropFreq = 800f + d * 200f
                splash += sin(2 * PI * dropFreq * (t - dropTime).toDouble()).toFloat() * dropEnv * 0.2f
            }
            // 水流声
            val flow = (Random.nextFloat() * 2f - 1f) * 0.25f * exp(-t * 4).toFloat()
            // 气泡破裂
            val bubble = sin(2 * PI * 1500 * t.toDouble()).toFloat() * exp(-t * 25).toFloat() * 0.15f
            val value = (splash + flow + bubble) * envelope
            samples[i] = (value * Short.MAX_VALUE * 0.5f).toInt().toShort()
        }
        
        val path = writeWav(samples, sampleRate)
        val soundId = soundPool.load(path, 1)
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
        return soundId
    }
    
    // 暗属性 - 低沉嗡鸣
    private fun generateAndPlayDarkSound(isRare: Boolean): Int {
        val sampleRate = 22050
        val duration = if (isRare) 1.0f else 0.6f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        
        for (i in samples.indices) {
            val t = i.toFloat() / sampleRate
            val envelope = exp(-t * 4).toFloat()
            // 低沉嗡鸣
            val drone1 = sin(2 * PI * 40 * t.toDouble()).toFloat() * 0.5f
            val drone2 = sin(2 * PI * 45 * t.toDouble() + sin(t * 2) * PI * 0.5).toFloat() * 0.3f
            // 暗影低语
            val whisper = sin(2 * PI * 200 * t.toDouble() + sin(t * 7) * PI).toFloat() * 0.15f * exp(-t * 6).toFloat()
            // 嘶嘶声
            val hiss = (Random.nextFloat() * 2f - 1f) * 0.1f * exp(-t * 5).toFloat()
            val value = (drone1 + drone2 + whisper + hiss) * envelope
            samples[i] = (value * Short.MAX_VALUE * 0.55f).toInt().toShort()
        }
        
        val path = writeWav(samples, sampleRate)
        val soundId = soundPool.load(path, 1)
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
        return soundId
    }
    
    // 光属性 - 清脆风铃
    private fun generateAndPlayLightSound(isRare: Boolean): Int {
        val sampleRate = 22050
        val duration = if (isRare) 0.9f else 0.5f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        
        for (i in samples.indices) {
            val t = i.toFloat() / sampleRate
            val envelope = exp(-t * 5).toFloat()
            // 风铃叮当
            val chime1 = sin(2 * PI * 1200 * t.toDouble()).toFloat() * exp(-t * 8).toFloat() * 0.4f
            val chime2 = sin(2 * PI * 1800 * t.toDouble()).toFloat() * exp(-t * 10).toFloat() * 0.3f
            val chime3 = sin(2 * PI * 2400 * t.toDouble()).toFloat() * exp(-t * 12).toFloat() * 0.2f
            // 清脆泛音
            val harmonic = sin(2 * PI * 3600 * t.toDouble()).toFloat() * exp(-t * 15).toFloat() * 0.1f
            val value = (chime1 + chime2 + chime3 + harmonic) * envelope
            samples[i] = (value * Short.MAX_VALUE * 0.35f).toInt().toShort()
        }
        
        val path = writeWav(samples, sampleRate)
        val soundId = soundPool.load(path, 1)
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
        return soundId
    }
    
    // 冰属性 - 咔嚓碎裂
    private fun generateAndPlayIceSound(isRare: Boolean): Int {
        val sampleRate = 22050
        val duration = if (isRare) 0.7f else 0.4f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        
        for (i in samples.indices) {
            val t = i.toFloat() / sampleRate
            val envelope = exp(-t * 8).toFloat()
            // 咔嚓 - 冰裂脉冲
            var crack = 0f
            for (c in 0..3) {
                val crackTime = c * 0.06f
                val crackEnv = exp(-(t - crackTime) * 50).toFloat().coerceAtLeast(0f)
                crack += sin(2 * PI * 3000 * (t - crackTime).toDouble()).toFloat() * crackEnv * 0.3f
            }
            // 碎裂沙沙
            val shatter = (Random.nextFloat() * 2f - 1f) * 0.2f * exp(-t * 10).toFloat()
            // 冰晶叮当
            val tinkle = sin(2 * PI * 5000 * t.toDouble()).toFloat() * exp(-t * 30).toFloat() * 0.1f
            val value = (crack + shatter + tinkle) * envelope
            samples[i] = (value * Short.MAX_VALUE * 0.45f).toInt().toShort()
        }
        
        val path = writeWav(samples, sampleRate)
        val soundId = soundPool.load(path, 1)
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
        return soundId
    }
    
    // 虚空属性 - 吞噬声
    private fun generateAndPlayVoidSound(isRare: Boolean): Int {
        val sampleRate = 22050
        val duration = if (isRare) 1.0f else 0.7f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        
        for (i in samples.indices) {
            val t = i.toFloat() / sampleRate
            val envelope = exp(-t * 3).toFloat()
            // 吞噬 - 频率下降
            val suckFreq = (600f - t * 500f).coerceAtLeast(20f)
            val suck = sin(2 * PI * suckFreq * t.toDouble()).toFloat() * 0.4f
            // 虚空嗡鸣
            val voidDrone = sin(2 * PI * 25 * t.toDouble()).toFloat() * 0.3f
            // 吸入声
            val inhale = (Random.nextFloat() * 2f - 1f) * 0.15f * exp(-t * 4).toFloat()
            val value = (suck + voidDrone + inhale) * envelope
            samples[i] = (value * Short.MAX_VALUE * 0.5f).toInt().toShort()
        }
        
        val path = writeWav(samples, sampleRate)
        val soundId = soundPool.load(path, 1)
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
        return soundId
    }
    
    // 星属性 - 闪烁叮当
    private fun generateAndPlayStarSound(isRare: Boolean): Int {
        val sampleRate = 22050
        val duration = if (isRare) 1.0f else 0.6f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        
        for (i in samples.indices) {
            val t = i.toFloat() / sampleRate
            val envelope = exp(-t * 4).toFloat()
            // 闪烁叮当 - 多个音符
            var twinkle = 0f
            for (n in 0..5) {
                val noteTime = n * 0.08f
                val noteEnv = exp(-(t - noteTime) * 15).toFloat().coerceAtLeast(0f)
                val noteFreq = listOf(1200f, 1500f, 1800f, 2100f, 2400f, 2700f)[n]
                twinkle += sin(2 * PI * noteFreq * (t - noteTime).toDouble()).toFloat() * noteEnv * 0.2f
            }
            // 魔法泛音
            val magic = sin(2 * PI * 4000 * t.toDouble() + sin(t * 10) * PI).toFloat() * 0.1f * exp(-t * 8).toFloat()
            val value = (twinkle + magic) * envelope
            samples[i] = (value * Short.MAX_VALUE * 0.35f).toInt().toShort()
        }
        
        val path = writeWav(samples, sampleRate)
        val soundId = soundPool.load(path, 1)
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
        return soundId
    }
    
    fun release() {
        soundPool.release()
    }
}
