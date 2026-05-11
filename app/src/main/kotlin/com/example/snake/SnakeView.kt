package com.example.snake

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class SnakeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val game = SnakeGame()
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private val tickMs = 150L

    var onScoreChanged: ((Int) -> Unit)? = null
    var onGameOver: (() -> Unit)? = null

    private val bgPaint = Paint().apply { color = Color.parseColor("#1a1a2e") }
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#16213e")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val snakeHeadPaint = Paint().apply { color = Color.parseColor("#4ecca3"); isAntiAlias = true }
    private val snakeBodyPaint = Paint().apply { color = Color.parseColor("#3a9e7f"); isAntiAlias = true }
    private val foodPaint = Paint().apply { color = Color.parseColor("#e94560"); isAntiAlias = true }

    private val rect = RectF()

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            val prevScore = game.score
            game.step()
            if (game.score != prevScore) onScoreChanged?.invoke(game.score)
            invalidate()
            if (game.isGameOver) {
                running = false
                onGameOver?.invoke()
            } else {
                handler.postDelayed(this, tickMs)
            }
        }
    }

    // Swipe detection
    private var touchX = 0f
    private var touchY = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchX = event.x
                touchY = event.y
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - touchX
                val dy = event.y - touchY
                if (abs(dx) < 20 && abs(dy) < 20) return true
                if (abs(dx) > abs(dy)) {
                    game.setDirection(if (dx > 0) Direction.RIGHT else Direction.LEFT)
                } else {
                    game.setDirection(if (dy > 0) Direction.DOWN else Direction.UP)
                }
            }
        }
        return true
    }

    fun restart() {
        game.reset()
        onScoreChanged?.invoke(0)
        resume()
    }

    fun pause() {
        running = false
        handler.removeCallbacks(tick)
    }

    fun resume() {
        if (!running && !game.isGameOver) {
            running = true
            handler.postDelayed(tick, tickMs)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cellW = w / game.cols
        val cellH = h / game.rows

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Grid lines (subtle)
        for (i in 0..game.cols) canvas.drawLine(i * cellW, 0f, i * cellW, h, gridPaint)
        for (i in 0..game.rows) canvas.drawLine(0f, i * cellH, w, i * cellH, gridPaint)

        // Food
        val food = game.food
        val fr = cellW * 0.4f
        canvas.drawCircle(
            food.x * cellW + cellW / 2,
            food.y * cellH + cellH / 2,
            fr, foodPaint
        )

        // Snake
        game.snake.forEachIndexed { index, cell ->
            val paint = if (index == 0) snakeHeadPaint else snakeBodyPaint
            val margin = if (index == 0) 2f else 3f
            rect.set(
                cell.x * cellW + margin,
                cell.y * cellH + margin,
                cell.x * cellW + cellW - margin,
                cell.y * cellH + cellH - margin
            )
            val radius = if (index == 0) cellW * 0.3f else cellW * 0.2f
            canvas.drawRoundRect(rect, radius, radius, paint)
        }
    }
}
