package com.example.snake

import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }

data class Cell(val x: Int, val y: Int)

class SnakeGame(val cols: Int = 20, val rows: Int = 20) {

    var snake: ArrayDeque<Cell> = ArrayDeque()
    var food: Cell = Cell(0, 0)
    var direction: Direction = Direction.RIGHT
    private var pendingDirection: Direction = Direction.RIGHT
    var score: Int = 0
    var isGameOver: Boolean = false

    // Speed increases with score: 150ms → 60ms floor
    val tickMs: Long get() = maxOf(60L, 150L - score * 5L)

    init {
        reset()
    }

    fun reset() {
        snake = ArrayDeque<Cell>().also {
            it.addFirst(Cell(cols / 2, rows / 2))
            it.addFirst(Cell(cols / 2 + 1, rows / 2))
            it.addFirst(Cell(cols / 2 + 2, rows / 2))
        }
        direction = Direction.RIGHT
        pendingDirection = Direction.RIGHT
        score = 0
        isGameOver = false
        spawnFood()
    }

    fun queueDirection(d: Direction) {
        val blocked = when (d) {
            Direction.UP -> direction == Direction.DOWN
            Direction.DOWN -> direction == Direction.UP
            Direction.LEFT -> direction == Direction.RIGHT
            Direction.RIGHT -> direction == Direction.LEFT
        }
        if (!blocked) pendingDirection = d
    }

    fun step() {
        if (isGameOver) return
        direction = pendingDirection

        val head = snake.first()
        val next = when (direction) {
            Direction.UP -> Cell(head.x, head.y - 1)
            Direction.DOWN -> Cell(head.x, head.y + 1)
            Direction.LEFT -> Cell(head.x - 1, head.y)
            Direction.RIGHT -> Cell(head.x + 1, head.y)
        }

        if (next.x < 0 || next.x >= cols || next.y < 0 || next.y >= rows) {
            isGameOver = true
            return
        }
        if (snake.contains(next)) {
            isGameOver = true
            return
        }

        snake.addFirst(next)
        if (next == food) {
            score++
            spawnFood()
        } else {
            snake.removeLast()
        }
    }

    private fun spawnFood() {
        val free = mutableListOf<Cell>()
        for (x in 0 until cols) for (y in 0 until rows) {
            val c = Cell(x, y)
            if (!snake.contains(c)) free.add(c)
        }
        food = if (free.isNotEmpty()) free[Random.nextInt(free.size)] else Cell(0, 0)
    }
}
