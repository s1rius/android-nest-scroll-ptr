package wtf.s1.android.ptr.demo.nestviewpager

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.max

class FixVerticalLinearLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attrs, defStyleAttr) {

    companion object {
        const val TAG = "UseMatchLayout"
        const val DEBUG = true
    }

    init {
        orientation = VERTICAL

    }

    var mMoreTotalLength: Int = 0
    var parentHeight : Int = 0


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val pH = MeasureSpec.getSize(heightMeasureSpec)
        if (DEBUG) Log.i(TAG, "measureSpec ${MeasureSpec.getSize(widthMeasureSpec)} " +
                "${pH} parent measure height ${(parent as? View)?.measuredHeight}" +
                " root view ${rootView.measuredHeight}")
        if (pH > 0) {
            parentHeight = pH
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (orientation == VERTICAL) {
            mMoreTotalLength = 0
            mMoreTotalLength = paddingTop + paddingBottom

            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child != null) {
                    val lp = child.layoutParams as LayoutParams
                    mMoreTotalLength = max(mMoreTotalLength, mMoreTotalLength + child.measuredHeight + lp.topMargin + lp.bottomMargin)
                }
            }
            setMeasuredDimension(measuredWidth, mMoreTotalLength)
            if (DEBUG) {

                Log.i(TAG, "total height $mMoreTotalLength")
            }
        }

        if (DEBUG) {
            Log.i(TAG, "current height = $measuredHeight")
        }
    }

    override fun measureChild(child: View?, parentWidthMeasureSpec: Int, parentHeightMeasureSpec: Int) {
        super.measureChild(child, parentWidthMeasureSpec, parentHeightMeasureSpec)
    }

    override fun measureChildWithMargins(child: View?, parentWidthMeasureSpec: Int, widthUsed: Int, parentHeightMeasureSpec: Int, heightUsed: Int) {
        if (child == null) {
            return
        }
        val lp = (child.layoutParams) as MarginLayoutParams
        if (lp.height == LayoutParams.MATCH_PARENT) {
            val childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                    child.paddingLeft + child.paddingRight + lp.leftMargin + lp.rightMargin
                            + widthUsed, lp.width)
            val childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                    (child.paddingTop + child.paddingBottom + lp.topMargin + lp.bottomMargin + heightUsed),
                    MeasureSpec.getSize(parentHeightMeasureSpec))

            Log.i(TAG, "${MeasureSpec.getSize(parentHeightMeasureSpec)}  cur ${MeasureSpec.getSize(parentHeight)}")
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
        } else {
            super.measureChildWithMargins(child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed)
        }
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return super.generateDefaultLayoutParams()
    }
}