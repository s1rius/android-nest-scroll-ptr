package wtf.s1.android.ptr

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout

open class NSPtrEZHeader @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), NSPtrComponent, NSPtrListener {

    private var mIsOverToRefresh = false
    private val progressBar: NSPtrProgressBar = NSPtrProgressBar(context).apply {
        addView(this, LayoutParams(toDp(30), toDp(30)).apply {
            gravity = Gravity.CENTER
            setMargins(0, toDp(15), 0, toDp(15))
        })
    }

    override fun onTransition(ptrLayout: NSPtrLayout,
                              transition: StateMachine.Transition.Valid<NSPtrLayout.State, NSPtrLayout.Event, NSPtrLayout.SideEffect>) {
        super.onTransition(ptrLayout, transition)
        when (transition.toState) {
            is NSPtrLayout.State.IDLE -> {
                if (transition.fromState == NSPtrLayout.State.REFRESHING) {
                    progressBar.inProgress = false
                }
                progressBar.stop()
            }
            is NSPtrLayout.State.REFRESHING -> {
                progressBar.animateProgress()
            }
            is NSPtrLayout.State.DRAG -> {
                progressBar.inProgress = true
            }
        }
    }

    override fun onPositionChange(frame: NSPtrLayout, offset: Int) {
        if (frame.currentState != NSPtrLayout.State.REFRESHING) {
            if (mIsOverToRefresh != frame.isOverToRefreshPosition) {
                mIsOverToRefresh = frame.isOverToRefreshPosition
            }
            progressBar.progress = (frame.contentTopPosition * 100f / frame.config.refreshPosition()).toInt()
        }
    }

    override fun prtMeasure(ptrLayout: NSPtrLayout, widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun ptrLayout(ptrLayout: NSPtrLayout) {
        val lp = layoutParams as NSPtrLayout.LayoutParams
        val left = paddingLeft + lp.leftMargin
        val top = 0
        val right = left + measuredWidth
        val bottom = top + measuredHeight
        layout(left, top, right, bottom)
    }

    private fun toDp(dp: Int): Int {
        return toDp(dp, resources)
    }
}