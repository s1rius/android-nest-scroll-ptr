package wtf.s1.ui.nsptr.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import wtf.s1.ui.nsptr.Event
import wtf.s1.ui.nsptr.SideEffect
import wtf.s1.ui.nsptr.State
import wtf.s1.ui.nsptr.StateMachine

/**
 * implement NSPtrHeader in an easy way
 */
open class NSPtrEZHeader @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), NSPtrHeader, NSPtrListener {

    private var mIsOverToRefresh = false
    private val progressBar: NSPtrProgressBar = NSPtrProgressBar(context).apply {
        addView(this, LayoutParams(toDp(30), toDp(30)).apply {
            gravity = Gravity.CENTER
            setMargins(0, toDp(15), 0, toDp(15))
        })
    }

    override fun onTransition(ptrLayout: NSPtrLayout,
                              transition: StateMachine.Transition.Valid<State, Event, SideEffect>) {
        super.onTransition(ptrLayout, transition)
        when (transition.toState) {
            is State.IDLE -> {
                if (transition.fromState == State.REFRESHING) {
                    progressBar.inProgress = false
                }
                progressBar.stop()
            }
            is State.REFRESHING -> {
                progressBar.animateProgress()
            }
            is State.DRAG -> {
                progressBar.inProgress = true
            }
        }
    }

    override fun onPositionChange(ptrLayout: NSPtrLayout, offset: Int) {
        if (ptrLayout.currentState != State.REFRESHING) {
            if (mIsOverToRefresh != ptrLayout.isOverToRefreshPosition) {
                mIsOverToRefresh = ptrLayout.isOverToRefreshPosition
            }
            progressBar.progress = (ptrLayout.contentTopPosition * 100f / ptrLayout.config.contentRefreshPosition()).toInt()
        }
    }

    override fun prtMeasure(ptrLayout: NSPtrLayout, parentWidthMeasureSpec: Int, parentHeightMeasureSpec: Int) {
        val lp = layoutParams as NSPtrLayout.LayoutParams
        val childWidthMeasureSpec = getChildMeasureSpec(
            parentWidthMeasureSpec,
            ptrLayout.paddingLeft + ptrLayout.paddingRight
                    + lp.leftMargin + lp.rightMargin,
            lp.width
        )
        val childHeightMeasureSpec = getChildMeasureSpec(
            parentHeightMeasureSpec,
            (ptrLayout.paddingTop + ptrLayout.paddingBottom
                    + lp.topMargin + lp.bottomMargin),
            lp.height
        )

        measure(childWidthMeasureSpec, childHeightMeasureSpec)
    }

    override fun ptrLayout(ptrLayout: NSPtrLayout) {
        val lp = layoutParams as NSPtrLayout.LayoutParams
        val left = ptrLayout.paddingLeft + lp.leftMargin
        val top = lp.topMargin + ptrLayout.paddingTop
        val right = left + measuredWidth
        val bottom = top + measuredHeight
        layout(left, top, right, bottom)
    }

    private fun toDp(dp: Int): Int {
        return toDp(dp, resources)
    }
}