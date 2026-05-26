package com.bubblepop.game.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
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
    
    fun release() {
        soundPool.release()
    }
}
