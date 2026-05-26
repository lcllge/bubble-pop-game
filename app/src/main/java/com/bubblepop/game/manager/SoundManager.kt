package com.bubblepop.game.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class SoundManager(context: Context) {
    
    private val soundPool: SoundPool
    private var popSoundId: Int = -1
    private var loaded = false
    
    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()
        
        // Generate pop sound programmatically
        val popSound = generatePopSound()
        popSoundId = soundPool.load(popSound, 1)
        
        soundPool.setOnLoadCompleteListener { _, _, status ->
            loaded = status == 0
        }
    }
    
    private fun generatePopSound(): String {
        val sampleRate = 22050
        val duration = 0.08f // 80ms pop
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        
        // Create a short "pop" sound: quick frequency sweep with decay
        for (i in samples.indices) {
            val t = i.toFloat() / sampleRate
            val freq = 800f - t * 6000f
            val envelope = exp(-t * 60).toFloat()
            val noise = (Random.nextFloat() * 2f - 1f) * 0.3f
            val value = (sin(2 * PI * freq * t.toDouble()).toFloat() + noise) * envelope
            samples[i] = (value * Short.MAX_VALUE).toInt().toShort()
        }
        
        // Write to temporary WAV file
        val file = java.io.File.createTempFile("pop_sound", ".wav", context.cacheDir)
        file.deleteOnExit()
        
        file.outputStream().use { out ->
            // WAV header
            val byteRate = sampleRate * 2
            val blockAlign = 2
            val dataSize = numSamples * 2
            val totalSize = 36 + dataSize
            
            out.write("RIFF".toByteArray())
            out.write(intToLittleEndian(totalSize))
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray())
            out.write(intToLittleEndian(16)) // Subchunk1Size
            out.write(shortToLittleEndian(1)) // PCM
            out.write(shortToLittleEndian(1)) // Mono
            out.write(intToLittleEndian(sampleRate))
            out.write(intToLittleEndian(byteRate))
            out.write(shortToLittleEndian(blockAlign.toShort()))
            out.write(shortToLittleEndian(16)) // Bits per sample
            out.write("data".toByteArray())
            out.write(intToLittleEndian(dataSize))
            
            // Write samples
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
    
    fun playPop() {
        if (loaded && popSoundId > 0) {
            soundPool.play(popSoundId, 1f, 1f, 1, 0, 1f)
        }
    }
    
    fun release() {
        soundPool.release()
    }
}
