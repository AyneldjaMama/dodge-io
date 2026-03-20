package io.dodge.engine

enum class TextAlign { LEFT, CENTER, RIGHT }

sealed interface RenderCommand {
    data class ClearBackground(val color: Long) : RenderCommand
    data class DrawGrid(val w: Float, val h: Float, val spacing: Float, val color: Long) : RenderCommand

    data class DrawCircle(
        val x: Float, val y: Float, val radius: Float,
        val color: Long, val alpha: Float = 1f
    ) : RenderCommand

    data class DrawGlowCircle(
        val x: Float, val y: Float,
        val innerRadius: Float, val outerRadius: Float,
        val color: Long, val alpha: Float = 0.15f
    ) : RenderCommand

    data class DrawRing(
        val x: Float, val y: Float, val radius: Float,
        val color: Long, val alpha: Float, val lineWidth: Float,
        val dashPattern: FloatArray? = null
    ) : RenderCommand

    data class DrawArc(
        val x: Float, val y: Float, val radius: Float,
        val startAngle: Float, val sweepAngle: Float,
        val color: Long, val lineWidth: Float
    ) : RenderCommand

    data class DrawLaser(
        val x: Float, val y: Float,
        val angle: Float, val length: Float, val width: Float,
        val age: Int, val chargeFrames: Int
    ) : RenderCommand

    data class DrawParticle(
        val x: Float, val y: Float, val size: Float,
        val r: Int, val g: Int, val b: Int, val alpha: Float
    ) : RenderCommand

    data class DrawPowerUp(
        val x: Float, val y: Float, val radius: Float,
        val label: String, val pulse: Float, val alpha: Float
    ) : RenderCommand

    data class DrawText(
        val text: String, val x: Float, val y: Float,
        val color: Long, val alpha: Float, val size: Float,
        val bold: Boolean = false, val align: TextAlign = TextAlign.CENTER
    ) : RenderCommand

    data class SetScreenShake(val magnitude: Float) : RenderCommand
    data class DrawFlash(val alpha: Float) : RenderCommand
    data object ResetTransform : RenderCommand
}
