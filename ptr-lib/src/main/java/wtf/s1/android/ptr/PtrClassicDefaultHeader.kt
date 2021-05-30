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

open class PtrClassicDefaultHeader @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), PtrListener {
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

    init {
        initViews(attrs)
    }

    private fun initViews(attrs: AttributeSet?) {
        val arr = context.obtainStyledAttributes(attrs, R.styleable.PtrClassicHeader, 0, 0)
        if (arr != null) {
            mRotateAniTime = arr.getInt(R.styleable.PtrClassicHeader_ptr_rotate_ani_time, mRotateAniTime)
            arr.recycle()
        }
        buildAnimation()
        val header = LayoutInflater.from(context).inflate(R.layout.cube_ptr_classic_default_header, this)
        mRotateView = header.findViewById(R.id.ptr_classic_header_rotate_view)
        mTitleTextView = header.findViewById<View>(R.id.ptr_classic_header_rotate_view_header_title) as TextView
        mLastUpdateTextView = header.findViewById<View>(R.id.ptr_classic_header_rotate_view_header_last_update) as TextView
        mProgressBar = header.findViewById(R.id.ptr_classic_header_rotate_view_progressbar)
        resetView()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mLastUpdateTimeUpdater?.stop()
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

    override fun onReset(frame: PtrLayout?) {
        resetView()
        mShouldShowLastUpdate = true
        tryUpdateLastUpdateTime()
    }

    override fun onPrepare(frame: PtrLayout?) {
        mShouldShowLastUpdate = true
        tryUpdateLastUpdateTime()
        mLastUpdateTimeUpdater!!.start()
        mProgressBar!!.visibility = INVISIBLE
        mRotateView!!.visibility = VISIBLE
        mTitleTextView!!.visibility = VISIBLE
        if (frame!!.isPullToRefresh) {
            mTitleTextView!!.text = resources.getString(R.string.cube_ptr_pull_down_to_refresh)
        } else {
            mTitleTextView!!.text = resources.getString(R.string.cube_ptr_pull_down)
        }
    }

    override fun onBegin(frame: PtrLayout?) {
        mShouldShowLastUpdate = false
        hideRotateView()
        mProgressBar!!.visibility = VISIBLE
        mTitleTextView!!.visibility = VISIBLE
        mTitleTextView!!.setText(R.string.cube_ptr_refreshing)
        tryUpdateLastUpdateTime()
        mLastUpdateTimeUpdater!!.stop()
    }

    override fun onComplete(frame: PtrLayout?) {
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

    override fun onPositionChange(frame: PtrLayout?, ptrStateController: PtrStateController?) {
        val mOffsetToRefresh = frame!!.offsetToRefresh
        val currentPos = ptrStateController!!.currentPosY
        val lastPos = ptrStateController.lastPosY
        if (mOffsetToRefresh in (currentPos + 1)..lastPos) {
            if (frame.currentState == PtrLayout.State.DRAG) {
                crossRotateLineFromBottomUnderTouch(frame)
                if (mRotateView != null) {
                    mRotateView!!.clearAnimation()
                    mRotateView!!.startAnimation(mReverseFlipAnimation)
                }
            }
        } else if (mOffsetToRefresh in lastPos until currentPos) {
            if (frame.currentState == PtrLayout.State.DRAG) {
                crossRotateLineFromTopUnderTouch(frame)
                if (mRotateView != null) {
                    mRotateView!!.clearAnimation()
                    mRotateView!!.startAnimation(mFlipAnimation)
                }
            }
        }
    }

    private fun crossRotateLineFromTopUnderTouch(frame: PtrLayout?) {
        if (!frame!!.isPullToRefresh) {
            mTitleTextView!!.visibility = VISIBLE
            mTitleTextView!!.setText(R.string.cube_ptr_release_to_refresh)
        }
    }

    private fun crossRotateLineFromBottomUnderTouch(frame: PtrLayout?) {
        mTitleTextView!!.visibility = VISIBLE
        if (frame!!.isPullToRefresh) {
            mTitleTextView!!.text = resources.getString(R.string.cube_ptr_pull_down_to_refresh)
        } else {
            mTitleTextView!!.text = resources.getString(R.string.cube_ptr_pull_down)
        }
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
}