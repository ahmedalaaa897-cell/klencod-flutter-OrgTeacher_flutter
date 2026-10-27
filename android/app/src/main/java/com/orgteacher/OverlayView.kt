package com.orgteacher

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

/** طبقة شفافة بتغمّق الشاشة وتنوّر مكان الزرار المطلوب. */
class OverlayView(ctx: Context) : View(ctx) {
    var target: RectF? = null          // بإحداثيات الشاشة
    var label: String = ""
    var strong = false
    var hint: PointF? = null           // علامة وقت المعايرة
    var message: String? = null
    var onTap: ((Float, Float) -> Unit)? = null

    private val dim = Paint().apply { color = 0xB005070A.toInt() }
    private val clear = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 8f }
    private val pill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private var pulse = 0f
    private var downX = 0f
    private var downY = 0f
    private val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 700; repeatMode = ValueAnimator.REVERSE; repeatCount = ValueAnimator.INFINITE
        addUpdateListener { pulse = it.animatedValue as Float; invalidate() }
    }

    init { setLayerType(LAYER_TYPE_HARDWARE, null) }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); anim.start() }
    override fun onDetachedFromWindow() { anim.cancel(); super.onDetachedFromWindow() }

    override fun onDraw(c: Canvas) {
        val loc = IntArray(2); getLocationOnScreen(loc)
        val dx = loc[0].toFloat(); val dy = loc[1].toFloat()
        val layer = c.saveLayer(RectF(0f, 0f, width.toFloat(), height.toFloat()), null)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dim)
        val t = target?.let { RectF(it).apply { offset(-dx, -dy); inset(-10f, -10f) } }
        if (t != null) c.drawRoundRect(t, 18f, 18f, clear)
        c.restoreToCount(layer)

        val dens = resources.displayMetrics.density
        if (t != null) {
            ring.color = if (strong) 0xFFFF7A66.toInt() else Color.rgb(216, 179, 106)
            ring.alpha = (160 + 95 * pulse).toInt()
            c.drawRoundRect(t, 18f, 18f, ring)
            txt.textSize = 15f * dens
            val w = txt.measureText(label) + 28f * dens
            val h = 28f * dens
            val cx = (t.centerX()).coerceIn(w / 2 + 8f, width - w / 2 - 8f)
            val above = t.top > h + 24f
            val top = if (above) t.top - h - 12f else t.bottom + 12f
            pill.color = if (strong) 0xFFFF7A66.toInt() else Color.rgb(216, 179, 106)
            c.drawRoundRect(cx - w / 2, top, cx + w / 2, top + h, h / 2, h / 2, pill)
            txt.color = if (strong) Color.WHITE else 0xFF1A1405.toInt()
            c.drawText(label, cx, top + h / 2 + 5f * dens, txt)
        }
        hint?.let {
            val px = it.x - dx; val py = it.y - dy
            ring.color = Color.WHITE; ring.alpha = 255
            c.drawCircle(px, py, 36f, ring)
            c.drawLine(px - 60f, py, px + 60f, py, ring)
            c.drawLine(px, py - 60f, px, py + 60f, ring)
        }
        message?.let {
            txt.color = Color.WHITE; txt.textSize = 18f * dens
            c.drawText(it, width / 2f, height * 0.35f, txt)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX = e.rawX; downY = e.rawY }
            MotionEvent.ACTION_UP -> {
                if (Math.hypot((e.rawX - downX).toDouble(), (e.rawY - downY).toDouble()) < 40) onTap?.invoke(e.rawX, e.rawY)
            }
        }
        return true
    }
}
