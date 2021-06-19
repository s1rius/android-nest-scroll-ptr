package wtf.s1.android.ptr

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.abs
import kotlin.math.min

class NSPtrProgressBar @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var inProgress = false
    set(value) {
        if (value && isInAnimate) {
            stop()
        }
        if (!value) {
            progress = 0
        }
        field = value
        invalidate()
    }

    var progress: Int = 0
    set(value) {
        field = value
        invalidate()
    }

    var arcPaint = Paint().apply {
        color = Color.GRAY
        isAntiAlias = true
        style = Paint.Style.STROKE
    }
    var animatedFraction = 0f

    private var progressRectF = RectF()
    private var isInAnimate = false

    private val animator: ValueAnimator = ValueAnimator.ofFloat(1f).apply {
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        this.addUpdateListener {
            this@NSPtrProgressBar.animatedFraction = it.animatedFraction
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        arcPaint.strokeWidth = min(measuredHeight, measuredWidth) / 10f
        val fl = arcPaint.strokeWidth / 2
        val padding = abs((measuredWidth - measuredHeight) / 2f) + fl
        if (measuredWidth >= measuredHeight) {
            progressRectF.set(padding, fl, measuredWidth - padding, measuredHeight.toFloat() - fl)
        } else {
            progressRectF.set(fl, padding, measuredWidth.toFloat() - fl, measuredHeight - padding)
        }

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let {
            if (inProgress) {
                drawProgressArc(canvas)
            } else if (isInAnimate) {
                drawAnimateArc(canvas)
            }
        }

    }

    private fun drawProgressArc(canvas: Canvas) {
        canvas.drawArc(progressRectF,
                -90f,
                360f * progress / 100,
                false,
                arcPaint)
    }

    private fun drawAnimateArc(canvas: Canvas) {
        canvas.drawArc(progressRectF,
                -100f + 360 * animatedFraction,
                300f,
                false,
                arcPaint
        )
    }

    fun animateProgress() {
        inProgress = false
        isInAnimate = true
        animator.start()
    }

    fun stop() {
        isInAnimate = false
        animator.cancel()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }
}