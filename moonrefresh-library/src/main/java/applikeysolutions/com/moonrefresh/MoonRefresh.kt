package applikeysolutions.com.moonrefresh

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.support.annotation.ColorInt
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.Transformation
import android.widget.ImageView
import applikeysolutions.com.moonrefresh.Constants.Companion.DECELERATE_INTERPOLATION_FACTOR
import applikeysolutions.com.moonrefresh.Constants.Companion.DRAG_MAX_DISTANCE
import applikeysolutions.com.moonrefresh.Constants.Companion.DRAG_RATE
import applikeysolutions.com.moonrefresh.Constants.Companion.INVALID_POINTER
import applikeysolutions.com.moonrefresh.Constants.Companion.MAX_OFFSET_ANIMATION_DURATION


class MoonRefresh constructor(context: Context, attrs: AttributeSet? = null) : ViewGroup(context, attrs) {

    private val refreshView: ImageView
    private val interpolator = DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR)
    private val touchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop
    private var totalDragDistance = 0

    private var target: View? = null
    private var onRefreshListener: OnRefreshListener? = null
    private lateinit var moonRefreshView: MoonRefreshView

    private var currentDragPercent = 0f
    private var initialMotionY = 0f
    private var fromDragPercent = 0f
    private var currentOffsetTop = 0
    private var activePointerId = 0
    private var from = 0
    private var targetPaddingTop = 0
    private var targetPaddingBottom = 0
    private var targetPaddingRight = 0
    private var targetPaddingLeft = 0
    private var isBeingDragged = false
    private var notify = false
    private var refreshing = false
    private var refreshReleased = false

    private val animateToStartPosition = object : Animation() {
        public override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            moveToStart(interpolatedTime)
            moonRefreshView.onPullingTop(target!!.top)
        }
    }

    private val animateToCorrectPosition = object : Animation() {
        public override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            val targetTop = from + ((totalDragDistance - from) * interpolatedTime).toInt()
            val offset = targetTop - target!!.top

            currentDragPercent = fromDragPercent - (fromDragPercent - .5f) * interpolatedTime
            setTargetOffsetTop(offset)
        }
    }

    init {
        val obtainStyledAttributes = context.obtainStyledAttributes(attrs, R.styleable.MoonRefresh)
        moonRefreshView = MoonRefreshView(this)
        with(moonRefreshView) {
            setBackgroundColor(
                    obtainStyledAttributes.getColor(
                            R.styleable.MoonRefresh_backgroundColor,
                            ContextCompat.getColor(context, R.color.background)))
            setBallColor(
                    obtainStyledAttributes.getColor(
                            R.styleable.MoonRefresh_ballColor,
                            Color.WHITE))
            setFinishColor(
                    obtainStyledAttributes.getColor(
                            R.styleable.MoonRefresh_finishColor,
                            ContextCompat.getColor(context, R.color.finish)))
        }
        totalDragDistance = Utils.convertDpToPixel(context, DRAG_MAX_DISTANCE)
        refreshView = ImageView(context)
        refreshView.setImageDrawable(moonRefreshView)
        addView(refreshView)
        setWillNotDraw(false)
        ViewCompat.setChildrenDrawingOrderEnabled(this, true)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isEnabled || canChildScrollUp() || refreshing) {
            return false
        }

        val action = ev.actionMasked

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                setTargetOffsetTop(0)
                activePointerId = ev.getPointerId(0)
                isBeingDragged = false
                val initialMotionY = getMotionEventY(ev, activePointerId)
                if (initialMotionY == -1f) {
                    return false
                }
                this.initialMotionY = initialMotionY
            }
            MotionEvent.ACTION_MOVE -> {
                if (activePointerId == INVALID_POINTER) {
                    return false
                }
                val y = getMotionEventY(ev, activePointerId)
                if (y == -1f) {
                    return false
                }
                val yDiff = y - this.initialMotionY
                if (yDiff > touchSlop && !isBeingDragged) {
                    isBeingDragged = true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isBeingDragged = false
                activePointerId = INVALID_POINTER
            }
            MotionEvent.ACTION_POINTER_UP ->
                onSecondaryPointerUp(ev)
        }
        return isBeingDragged
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!isBeingDragged) {
            return super.onTouchEvent(ev)
        }

        val action = ev.actionMasked

        when (action) {
            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex < 0) {
                    return false
                }

                val y = ev.getY(pointerIndex)
                val yDiff = y - initialMotionY
                val scrollTop = yDiff * DRAG_RATE
                currentDragPercent = scrollTop / totalDragDistance
                if (currentDragPercent < 0) {
                    return false
                }
                val boundedDragPercent = Math.min(1f, Math.abs(currentDragPercent))
                val extraOS = Math.abs(scrollTop) - totalDragDistance
                val slingshotDist = totalDragDistance.toFloat()
                val tensionSlingshotPercent = Math.max(0f,
                        Math.min(extraOS, slingshotDist * 2) / slingshotDist)
                val tensionPercent = (tensionSlingshotPercent / 4 - Math.pow(
                        (tensionSlingshotPercent / 4).toDouble(), 2.0)).toFloat() * 2f
                val extraMove = slingshotDist * tensionPercent / 2
                val targetY = (slingshotDist * boundedDragPercent + extraMove).toInt()

                moonRefreshView.invalidate()
                if (extraOS > 0) {
                    refreshReleased = true
                    moonRefreshView.onPullingDown(totalDragDistance, totalDragDistance + extraOS.toInt() / 6)
                } else {
                    moonRefreshView.onPullingDown(targetY, 0)
                    refreshReleased = false
                }
                setTargetOffsetTop(targetY - currentOffsetTop)
            }
            MotionEvent.ACTION_UP -> {
                if (refreshReleased)
                    moonRefreshView.onRefreshReleased()
                run {
                    if (activePointerId == INVALID_POINTER) {
                        return false
                    }

                    val pointerIndex = ev.findPointerIndex(activePointerId)
                    val y = ev.getY(pointerIndex)
                    val overScrollTop = (y - initialMotionY) * DRAG_RATE
                    isBeingDragged = false
                    if (overScrollTop > totalDragDistance) {
                        setRefreshing(true, true)
                    } else {
                        refreshing = false
                        animateOffsetToStartPosition()
                    }
                    activePointerId = INVALID_POINTER
                    return false
                }
            }

            MotionEvent.ACTION_POINTER_UP ->
                onSecondaryPointerUp(ev)

            MotionEvent.ACTION_CANCEL -> {
                if (activePointerId == INVALID_POINTER) {
                    return false
                }
                val pointerIndex = ev.findPointerIndex(activePointerId)
                val y = ev.getY(pointerIndex)
                val overScrollTop = (y - initialMotionY) * DRAG_RATE
                isBeingDragged = false
                if (overScrollTop > totalDragDistance) {
                    setRefreshing(true, true)
                } else {
                    refreshing = false
                    animateOffsetToStartPosition()
                }
                activePointerId = INVALID_POINTER
                return false
            }
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthSpec = widthMeasureSpec
        var heightSpec = heightMeasureSpec
        super.onMeasure(widthSpec, heightSpec)

        ensureTarget()
        if (target == null)
            return

        widthSpec = View.MeasureSpec.makeMeasureSpec(measuredWidth - paddingRight - paddingLeft, View.MeasureSpec.EXACTLY)
        heightSpec = View.MeasureSpec.makeMeasureSpec(measuredHeight - paddingTop - paddingBottom, View.MeasureSpec.EXACTLY)
        target!!.measure(widthSpec, heightSpec)
        refreshView.measure(widthSpec, heightSpec)
    }

    fun setOnRefreshListener(listener: OnRefreshListener) {
        onRefreshListener = listener
    }

    fun setRefreshing() {
        if (refreshing) {
            setRefreshing(false, false /* notify */)
        }
    }

    private fun animateOffsetToStartPosition() {
        from = currentOffsetTop
        fromDragPercent = currentDragPercent
        val animationDuration = Math.abs((MAX_OFFSET_ANIMATION_DURATION * fromDragPercent).toLong())

        animateToStartPosition.reset()
        animateToStartPosition.duration = animationDuration
        animateToStartPosition.interpolator = interpolator
        refreshView.clearAnimation()
        refreshView.startAnimation(animateToStartPosition)
    }

    private fun animateOffsetToCorrectPosition() {
        from = currentOffsetTop
        fromDragPercent = currentDragPercent

        animateToCorrectPosition.reset()
        animateToCorrectPosition.duration = MAX_OFFSET_ANIMATION_DURATION.toLong()
        animateToCorrectPosition.interpolator = interpolator
        refreshView.clearAnimation()
        refreshView.startAnimation(animateToCorrectPosition)

        if (notify) {
            if (onRefreshListener != null) {
                onRefreshListener!!.onRefresh()
            }
        }

        currentOffsetTop = target!!.top
        target!!.setPadding(targetPaddingLeft, targetPaddingTop, targetPaddingRight, totalDragDistance)
    }

    private fun ensureTarget() {
        if (target != null)
            return
        if (childCount > 0) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child !== refreshView) {
                    target = child
                    targetPaddingBottom = target!!.paddingBottom
                    targetPaddingLeft = target!!.paddingLeft
                    targetPaddingRight = target!!.paddingRight
                    targetPaddingTop = target!!.paddingTop
                }
            }
        }
    }

    private fun moveToStart(interpolatedTime: Float) {
        val targetTop = from - (from * interpolatedTime).toInt()
        val targetPercent = fromDragPercent * (1.0f - interpolatedTime)
        val offset = targetTop - target!!.top

        currentDragPercent = targetPercent
        target!!.setPadding(targetPaddingLeft, targetPaddingTop, targetPaddingRight, targetPaddingBottom + targetTop)
        setTargetOffsetTop(offset)
    }

    private fun setRefreshing(refreshing: Boolean, notify: Boolean) {
        if (this.refreshing != refreshing) {
            this.notify = notify
            ensureTarget()
            this.refreshing = refreshing
            if (this.refreshing) {
                animateOffsetToCorrectPosition()
            } else {
                moonRefreshView.onFinish(object : MoonRefreshView.FinishCallback {
                    override fun apply() {
                        refreshReleased = false
                        animateOffsetToStartPosition()
                    }
                })
            }
        }
    }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex = ev.actionIndex
        val pointerId = ev.getPointerId(pointerIndex)
        if (pointerId == activePointerId) {
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            activePointerId = ev.getPointerId(newPointerIndex)
        }
    }

    private fun getMotionEventY(ev: MotionEvent, activePointerId: Int): Float {
        val index = ev.findPointerIndex(activePointerId)
        return if (index < 0) {
            -1f
        } else ev.getY(index)
    }

    private fun setTargetOffsetTop(offset: Int) {
        target!!.offsetTopAndBottom(offset)
        currentOffsetTop = target!!.top
    }

    private fun canChildScrollUp(): Boolean {
        return target!!.canScrollVertically(-1)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        ensureTarget()
        if (target == null)
            return

        val height = measuredHeight
        val width = measuredWidth
        val left = paddingLeft
        val top = paddingTop
        val right = paddingRight
        val bottom = paddingBottom

        target!!.layout(left, top + currentOffsetTop, left + width - right, top + height - bottom + currentOffsetTop)
        refreshView.layout(left, top, left + width - right, top + height - bottom)
    }

    interface OnRefreshListener {
        fun onRefresh()
    }
}
