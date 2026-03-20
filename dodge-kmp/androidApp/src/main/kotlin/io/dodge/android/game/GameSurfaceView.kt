package io.dodge.android.game

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import io.dodge.engine.FrameResult
import io.dodge.engine.GameEngine
import io.dodge.model.GameConfig
import io.dodge.model.GameEvent
import io.dodge.model.GameMode

class GameSurfaceView(
    context: Context,
    private val mode: GameMode = GameMode.ARCADE,
    private val dailySeed: String? = null,
    private val onGameEvent: (GameEvent) -> Unit = {}
) : SurfaceView(context), SurfaceHolder.Callback {

    private var gameThread: GameThread? = null
    private var engine: GameEngine? = null
    private val renderer = AndroidGameRenderer()
    private val density = context.resources.displayMetrics.density

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Convert raw pixels to CSS-pixel-equivalent (density-independent)
        // The JS game was designed for CSS pixels (~360-430px wide screens)
        val w = width.toFloat() / density
        val h = height.toFloat() / density
        engine = GameEngine(w, h, mode, dailySeed).also { it.start() }

        gameThread = GameThread(holder).also {
            it.running = true
            it.start()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        gameThread?.running = false
        gameThread?.join()
        gameThread = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                // Convert touch coordinates from raw pixels to game coordinates
                engine?.updateTouchPosition(event.x / density, event.y / density)
            }
            MotionEvent.ACTION_UP -> {
                val eng = engine ?: return true
                if (!eng.running && !eng.alive) {
                    eng.start()
                }
            }
        }
        return true
    }

    fun sendRespawn() {
        engine?.respawn()
    }

    fun sendPause() {
        val eng = engine ?: return
        if (eng.running) {
            eng.tick()
        }
    }

    fun getEngine(): GameEngine? = engine

    inner class GameThread(private val surfaceHolder: SurfaceHolder) : Thread("GameThread") {
        @Volatile
        var running = true

        override fun run() {
            var lastFrameMs = 0L

            while (running) {
                val now = System.nanoTime() / 1_000_000
                if (now - lastFrameMs < GameConfig.FRAME_CAP_MS) {
                    try { sleep(1) } catch (_: InterruptedException) { break }
                    continue
                }
                lastFrameMs = now

                val eng = engine ?: continue
                val result: FrameResult = eng.tick()

                for (event in result.events) {
                    onGameEvent(event)
                }

                val canvas = surfaceHolder.lockCanvas() ?: continue
                try {
                    // Scale the canvas by density so game coordinates map to real pixels
                    canvas.save()
                    canvas.scale(density, density)
                    // Save again so ResetTransform can restore to this density-scaled state
                    canvas.save()
                    renderer.gameWidth = canvas.width / density
                    renderer.gameHeight = canvas.height / density
                    renderer.render(canvas, result.renderCommands)
                    canvas.restoreToCount(1)
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
}
