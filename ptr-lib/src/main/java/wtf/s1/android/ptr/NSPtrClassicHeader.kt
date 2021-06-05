package wtf.s1.android.ptr

import android.annotation.SuppressLint
import android.content.Context
import android.widget.FrameLayout
import android.view.animation.RotateAnimation
import android.view.LayoutInflater
import android.view.animation.LinearInterpolator
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

open class NSPtrClassicHeader @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), NSPtrListener, NSPtrComponent {

    private var mRotateAniTime = 150
    private var mFlipAnimation: RotateAnimation? = null
    private var mReverseFlipAnimation: RotateAnimation? = null
    private var mTitleTextView: TextView? = null
    private var mRotateView: View? = null
    private var mProgressBar: View? = null
    private var mLastUpdateTime: Long = -1
    private var mLastUpdateTextView: TextView? = null
    private var mLastUpdateTimeKey: String? = null
    private var mShouldShowLastUpdate = false
    private val mLastUpdateTimeUpdater: LastUpdateTimeUpdater = LastUpdateTimeUpdater()
    private var mIsOverToRefresh = false

    init {
        initViews(attrs)
    }

    private fun initViews(attrs: AttributeSet?) {
        buildAnimation()
        val header = LayoutInflater.from(context).inflate(R.layout.ns_ptr_classic_default_header, this)
        mRotateView = header.findViewById(R.id.ptr_classic_header_rotate_view)
        mTitleTextView = header.findViewById<View>(R.id.ptr_classic_header_rotate_view_header_title) as TextView
        mLastUpdateTextView = header.findViewById<View>(R.id.ptr_classic_header_rotate_view_header_last_update) as TextView
        mProgressBar = header.findViewById(R.id.ptr_classic_header_rotate_view_progressbar)
        resetView()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mLastUpdateTimeUpdater.stop()
    }

    fun setRotateAniTime(time: Int) {
        if (time == mRotateAniTime || time == 0) {
            return
        }
        mRotateAniTime = time
        buildAnimation()
    }

    /**
     * Specify the last update time by this key string
     *
     * @param key
     */
    fun setLastUpdateTimeKey(key: String?) {
        if (TextUtils.isEmpty(key)) {
            return
        }
        mLastUpdateTimeKey = key
    }

    /**
     * Using an object to specify the last update time.
     *
     * @param object
     */
    fun setLastUpdateTimeRelateObject(`object`: Any) {
        setLastUpdateTimeKey(`object`.javaClass.name)
    }

    private fun buildAnimation() {
        mFlipAnimation = RotateAnimation(0f, (-180).toFloat(), RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f)
        mFlipAnimation!!.interpolator = LinearInterpolator()
        mFlipAnimation!!.duration = mRotateAniTime.toLong()
        mFlipAnimation!!.fillAfter = true
        mReverseFlipAnimation = RotateAnimation((-180).toFloat(), 0f, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f)
        mReverseFlipAnimation!!.interpolator = LinearInterpolator()
        mReverseFlipAnimation!!.duration = mRotateAniTime.toLong()
        mReverseFlipAnimation!!.fillAfter = true
    }

    private fun resetView() {
        hideRotateView()
        mProgressBar!!.visibility = INVISIBLE
    }

    private fun hideRotateView() {
        mRotateView!!.clearAnimation()
        mRotateView!!.visibility = INVISIBLE
    }

    override fun onDrag(frame: NSPtrLayout) {
        mShouldShowLastUpdate = true
        tryUpdateLastUpdateTime()
        mLastUpdateTimeUpdater!!.start()
        mProgressBar!!.visibility = INVISIBLE
        mRotateView!!.visibility = VISIBLE
        mTitleTextView!!.visibility = VISIBLE
        mTitleTextView!!.text = resources.getString(R.string.cube_ptr_pull_down_to_refresh)
    }

    override fun onRefreshing(frame: NSPtrLayout) {
        mShouldShowLastUpdate = false
        hideRotateView()
        mProgressBar!!.visibility = VISIBLE
        mTitleTextView!!.visibility = VISIBLE
        mTitleTextView!!.setText(R.string.cube_ptr_refreshing)
        tryUpdateLastUpdateTime()
        mLastUpdateTimeUpdater!!.stop()
    }

