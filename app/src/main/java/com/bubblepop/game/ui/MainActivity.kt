package com.bubblepop.game.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bubblepop.game.R
import com.bubblepop.game.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var scoreText: TextView
    private lateinit var finalScoreText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        scoreText = binding.scoreText
        finalScoreText = binding.finalScoreText
        updateScoreText(0)
        
        binding.bubbleView.onScoreChanged = { score ->
            updateScoreText(score)
        }
        
        binding.bubbleView.onGameOver = {
            runOnUiThread {
                val score = binding.bubbleView.getScore()
                finalScoreText.text = getString(R.string.final_score, score)
                binding.gameOverOverlay.visibility = View.VISIBLE
            }
        }
        
        binding.restartButton.setOnClickListener {
            binding.gameOverOverlay.visibility = View.GONE
            binding.bubbleView.resetGame()
        }
        
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
    
    private fun updateScoreText(score: Int) {
        scoreText.text = score.toString()
    }
    
    override fun onResume() {
        super.onResume()
        if (!binding::bubbleView.isInitialized.not()) {
            updateScoreText(binding.bubbleView.getScore())
        }
    }
}
