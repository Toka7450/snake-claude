package com.example.snake

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var snakeView: SnakeView
    private lateinit var scoreText: TextView
    private lateinit var restartButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        snakeView = findViewById(R.id.snake_view)
        scoreText = findViewById(R.id.score_text)
        restartButton = findViewById(R.id.restart_button)

        snakeView.onScoreChanged = { score ->
            runOnUiThread { scoreText.text = "Score: $score" }
        }
        snakeView.onGameOver = {
            runOnUiThread { restartButton.visibility = android.view.View.VISIBLE }
        }

        restartButton.setOnClickListener {
            restartButton.visibility = android.view.View.GONE
            snakeView.restart()
        }
    }

    override fun onPause() {
        super.onPause()
        snakeView.pause()
    }

    override fun onResume() {
        super.onResume()
        snakeView.resume()
    }
}
