package dev.kobalt.holdem.android.view

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import dev.kobalt.holdem.android.R

/* https://github.com/andreilisun/Circular-Layout */
class CircularView : ViewGroup {
    private val BAD_ANGLE = -1.0
    private var maxWidth = 0
    private var maxHeight = 0
    private val childRect = Rect()
    private val childCenter = Point()
    private val center = Point()
    private var angleInRadians = BAD_ANGLE
    private var inflatedChildCount = 0

    constructor(context: Context?) : super(context) {}
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet) {
        val typedArray = context
            .obtainStyledAttributes(attrs, R.styleable.CircularView)
        val capacity = typedArray.getInteger(R.styleable.CircularView_capacity, 0)
        if (capacity != 0) {
            setCapacity(capacity)
        }
        /*angle attr always wins*/
        val angle = typedArray.getFloat(
            R.styleable.CircularView_angle,
            BAD_ANGLE.toFloat()
        )
        if (angle.toDouble() != BAD_ANGLE) {
            setAngle(angle.toDouble())
        }
        typedArray.recycle()
    }

    fun setAngle(degrees: Double) {
        angleInRadians = degreesToRadians(degrees)
        requestLayout()
    }

    fun setCapacity(expectedViewsQuantity: Int) {
        angleInRadians = degreesToRadians(360.0 / expectedViewsQuantity)
        requestLayout()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        inflatedChildCount = childCount
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val childCount = childCount
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec)
                maxWidth = Math.max(maxWidth, child.measuredWidth)
                maxHeight = Math.max(maxHeight, child.measuredHeight)
            }
        }
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(b: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val childCount = childCount

        /*if angle hasn't been set, try to calculate it
        taking into account how many items declared
        in xml inside CircularLayout*/if (angleInRadians == BAD_ANGLE && inflatedChildCount > 0) {
            setCapacity(childCount)
        }
        if (angleInRadians == BAD_ANGLE) {
            throw IllegalStateException(
                "set angle or capacity first with " +
                        "setAngle(double degrees)/setCapacity(int expectedViewsQuantity) or " +
                        "with xml angle/capacity attrs."
            )
        }
        val width = right - left
        val height = bottom - top
        if (width != height) {
            throw IllegalStateException("width should be the same as height")
        }
        val radius = (width / 2) - Math.max(maxWidth / 2, maxHeight / 2) - maxPadding()
        center[width / 2] = width / 2
        var firstIsLaidOut = false
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                if (!firstIsLaidOut) {
                    childCenter.x = center.x
                    childCenter.y = center.y - radius
                    firstIsLaidOut = true
                } else {
                    val deltaX = childCenter.x - center.x
                    val deltaY = childCenter.y - center.y
                    val cos = Math.cos(angleInRadians)
                    val sin = Math.sin(angleInRadians)
                    childCenter.x = (center.x + deltaX * cos - deltaY * sin).toInt()
                    childCenter.y = (center.y + (deltaX * sin) + (deltaY * cos)).toInt()
                }
                layoutChild(child)
            }
        }
    }

    private fun maxPadding(): Int {
        val paddingTop = paddingTop
        val paddingBottom = paddingBottom
        val paddingLeft = paddingLeft
        val paddingRight = paddingRight
        return Math.max(Math.max(Math.max(paddingTop, paddingBottom), paddingLeft), paddingRight)
    }

    private fun layoutChild(child: View) {
        val childWidth = child.measuredWidth
        val childHeight = child.measuredHeight
        childRect.top = childCenter.y - childHeight / 2
        childRect.left = childCenter.x - childWidth / 2
        childRect.right = childRect.left + childWidth
        childRect.bottom = childRect.top + childHeight
        child.layout(childRect.left, childRect.top, childRect.right, childRect.bottom)
    }

    private fun degreesToRadians(degrees: Double): Double {
        return (degrees * Math.PI / 180)
    }
}