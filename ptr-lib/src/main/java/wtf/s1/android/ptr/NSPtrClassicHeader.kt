package wtf.s1.android.ptr

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.ViewCompat

open class NSPtrClassicHeader @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), NSPtrComponent {

    private var mRotateAniTime = 150
    private var mFlipAnimation: RotateAnimation? = null
    private var mReverseFlipAnimation: RotateAnimation? = null
    private var mTitleTextView: TextView? = null
    private var mRotateView: View? = null
    private var mProgressBar: View? = null
    private var mIsOverToRefresh = false

    init {
        initViews()
    }

    private fun initViews() {
        buildAnimation()
        val header = LayoutInflater.from(context).inflate(R.layout.ns_ptr_classic_default_header, this)
        mRotateView = header.findViewById(R.id.ptr_classic_header_rotate_view)
        mTitleTextView = header.findViewById<View>(R.id.ptr_classic_header_rotate_view_header_title) as TextView
        mProgressBar = header.findViewById(R.id.ptr_classic_header_rotate_view_progressbar)
        resetView()
    }

    private fun buildAnimation() {
        mFlipAnimation = RotateAnimation(0f, (-180).toFloat(), RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f)
        mFlipAnimation?.interpolator = LinearInterpolator()
        mFlipAnimation?.duration = mRotateAniTime.toLong()
        mFlipAnimation?.fillAfter = true
        mReverseFlipAnimation = RotateAnimation((-180).toFloat(), 0f, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f)
        mReverseFlipAnimation?.interpolator = LinearInterpolator()
        mReverseFlipAnimation?.duration = mRotateAniTime.toLong()
        mReverseFlipAnimation?.fillAfter = true
    }

    private fun resetView() {
        hideRotateView()
        mProgressBar?.visibility = INVISIBLE
    }

    private fun hideRotateView() {
        mRotateView?.clearAnimation()
        mRotateView?.visibility = INVISIBLE
    }

    override fun onDrag(frame: NSPtrLayout) {
        mProgressBar?.visibility = INVISIBLE
        mRotateView?.visibility = VISIBLE
        mTitleTextView?.visibility = VISIBLE
        mTitleTextView?.text = resources.getString(R.string.cube_ptr_pull_down_to_refresh)
    }

    override fun onRefreshing(frame: NSPtrLayout) {
        hideRotateView()
        mProgressBar?.visibility = VISIBLE
        mTitleTextView?.visibility = VISIBLE
        mTitleTextView?.setText(R.string.cube_ptr_refreshing)
    }

    override fun onComplete(frame: NSPtrLayout) {
        hideRotateView()
        mProgressBar?.visibility = INVISIBLE
        mTitleTextView?.visibility = VISIBLE
        mTitleTextView?.text = resources.getString(R.string.cube_ptr_refresh_complete)
    }

    override fun onPositionChange(frame: NSPtrLayout) {
        if (frame.currentState == NSPtrLayout.State.DRAG) {
            if (mIsOverToRefresh != frame.isOverToRefreshPosition) {
                if (frame.isOverToRefreshPosition) {
                    crossRotateLineFromTopUnderTouch()
                    mRotateView?.let {
                        it.clearAnimation()
                        it.startAnimation(mFlipAnimation)
                    }
                } else {
                    crossRotateLineFromBottomUnderTouch()
                    mRotateView?.let {
                        it.clearAnimation()
                        it.startAnimation(mReverseFlipAnimation)
                    }
                }
                mIsOverToRefresh = frame.isOverToRefreshPosition
            }
        }
    }

    private fun crossRotateLineFromTopUnderTouch() {
        mTitleTextView?.visibility = VISIBLE
        mTitleTextView?.setText(R.string.cube_ptr_release_to_refresh)
    }

    private fun crossRotateLineFromBottomUnderTouch() {
        mTitleTextView?.visibility = VISIBLE
        mTitleTextView?.text = resources.getString(R.string.cube_ptr_pull_down_to_refresh)
    }

    override fun prtMeasure(ptrLayout: NSPtrLayout, widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun ptrLayout(ptrLayout: NSPtrLayout) {
        val lp = layoutParams as NSPtrLayout.LayoutParams
        val left = paddingLeft + lp.leftMargin
        // enhance readability(header is layout above screen when first init)
        val top = -(measuredHeight - paddingTop - lp.topMargin - ptrLayout.contentTopPosition)
        val right = left + measuredWidth
        val bottom = top + measuredHeight
        layout(left, top, right, bottom)
    }

    override fun ptrOnContentOffsetTopAndBottom(offset: Int) {
        ViewCompat.offsetTopAndBottom(this, offset)
    }
}