    override fun onComplete(frame: NSPtrLayout) {
        hideRotateView()
        mProgressBar!!.visibility = INVISIBLE
        mTitleTextView!!.visibility = VISIBLE
        mTitleTextView!!.text = resources.getString(R.string.cube_ptr_refresh_complete)

        // update last update time
        val sharedPreferences = context.getSharedPreferences(KEY_SharedPreferences, 0)
        if (!TextUtils.isEmpty(mLastUpdateTimeKey)) {
            mLastUpdateTime = Date().time
            sharedPreferences.edit().putLong(mLastUpdateTimeKey, mLastUpdateTime).commit()
        }
    }

    private fun tryUpdateLastUpdateTime() {
        if (TextUtils.isEmpty(mLastUpdateTimeKey) || !mShouldShowLastUpdate) {
            mLastUpdateTextView!!.visibility = GONE
        } else {
            val time = lastUpdateTime
            if (TextUtils.isEmpty(time)) {
                mLastUpdateTextView!!.visibility = GONE
            } else {
                mLastUpdateTextView!!.visibility = VISIBLE
                mLastUpdateTextView!!.text = time
            }
        }
    }

    private val lastUpdateTime: String?
        private get() {
            if (mLastUpdateTime == -1L && !TextUtils.isEmpty(mLastUpdateTimeKey)) {
                mLastUpdateTime = context.getSharedPreferences(KEY_SharedPreferences, 0).getLong(mLastUpdateTimeKey, -1)
            }
            if (mLastUpdateTime == -1L) {
                return null
            }
            val diffTime = Date().time - mLastUpdateTime
            val seconds = (diffTime / 1000).toInt()
            if (diffTime < 0) {
                return null
            }
            if (seconds <= 0) {
                return null
            }
            val sb = StringBuilder()
            sb.append(context.getString(R.string.cube_ptr_last_update))
            if (seconds < 60) {
                sb.append(seconds.toString() + context.getString(R.string.cube_ptr_seconds_ago))
            } else {
                val minutes = seconds / 60
                if (minutes > 60) {
                    val hours = minutes / 60
                    if (hours > 24) {
                        val date = Date(mLastUpdateTime)
                        sb.append(sDataFormat.format(date))
                    } else {
                        sb.append(hours.toString() + context.getString(R.string.cube_ptr_hours_ago))
                    }
                } else {
                    sb.append(minutes.toString() + context.getString(R.string.cube_ptr_minutes_ago))
                }
            }
            return sb.toString()
        }

    override fun onPositionChange(frame: NSPtrLayout) {
        if (frame.currentState == NSPtrLayout.State.DRAG) {
            if (mIsOverToRefresh != frame.isOverToRefreshPosition) {
                if (frame.isOverToRefreshPosition) {
                    crossRotateLineFromTopUnderTouch(frame)
                    mRotateView?.let {
                        it.clearAnimation()
                        it.startAnimation(mFlipAnimation)
                    }
                } else {
                    crossRotateLineFromBottomUnderTouch(frame)
                    mRotateView?.let {
                        it.clearAnimation()
                        it.startAnimation(mReverseFlipAnimation)
                    }
                }
                mIsOverToRefresh = frame.isOverToRefreshPosition
            }
        }
    }

    private fun crossRotateLineFromTopUnderTouch(frame: NSPtrLayout?) {
        mTitleTextView?.visibility = VISIBLE
        mTitleTextView?.setText(R.string.cube_ptr_release_to_refresh)
    }

    private fun crossRotateLineFromBottomUnderTouch(frame: NSPtrLayout) {
        mTitleTextView?.visibility = VISIBLE
        mTitleTextView?.text = resources.getString(R.string.cube_ptr_pull_down_to_refresh)
    }

    private inner class LastUpdateTimeUpdater : Runnable {
        private var mRunning = false

        fun start() {
            if (TextUtils.isEmpty(mLastUpdateTimeKey)) {
                return
            }
            mRunning = true
            run()
        }

        fun stop() {
            mRunning = false
            removeCallbacks(this)
        }

        override fun run() {
            tryUpdateLastUpdateTime()
            if (mRunning) {
                postDelayed(this, 1000)
            }
        }
    }

    companion object {
        private const val KEY_SharedPreferences = "cube_ptr_classic_last_update"
        @SuppressLint("SimpleDateFormat")
        private val sDataFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
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
        offsetTopAndBottom(offset)
    }
}