package applikeysolutions.com.moonrefresh

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import applikeysolutions.com.moonrefresh.Constants.Companion.DURATION_FINISH
import applikeysolutions.com.moonrefresh.Constants.Companion.STROKE_WIDTH
import applikeysolutions.com.moonrefresh.Constants.Companion.TARGET_DEGREE
import java.util.*


class MoonRefreshView constructor(layout: MoonRefresh) : Drawable(), Animatable {

    private val path = Path()
    private val backPaint = Paint()
    private val frontPaint = Paint()
    private val outerPaint = Paint()
    private val finishPaint = Paint()

    private val listOfPoints = ArrayList<PointF>()

    private var waveHeight = 0f
    private var springRatio = 0f
    private var finishRatio = 0f
    private var bollY = 0f
    private var bollRadius = 0f
    private var headHeight = 0
    private var screenWidth = 0
    private var refreshStop = 90
    private var refreshStart = 90
    private var inte = 0
    private var showBoll = false
    private var showOuter = false
    private var showBollTail = false
    private var outerIsStart = true

    init {
        layout.post { initiateDimens(layout.width) }

        backPaint.isAntiAlias = true
        frontPaint.isAntiAlias = true
        with(finishPaint) {
            isAntiAlias = true
            strokeWidth = STROKE_WIDTH.toFloat()
        }

        with(outerPaint) {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = STROKE_WIDTH.toFloat()
        }
    }

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, top)
    }

    override fun isRunning(): Boolean {
        return true
    }

    override fun start() {}

    override fun stop() {}

    override fun draw(canvas: Canvas) {
        drawWave(canvas, headHeight)
        drawSpringUp(canvas)
        drawBoll(canvas)
        drawOuter(canvas)
        drawFinish(canvas)
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(cf: ColorFilter?) {}

    fun setBackgroundColor(backgroundColor: Int) {
        backPaint.color = backgroundColor
    }

    fun setBallColor(ballColor: Int) {
        frontPaint.color = ballColor
        outerPaint.color = ballColor
    }

    fun setFinishColor(finishColor: Int) {
        finishPaint.color = finishColor
    }

    fun onPullingDown(headHeight: Int, offset: Int) {
        this.headHeight = headHeight
        waveHeight = Math.max(offset - headHeight, 0) * .8f
    }

    fun onPullingTop(headHeight: Int) {
        this.headHeight = headHeight
        invalidateSelf()
    }

    fun onRefreshReleased() {
        bollRadius = (headHeight / 6).toFloat()
        val interpolator = DecelerateInterpolator()
        val reboundHeight = Math.min(waveHeight * 0.8f, (headHeight / 2).toFloat())
        val waveAnimator = ValueAnimator.ofFloat(
                waveHeight, 0f,
                -(reboundHeight * 1.0f), 0f,
                -(reboundHeight * 0.4f), 0f
        )
        waveAnimator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            var speed = 0f
            var springBollY: Float = 0.toFloat()
            var springRatio = 0f
            var springStatus = 0

            override fun onAnimationUpdate(animation: ValueAnimator) {
                val curValue = animation.animatedValue as Float
                if (springStatus == 0 && curValue <= 0) {
                    springStatus = 1
                    speed = Math.abs(curValue - waveHeight)
                }
                if (springStatus == 1) {
                    springRatio = -curValue / reboundHeight
                    if (springRatio >= this@MoonRefreshView.springRatio) {
                        this@MoonRefreshView.springRatio = springRatio
                        bollY = headHeight + curValue
                        speed = Math.abs(curValue - waveHeight)
                    } else {
                        springStatus = 2
                        this@MoonRefreshView.springRatio = 0f
                        showBoll = true
                        showBollTail = true
                        springBollY = bollY
                    }
                }
                if (springStatus == 2) {
                    if (bollY > headHeight / 2) {
                        bollY = Math.max((headHeight / 2).toFloat(), bollY - speed)
                        val bolly = animation.animatedFraction * (headHeight / 2 - springBollY) + springBollY
                        if (bollY > bolly) {
                            bollY = bolly
                        }
                    }
                }
                if (showBollTail && curValue < waveHeight) {
                    showOuter = true
                    showBollTail = false
                    outerIsStart = true
                    refreshStart = 90
                    refreshStop = 90
                }
                waveHeight = curValue
                invalidateSelf()
            }
        })
        waveAnimator.interpolator = interpolator
        waveAnimator.duration = 1000
        waveAnimator.start()
    }

    fun onFinish(callback: FinishCallback) {
        inte = 0
        showOuter = false
        showBoll = false
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.addUpdateListener { animation ->
            finishRatio = animation.animatedValue as Float
            invalidateSelf()
        }
        animator.interpolator = AccelerateInterpolator()
        animator.duration = DURATION_FINISH.toLong()
        animator.start()
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                callback.apply()
            }

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    fun invalidate() {
        invalidateSelf()
    }

    private fun initiateDimens(viewWidth: Int) {
        if (viewWidth <= 0 || viewWidth == screenWidth) return
        screenWidth = viewWidth
    }

    private fun drawFinish(canvas: Canvas) {
        if (finishRatio > 0) {
            val beforeColor = outerPaint.color
            if (finishRatio < 0.3) {
                canvas.drawCircle((canvas.width / 2).toFloat(), bollY, bollRadius, frontPaint)
                val outerR = (bollRadius + outerPaint.strokeWidth * 2f * (1 + finishRatio / 0.3f)).toInt()
                outerPaint.color = finishPaint.color
                canvas.drawArc(RectF((canvas.width / 2 - outerR).toFloat(), bollY - outerR,
                        (canvas.width / 2 + outerR).toFloat(), bollY + outerR), 0f,
                        360f, false, outerPaint)

                drawLine(canvas, canvas.width / 2 + bollRadius / 2,
                        bollY - bollRadius / 5, canvas.width / 2 - bollRadius / 15,
                        bollY + bollRadius / 3, canvas.width / 2 - bollRadius / 15,
                        bollY + bollRadius / 3, canvas.width / 2 - bollRadius / 3,
                        bollY + bollRadius / 10)
            }
            outerPaint.color = beforeColor


            if (finishRatio >= 0.3 && finishRatio < 0.7) {
                val fraction = (finishRatio - 0.3f) / 0.4f
                bollY = (headHeight / 2 + (headHeight - headHeight / 2) * fraction).toInt().toFloat()
                canvas.drawCircle((canvas.width / 2).toFloat(), bollY, bollRadius, frontPaint)
                if (bollY >= headHeight - bollRadius * 2) {
                    showBollTail = true
                    drawBollTail(canvas, canvas.width, fraction)
                }
                showBollTail = false
            }

            if (finishRatio in 0.7..1.0) {
                val fraction = (finishRatio - 0.7f) / 0.3f
                val leftX = ((canvas.width / 2).toFloat() - bollRadius - 2f * bollRadius * fraction).toInt()
                path.reset()
                path.moveTo(leftX.toFloat(), headHeight.toFloat())
                path.quadTo((canvas.width / 2).toFloat(), headHeight - bollRadius * (1 - fraction),
                        (canvas.width - leftX).toFloat(), headHeight.toFloat())
                canvas.drawPath(path, frontPaint)
            }
        }
    }

    private fun drawWave(canvas: Canvas, viewHeight: Int) {
        val baseHeight = Math.min(headHeight, viewHeight).toFloat()
        if (waveHeight != 0f) {
            path.reset()
            path.lineTo(canvas.width.toFloat(), 0f)
            path.lineTo(canvas.width.toFloat(), baseHeight)
            path.quadTo((canvas.width / 2).toFloat(), baseHeight + waveHeight * 2, 0f, baseHeight)
            path.close()
            canvas.drawPath(path, backPaint)
        } else {
            canvas.drawRect(0f, 0f, canvas.width.toFloat(), baseHeight, backPaint)
        }
    }

    private fun drawSpringUp(canvas: Canvas) {
        if (springRatio > 0) {
            val leftX = canvas.width / 2 - 4 * bollRadius + springRatio * 3f * bollRadius
            if (springRatio < 0.9) {
                path.reset()
                path.moveTo(leftX, bollY)
                path.quadTo((canvas.width / 2).toFloat(), bollY - bollRadius * springRatio * 2f,
                        canvas.width - leftX, bollY)
                canvas.drawPath(path, frontPaint)
            } else {
                canvas.drawCircle((canvas.width / 2).toFloat(), bollY, bollRadius, frontPaint)
            }
        }
    }

    private fun drawBoll(canvas: Canvas) {
        if (showBoll) {
            canvas.drawCircle((canvas.width / 2).toFloat(), bollY, bollRadius, frontPaint)
            drawBollTail(canvas, canvas.width, (headHeight + waveHeight) / headHeight)
        }
    }

    private fun drawBollTail(canvas: Canvas, viewWidth: Int, fraction: Float) {
        if (showBollTail) {
            val bottom = headHeight + waveHeight
            val startY = bollY + bollRadius * fraction / 2
            val startX = viewWidth / 2 + Math.sqrt((bollRadius * bollRadius * (1 - fraction * fraction / 4)).toDouble()).toFloat()
            val bezier1x = viewWidth / 2 + bollRadius * 3 / 4 * (1 - fraction)
            val bezier2x = bezier1x + bollRadius

            path.reset()
            path.moveTo(startX, startY)
            path.quadTo(bezier1x, bottom, bezier2x, bottom)
            path.lineTo(viewWidth - bezier2x, bottom)
            path.quadTo(viewWidth - bezier1x, bottom, viewWidth - startX, startY)
            canvas.drawPath(path, frontPaint)
        }
    }

    private fun drawOuter(canvas: Canvas) {
        if (showOuter) {
            val outerR = bollRadius + outerPaint.strokeWidth * 2

            refreshStart += if (outerIsStart) 3 else 10
            refreshStop += if (outerIsStart) 10 else 3
            refreshStart %= 360
            refreshStop %= 360

            var swipe = refreshStop - refreshStart
            swipe = if (swipe < 0) swipe + 360 else swipe

            canvas.drawArc(RectF(canvas.width / 2 - outerR, bollY - outerR,
                    canvas.width / 2 + outerR, bollY + outerR), refreshStart.toFloat(),
                    swipe.toFloat(), false, outerPaint)
            if (swipe >= TARGET_DEGREE) {
                outerIsStart = false
            } else if (swipe <= 10) {
                outerIsStart = true
            }
            invalidateSelf()
        }

    }

    private fun drawLine(canvas: Canvas, startFirstLineX: Float, startFirstLineY: Float,
                         finishFirstLineX: Float, finishFirstLineY: Float, startSecLineX: Float,
                         startSecLineY: Float, finishSecLineX: Float, finishSecLineY: Float) {

        fillPointsToDraw(
                startFirstLineX, startFirstLineY, finishFirstLineX, finishFirstLineY, startSecLineX,
                startSecLineY, finishSecLineX, finishSecLineY)

        val myPaint = Paint()
        with(myPaint) {
            color = finishPaint.color
            strokeWidth = 5f
        }
        if (inte < listOfPoints.size / 2) {
            canvas.drawLine(listOfPoints[0].x, listOfPoints[0].y, listOfPoints[inte].x, listOfPoints[inte].y, myPaint)
            inte++
        } else {
            canvas.drawLine(startFirstLineX, startFirstLineY, finishFirstLineX, finishFirstLineY, myPaint)
            if (inte >= listOfPoints.size / 2 && inte < listOfPoints.size) {
                canvas.drawLine(listOfPoints[6].x, listOfPoints[6].y, listOfPoints[inte].x, listOfPoints[inte].y, myPaint)
                inte++
                canvas.drawLine(startFirstLineX, startFirstLineY, finishFirstLineX, finishFirstLineY, myPaint)
            } else
                canvas.drawLine(startSecLineX, startSecLineY, finishSecLineX, finishSecLineY, myPaint)
        }
    }

    private fun fillPointsToDraw(startFirstLineX: Float, startFirstLineY: Float, finishFirstLineX: Float,
                                 finishFirstLineY: Float, startSecLineX: Float, startSecLineY: Float,
                                 finishSecLineX: Float, finishSecLineY: Float) {
        listOfPoints.clear()
        (1..6).mapTo(listOfPoints) { PointF(startFirstLineX + it * (finishFirstLineX - startFirstLineX) / 6, startFirstLineY + it * (finishFirstLineY - startFirstLineY) / 6) }

        (1..6).mapTo(listOfPoints) { PointF(startSecLineX + it * (finishSecLineX - startSecLineX) / 6, startSecLineY + it * (finishSecLineY - startSecLineY) / 6) }
    }

    interface FinishCallback {
        fun apply()
    }
}
