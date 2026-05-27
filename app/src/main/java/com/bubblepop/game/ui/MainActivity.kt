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
    private lateinit var totalScoreText: TextView
    private lateinit var finalScoreText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        scoreText = binding.scoreText
        totalScoreText = binding.totalScoreText
        finalScoreText = binding.finalScoreText
        updateScoreText(0, 0)
        
        binding.bubbleView.onScoreChanged = { score ->
            updateScoreText(score, binding.bubbleView.getTotalScore())
        }
        
        binding.bubbleView.onTotalScoreChanged = { totalScore ->
            updateScoreText(binding.bubbleView.getScore(), totalScore)
        }
        
        binding.bubbleView.onGameOver = {
            runOnUiThread {
                val score = binding.bubbleView.getScore()
                val total = binding.bubbleView.getTotalScore()
                finalScoreText.text = getString(R.string.final_score, score, total)
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
    
    private fun updateScoreText(score: Int, totalScore: Int) {
        scoreText.text = getString(R.string.score_display, score, totalScore)
    }
    
    override fun onResume() {
        super.onResume()
        updateScoreText(binding.bubbleView.getScore(), binding.bubbleView.getTotalScore())
    }
}
