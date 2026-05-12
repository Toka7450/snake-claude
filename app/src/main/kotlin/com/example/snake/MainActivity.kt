package com.example.snake

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var snakeView: SnakeView
    private lateinit var scoreText: TextView
    private lateinit var bestText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        snakeView = findViewById(R.id.snake_view)
        scoreText = findViewById(R.id.score_text)
        bestText = findViewById(R.id.best_text)

        snakeView.onScoreChanged = { score ->
            runOnUiThread { scoreText.text = "Score  $score" }
        }
        snakeView.onBestChanged = { best ->
            runOnUiThread { bestText.text = "Best  $best" }
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
