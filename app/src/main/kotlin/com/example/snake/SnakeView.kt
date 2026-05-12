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
import kotlin.math.sin
import kotlin.math.PI

private enum class GameState { COUNTDOWN, PLAYING, GAME_OVER }

class SnakeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val game = SnakeGame()
    private val handler = Handler(Looper.getMainLooper())
    private var state = GameState.COUNTDOWN
    private var countdown = 3

    var bestScore = 0
        private set

    var onScoreChanged: ((Int) -> Unit)? = null
    var onBestChanged: ((Int) -> Unit)? = null

    // --- Paints ---
    private val bgPaint = Paint().apply { color = Color.parseColor("#0f0f23") }
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#16213e")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val foodPaint = Paint().apply { color = Color.parseColor("#e94560"); isAntiAlias = true }
    private val foodGlowPaint = Paint().apply {
        // #AARRGGBB: alpha=55 (~33%), color=e94560
        color = Color.parseColor("#55e94560")
        isAntiAlias = true
    }
    private val segmentPaint = Paint().apply { isAntiAlias = true }
    private val eyePaint = Paint().apply { color = Color.WHITE; isAntiAlias = true }
    private val pupilPaint = Paint().apply { color = Color.parseColor("#0f0f23"); isAntiAlias = true }
    private val overlayPaint = Paint().apply { color = Color.parseColor("#CC0f0f23") }
    private val countdownPaint = Paint().apply {
        color = Color.parseColor("#4ecca3")
        textSize = 120f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }
    private val titlePaint = Paint().apply {
        color = Color.parseColor("#e94560")
        textSize = 64f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }
    private val infoScorePaint = Paint().apply {
        color = Color.parseColor("#4ecca3")
        textSize = 48f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }
    private val subtitlePaint = Paint().apply {
        color = Color.parseColor("#aaaacc")
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val rect = RectF()

    // --- Runnables ---

    private val gameTick = object : Runnable {
        override fun run() {
            if (state != GameState.PLAYING) return
            val prevScore = game.score
            game.step()
            if (game.score != prevScore) onScoreChanged?.invoke(game.score)
            invalidate()
            if (game.isGameOver) {
                state = GameState.GAME_OVER
                if (game.score > bestScore) {
                    bestScore = game.score
                    onBestChanged?.invoke(bestScore)
                }
            } else {
                handler.postDelayed(this, game.tickMs)
            }
        }
    }

    // Drives food pulse animation between game ticks
    private val animTick = object : Runnable {
        override fun run() {
            if (state != GameState.PLAYING) return
            invalidate()
            handler.postDelayed(this, 50)
        }
    }

    private val countdownTick = object : Runnable {
        override fun run() {
            if (state != GameState.COUNTDOWN) return
            countdown--
            if (countdown <= 0) {
                state = GameState.PLAYING
                handler.post(gameTick)
                handler.post(animTick)
            } else {
                invalidate()
                handler.postDelayed(this, 1000)
            }
        }
    }

    // --- Touch ---

    private var touchX = 0f
    private var touchY = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchX = event.x
                touchY = event.y
            }
            MotionEvent.ACTION_UP -> {
                if (state == GameState.GAME_OVER) {
                    restart()
                    return true
                }
                val dx = event.x - touchX
                val dy = event.y - touchY
                if (abs(dx) < 20 && abs(dy) < 20) return true
                if (abs(dx) > abs(dy)) {
                    game.queueDirection(if (dx > 0) Direction.RIGHT else Direction.LEFT)
                } else {
                    game.queueDirection(if (dy > 0) Direction.DOWN else Direction.UP)
                }
            }
        }
        return true
    }

    // --- Lifecycle ---

    fun restart() {
        handler.removeCallbacks(gameTick)
        handler.removeCallbacks(animTick)
        handler.removeCallbacks(countdownTick)
        game.reset()
        countdown = 3
        state = GameState.COUNTDOWN
        onScoreChanged?.invoke(0)
        invalidate()
        handler.postDelayed(countdownTick, 1000)
    }

    fun pause() {
        handler.removeCallbacks(gameTick)
        handler.removeCallbacks(animTick)
        handler.removeCallbacks(countdownTick)
    }

    fun resume() {
        when (state) {
            GameState.COUNTDOWN -> handler.postDelayed(countdownTick, 1000)
            GameState.PLAYING -> {
                handler.post(gameTick)
                handler.post(animTick)
            }
            GameState.GAME_OVER -> {}
        }
    }

    // --- Drawing ---

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cellW = w / game.cols
        val cellH = h / game.rows

        canvas.drawRect(0f, 0f, w, h, bgPaint)
        for (i in 0..game.cols) canvas.drawLine(i * cellW, 0f, i * cellW, h, gridPaint)
        for (i in 0..game.rows) canvas.drawLine(0f, i * cellH, w, i * cellH, gridPaint)

        drawFood(canvas, cellW, cellH)
        drawSnake(canvas, cellW, cellH)

        when (state) {
            GameState.COUNTDOWN -> drawCountdown(canvas, w, h)
            GameState.GAME_OVER -> drawGameOver(canvas, w, h)
            GameState.PLAYING -> {}
        }
    }

    private fun drawFood(canvas: Canvas, cellW: Float, cellH: Float) {
        val food = game.food
        val cx = food.x * cellW + cellW / 2
        val cy = food.y * cellH + cellH / 2
        val pulse = (sin(System.currentTimeMillis() / 500.0 * PI) * 0.15 + 0.85).toFloat()
        val fr = cellW * 0.38f * pulse
        canvas.drawCircle(cx, cy, fr * 2f, foodGlowPaint)
        canvas.drawCircle(cx, cy, fr, foodPaint)
    }

    private fun drawSnake(canvas: Canvas, cellW: Float, cellH: Float) {
        val snake = game.snake
        val size = snake.size
        snake.forEachIndexed { index, cell ->
            val t = if (size > 1) index.toFloat() / (size - 1) else 0f
            segmentPaint.color = lerpColor(0xFF4ecca3.toInt(), 0xFF1a6b50.toInt(), t)
            val margin = if (index == 0) 1.5f else 2.5f
            rect.set(
                cell.x * cellW + margin,
                cell.y * cellH + margin,
                cell.x * cellW + cellW - margin,
                cell.y * cellH + cellH - margin
            )
            val radius = if (index == 0) cellW * 0.35f else cellW * 0.25f
            canvas.drawRoundRect(rect, radius, radius, segmentPaint)
        }
        if (snake.isNotEmpty()) drawEyes(canvas, snake.first(), cellW, cellH)
    }

    private fun drawEyes(canvas: Canvas, head: Cell, cellW: Float, cellH: Float) {
        val cx = head.x * cellW + cellW / 2
        val cy = head.y * cellH + cellH / 2
        val er = cellW * 0.1f
        val pr = er * 0.55f
        val offset = cellW * 0.18f
        val (eye1, eye2) = when (game.direction) {
            Direction.RIGHT -> Pair(Pair(cx + offset * 0.5f, cy - offset), Pair(cx + offset * 0.5f, cy + offset))
            Direction.LEFT  -> Pair(Pair(cx - offset * 0.5f, cy - offset), Pair(cx - offset * 0.5f, cy + offset))
            Direction.UP    -> Pair(Pair(cx - offset, cy - offset * 0.5f), Pair(cx + offset, cy - offset * 0.5f))
            Direction.DOWN  -> Pair(Pair(cx - offset, cy + offset * 0.5f), Pair(cx + offset, cy + offset * 0.5f))
        }
        listOf(eye1, eye2).forEach { (ex, ey) ->
            canvas.drawCircle(ex, ey, er, eyePaint)
            canvas.drawCircle(ex, ey, pr, pupilPaint)
        }
    }

    private fun drawCountdown(canvas: Canvas, w: Float, h: Float) {
        canvas.drawRect(0f, 0f, w, h, overlayPaint)
        val textY = h / 2 - (countdownPaint.descent() + countdownPaint.ascent()) / 2
        canvas.drawText(countdown.toString(), w / 2, textY, countdownPaint)
        canvas.drawText("Swipe to move", w / 2, textY + 100f, subtitlePaint)
    }

    private fun drawGameOver(canvas: Canvas, w: Float, h: Float) {
        canvas.drawRect(0f, 0f, w, h, overlayPaint)
        val midX = w / 2
        var y = h / 2 - 140f
        canvas.drawText("GAME OVER", midX, y, titlePaint)
        y += 90f
        canvas.drawText("Score  ${game.score}", midX, y, infoScorePaint)
        y += 65f
        canvas.drawText("Best  $bestScore", midX, y, infoScorePaint)
        y += 90f
        canvas.drawText("Tap anywhere to play again", midX, y, subtitlePaint)
    }

    private fun lerpColor(from: Int, to: Int, t: Float): Int {
        val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * t).toInt().coerceIn(0, 255)
        val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * t).toInt().coerceIn(0, 255)
        val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
