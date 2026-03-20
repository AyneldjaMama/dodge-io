package io.dodge.android.game

import android.graphics.*
import io.dodge.engine.RenderCommand
import io.dodge.engine.TextAlign
import kotlin.math.sin

class AndroidGameRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
    }
    private val random = java.util.Random()

    // Set by the SurfaceView before rendering; game-space dimensions (CSS-pixel equivalent)
    var gameWidth: Float = 0f
    var gameHeight: Float = 0f

    fun render(canvas: Canvas, commands: List<RenderCommand>) {
        for (cmd in commands) {
            when (cmd) {
                is RenderCommand.ClearBackground -> {
                    // drawColor ignores transform, always fills full canvas
                    canvas.drawColor(cmd.color.toInt())
                }

                is RenderCommand.DrawGrid -> {
                    paint.color = cmd.color.toInt()
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1f
                    var x = 0f
                    while (x < cmd.w) {
                        canvas.drawLine(x, 0f, x, cmd.h, paint)
                        x += cmd.spacing
                    }
                    var y = 0f
                    while (y < cmd.h) {
                        canvas.drawLine(0f, y, cmd.w, y, paint)
                        y += cmd.spacing
                    }
                }

                is RenderCommand.DrawCircle -> {
                    paint.style = Paint.Style.FILL
                    paint.color = cmd.color.toInt()
                    paint.alpha = (cmd.alpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawCircle(cmd.x, cmd.y, cmd.radius, paint)
                    paint.alpha = 255
                }

                is RenderCommand.DrawGlowCircle -> {
                    if (cmd.outerRadius <= 0f) return
                    val innerStop = if (cmd.outerRadius > 0f) (cmd.innerRadius / cmd.outerRadius).coerceIn(0f, 0.99f) else 0f
                    val shader = RadialGradient(
                        cmd.x, cmd.y, cmd.outerRadius,
                        intArrayOf(
                            colorWithAlpha(cmd.color, cmd.alpha),
                            colorWithAlpha(cmd.color, 0f)
                        ),
                        floatArrayOf(innerStop, 1f),
                        Shader.TileMode.CLAMP
                    )
                    paint.style = Paint.Style.FILL
                    paint.shader = shader
                    canvas.drawCircle(cmd.x, cmd.y, cmd.outerRadius, paint)
                    paint.shader = null
                }

                is RenderCommand.DrawRing -> {
                    paint.style = Paint.Style.STROKE
                    paint.color = cmd.color.toInt()
                    paint.alpha = (cmd.alpha * 255).toInt().coerceIn(0, 255)
                    paint.strokeWidth = cmd.lineWidth
                    if (cmd.dashPattern != null) {
                        paint.pathEffect = DashPathEffect(cmd.dashPattern, 0f)
                    }
                    canvas.drawCircle(cmd.x, cmd.y, cmd.radius, paint)
                    paint.pathEffect = null
                    paint.alpha = 255
                }

                is RenderCommand.DrawArc -> {
                    paint.style = Paint.Style.STROKE
                    paint.color = cmd.color.toInt()
                    paint.strokeWidth = cmd.lineWidth
                    val left = cmd.x - cmd.radius
                    val top = cmd.y - cmd.radius
                    val right = cmd.x + cmd.radius
                    val bottom = cmd.y + cmd.radius
                    canvas.drawArc(
                        left, top, right, bottom,
                        Math.toDegrees(cmd.startAngle.toDouble()).toFloat(),
                        Math.toDegrees(cmd.sweepAngle.toDouble()).toFloat(),
                        false, paint
                    )
                }

                is RenderCommand.DrawLaser -> {
                    canvas.save()
                    canvas.translate(cmd.x, cmd.y)
                    canvas.rotate(Math.toDegrees(cmd.angle.toDouble()).toFloat())

                    if (cmd.age < cmd.chargeFrames) {
                        val chargeAlpha = (0.3f + sin(cmd.age * 0.5f) * 0.2f)
                        paint.style = Paint.Style.STROKE
                        paint.color = colorWithAlpha(0xFF00D4FF, chargeAlpha)
                        paint.strokeWidth = 1f
                        paint.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
                        canvas.drawLine(-cmd.length, 0f, cmd.length, 0f, paint)
                        paint.pathEffect = null
                    } else if (cmd.width > 0f) {
                        val shader = LinearGradient(
                            0f, -cmd.width / 2f, 0f, cmd.width / 2f,
                            intArrayOf(
                                colorWithAlpha(0xFF00D4FF, 0f),
                                colorWithAlpha(0xFF00D4FF, 0.4f),
                                colorWithAlpha(0xFFFFFFFF, 0.8f),
                                colorWithAlpha(0xFF00D4FF, 0.4f),
                                colorWithAlpha(0xFF00D4FF, 0f)
                            ),
                            floatArrayOf(0f, 0.3f, 0.5f, 0.7f, 1f),
                            Shader.TileMode.CLAMP
                        )
                        paint.style = Paint.Style.FILL
                        paint.shader = shader
                        canvas.drawRect(-cmd.length, -cmd.width / 2f, cmd.length, cmd.width / 2f, paint)
                        paint.shader = null
                    }
                    canvas.restore()
                }

                is RenderCommand.DrawParticle -> {
                    paint.style = Paint.Style.FILL
                    paint.color = Color.argb(
                        (cmd.alpha * 255).toInt().coerceIn(0, 255),
                        cmd.r, cmd.g, cmd.b
                    )
                    canvas.drawCircle(cmd.x, cmd.y, cmd.size, paint)
                }

                is RenderCommand.DrawPowerUp -> {
                    paint.style = Paint.Style.FILL
                    paint.color = colorWithAlpha(0xFF00D4FF, 0.3f * cmd.alpha)
                    canvas.drawCircle(cmd.x, cmd.y, cmd.radius, paint)
                    paint.style = Paint.Style.STROKE
                    paint.color = colorWithAlpha(0xFF00D4FF, 0.8f * cmd.alpha)
                    paint.strokeWidth = 2f
                    canvas.drawCircle(cmd.x, cmd.y, cmd.radius, paint)
                    textPaint.color = colorWithAlpha(0xFFFFFFFF, 0.9f * cmd.alpha)
                    textPaint.textSize = 10f
                    textPaint.textAlign = Paint.Align.CENTER
                    textPaint.typeface = Typeface.DEFAULT_BOLD
                    val fm = textPaint.fontMetrics
                    val textY = cmd.y - (fm.ascent + fm.descent) / 2f
                    canvas.drawText(cmd.label, cmd.x, textY, textPaint)
                }

                is RenderCommand.DrawText -> {
                    textPaint.color = cmd.color.toInt()
                    textPaint.alpha = (cmd.alpha * 255).toInt().coerceIn(0, 255)
                    textPaint.textSize = cmd.size
                    textPaint.typeface = if (cmd.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    textPaint.textAlign = when (cmd.align) {
                        TextAlign.LEFT -> Paint.Align.LEFT
                        TextAlign.CENTER -> Paint.Align.CENTER
                        TextAlign.RIGHT -> Paint.Align.RIGHT
                    }
                    canvas.drawText(cmd.text, cmd.x, cmd.y, textPaint)
                }

                is RenderCommand.SetScreenShake -> {
                    // Use save/translate instead of setMatrix to preserve the density scale
                    val offsetX = (random.nextFloat() - 0.5f) * cmd.magnitude * 2f
                    val offsetY = (random.nextFloat() - 0.5f) * cmd.magnitude * 2f
                    canvas.save()
                    canvas.translate(offsetX, offsetY)
                }

                is RenderCommand.ResetTransform -> {
                    // Restore to the density-scaled state (undo screen shake)
                    canvas.restore()
                    canvas.save()
                }

                is RenderCommand.DrawFlash -> {
                    paint.style = Paint.Style.FILL
                    paint.color = Color.argb((cmd.alpha * 255).toInt().coerceIn(0, 255), 255, 255, 255)
                    // Use game dimensions instead of canvas pixel dimensions
                    canvas.drawRect(0f, 0f, gameWidth, gameHeight, paint)
                }
            }
        }
    }

    private fun colorWithAlpha(color: Long, alpha: Float): Int {
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        val r = ((color shr 16) and 0xFF).toInt()
        val g = ((color shr 8) and 0xFF).toInt()
        val b = (color and 0xFF).toInt()
        return Color.argb(a, r, g, b)
    }
}
