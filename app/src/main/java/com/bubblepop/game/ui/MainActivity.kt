package com.bubblepop.game.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bubblepop.game.R
import com.bubblepop.game.databinding.ActivityMainBinding
import com.bubblepop.game.view.BubbleView

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var popCountText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        popCountText = binding.popCountText
        updatePopCountText()
        
        binding.bubbleView.setPopCountCallback { count ->
            updatePopCountText()
        }
        
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
    
    private fun updatePopCountText() {
        val app = application as com.bubblepop.game.BubblePopApplication
        val count = app.settingsManager.popCount
        popCountText.text = getString(R.string.pop_count, count)
    }
    
    override fun onResume() {
        super.onResume()
        updatePopCountText()
        binding.bubbleView.clearAllBubbles()
    }
}
