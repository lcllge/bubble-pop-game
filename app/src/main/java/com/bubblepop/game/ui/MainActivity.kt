package com.bubblepop.game.ui

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
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
        
        binding.viewScoresButton.setOnClickListener {
            showHighScoresDialog()
        }
        
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
    
    private fun showHighScoresDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_high_scores)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val scores = binding.bubbleView.getHighScores()
        
        val rank1Text = dialog.findViewById<TextView>(R.id.rank1_text)
        val rank2Text = dialog.findViewById<TextView>(R.id.rank2_text)
        val rank3Text = dialog.findViewById<TextView>(R.id.rank3_text)
        
        val nonZeroScores = scores.filter { it > 0 }
        
        if (nonZeroScores.isEmpty()) {
            rank1Text.text = getString(R.string.no_scores)
            rank2Text.visibility = View.GONE
            rank3Text.visibility = View.GONE
        } else {
            rank1Text.visibility = View.VISIBLE
            rank2Text.visibility = View.VISIBLE
            rank3Text.visibility = View.VISIBLE
            rank1Text.text = getString(R.string.rank_1, nonZeroScores[0])
            if (nonZeroScores.size > 1) {
                rank2Text.text = getString(R.string.rank_2, nonZeroScores[1])
            } else {
                rank2Text.text = getString(R.string.no_scores)
            }
            if (nonZeroScores.size > 2) {
                rank3Text.text = getString(R.string.rank_3, nonZeroScores[2])
            } else {
                rank3Text.text = getString(R.string.no_scores)
            }
        }
        
        dialog.findViewById<Button>(R.id.back_button).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun updateScoreText(score: Int, totalScore: Int) {
        scoreText.text = getString(R.string.score_display, score, totalScore)
    }
    
    override fun onResume() {
        super.onResume()
        updateScoreText(binding.bubbleView.getScore(), binding.bubbleView.getTotalScore())
    }
}
