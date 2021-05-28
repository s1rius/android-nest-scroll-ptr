package wtf.s1.android.ptr

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.PointF
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.AbsListView
import android.widget.ListView
import android.widget.TextView
import androidx.core.view.*
import androidx.core.widget.ListViewCompat
import wtf.s1.android.ptr.PtrListenerHolder.Companion.create

/**
 * This layout view for "Pull to Refresh(Ptr)" support all of the view,
 * you can contain everything you want.
 * support: pull to refresh / release to refresh / auto refresh / keep header view
 * while refreshing / hide header view while refreshing
 * It defines [PtrListener], which allows you customize the UI easily.
 */
open class PtrLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ViewGroup(context, attrs, defStyle), NestedScrollingParent, NestedScrollingChild {

    companion object {
        // status enum
        const val PTR_STATUS_INIT = 1
        private const val INVALID_POINTER = -1
        const val PTR_STATUS_PREPARE = 2
        const val PTR_STATUS_LOADING = 3
        const val PTR_STATUS_COMPLETE = 4
        var DEBUG = true
        private var ID = 1
    }

    private val mPullFriction = 0.56f
    var mStatus = PTR_STATUS_INIT
    private val LOG_TAG = "ptr-frame-" + ++ID
    var contentView: View? = null

    // optional config for define header and content in xml file
    private var mHeaderId = 0
    private var mContainerId = 0

    // ptr config
    private var mLoadingMinTime = 500
    private var mLoadingStartTime: Long = 0
    var isAutoRefresh = false
    private var mDurationToLoadingPosition = 200
    private var mDurationToCloseHeader = 200
    var isKeepHeaderWhenRefresh = true
    var isPullToRefresh = false
    private var mHeaderView: View? = null
    private val mPtrListenerHolder = create()

    // working parameters
    private val mScrollChecker: ScrollChecker
    var headerHeight = 0
    private var mPtrStateController: PtrStateController?

    //touch handle
    private var mActivePointerId = INVALID_POINTER
    private var mIsBeingDragged = false
    private val mInitialTouch = PointF((-1).toFloat(), (-1).toFloat())
    private val mLastTouch = PointF((-1).toFloat(), (-1).toFloat())
    private val mTouchSlop: Int
    private var mIsInTouchProgress = false

    //NestScroll
    private val mNestedScrollingParentHelper = NestedScrollingParentHelper(this)
    private val mNestedScrollingChildHelper = NestedScrollingChildHelper(this)
    private val mScrollOffset = IntArray(2)
    private val mScrollConsumed = IntArray(2)
    private val mParentOffsetInWindow = IntArray(2)
    private val mParentScrollConsumed = IntArray(2)
    private var mTotalUnconsumed = 0
    private var mInVerticalNestedScrolling = false
    private val mPerformRefreshCompleteDelay: Runnable = Runnable { performRefreshComplete() }


    init {
        mPtrStateController = PtrStateController()
        val arr = context.obtainStyledAttributes(attrs, R.styleable.PtrLayout, 0, 0)
        if (arr != null) {
            mHeaderId = arr.getResourceId(R.styleable.PtrLayout_ptr_header, mHeaderId)
            mContainerId = arr.getResourceId(R.styleable.PtrLayout_ptr_content, mContainerId)
            mPtrStateController!!.resistance =
                arr.getFloat(R.styleable.PtrLayout_ptr_resistance, mPtrStateController!!.resistance)
            mDurationToLoadingPosition = arr.getInt(
                R.styleable.PtrLayout_ptr_duration_to_loading_position,
                mDurationToLoadingPosition
            )
            mDurationToCloseHeader = arr.getInt(
                R.styleable.PtrLayout_ptr_duration_to_close_header,
                mDurationToCloseHeader
            )
            var ratio = mPtrStateController!!.ratioOfHeaderToHeightRefresh
            ratio = arr.getFloat(R.styleable.PtrLayout_ptr_ratio_of_header_height_to_refresh, ratio)
            mPtrStateController!!.setRatioOfHeaderHeightToRefresh(ratio)
            isPullToRefresh =
                arr.getBoolean(R.styleable.PtrLayout_ptr_pull_to_fresh, isPullToRefresh)
            arr.recycle()
        }
        mScrollChecker = ScrollChecker()
        val conf = ViewConfiguration.get(getContext())
        mTouchSlop = conf.scaledTouchSlop
        isNestedScrollingEnabled = true
    }

    override fun onFinishInflate() {
        val childCount = childCount// both are not specified
        // not specify header or content
        check(childCount <= 2) { "PtrFrameLayout can only contains 2 children" }
        if (childCount == 2) {
            if (mHeaderId != 0 && mHeaderView == null) {
                mHeaderView = findViewById(mHeaderId)
            }
            if (mContainerId != 0 && contentView == null) {
                contentView = findViewById(mContainerId)
            }

            // not specify header or content
            if (contentView == null || mHeaderView == null) {
                val child1 = getChildAt(0)
                val child2 = getChildAt(1)
                if (child1 is PtrListener) {
                    mHeaderView = child1
                    contentView = child2
                } else if (child2 is PtrListener) {
                    mHeaderView = child2
                    contentView = child1
                } else {
                    // both are not specified
                    if (contentView == null && mHeaderView == null) {
                        mHeaderView = child1
                        contentView = child2
                    } else {
                        if (mHeaderView == null) {
                            mHeaderView = if (contentView === child1) child2 else child1
                        } else {
                            contentView = if (mHeaderView === child1) child2 else child1
                        }
                    }
                }
            }
        } else if (childCount == 1) {
            contentView = getChildAt(0)
        } else {
            val errorView = TextView(context)
            errorView.isClickable = true
            errorView.setTextColor(-0x9a00)
            errorView.gravity = Gravity.CENTER
            errorView.textSize = 20f
            errorView.text =
                "The content view in PtrFrameLayout is empty. Do you forget to specify its id in xml layout file?"
            contentView = errorView
            addView(contentView)
        }
        if (mHeaderView != null) {
            mHeaderView!!.bringToFront()
        }
        super.onFinishInflate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mPerformRefreshCompleteDelay?.let { removeCallbacks(it) }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (mHeaderView != null) {
            measureChildWithMargins(mHeaderView, widthMeasureSpec, 0, heightMeasureSpec, 0)
            headerHeight = mHeaderView!!.measuredHeight
            mPtrStateController!!.headerHeight = headerHeight
        }
        if (contentView != null) {
            measureContentView(contentView!!, widthMeasureSpec, heightMeasureSpec)
        }
    }

    private fun measureContentView(
        child: View,
        parentWidthMeasureSpec: Int,
        parentHeightMeasureSpec: Int
    ) {
        val lp = child.layoutParams as MarginLayoutParams
        val childWidthMeasureSpec = getChildMeasureSpec(
            parentWidthMeasureSpec,
            paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin, lp.width
        )
        val childHeightMeasureSpec = getChildMeasureSpec(
            parentHeightMeasureSpec,
            paddingTop + paddingBottom + lp.topMargin, lp.height
        )
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
    }

    override fun onLayout(flag: Boolean, i: Int, j: Int, k: Int, l: Int) {
        layoutChildren()
    }

    private fun layoutChildren() {
        val offset = mPtrStateController!!.currentPosY
        val paddingLeft = paddingLeft
        val paddingTop = paddingTop
        if (mHeaderView != null) {
            val lp = mHeaderView!!.layoutParams as LayoutParams
            val left = paddingLeft + lp.leftMargin
            // enhance readability(header is layout above screen when first init)
            val top = -(headerHeight - paddingTop - lp.topMargin - offset)
            val right = left + mHeaderView!!.measuredWidth
            val bottom = top + mHeaderView!!.measuredHeight
            mHeaderView!!.layout(left, top, right, bottom)
        }
        if (contentView != null) {
            val lp = contentView!!.layoutParams as MarginLayoutParams
            val left = paddingLeft + lp.leftMargin
            val top = paddingTop + lp.topMargin + offset
            val right = left + contentView!!.measuredWidth
            val bottom = top + contentView!!.measuredHeight
            contentView!!.layout(left, top, right, bottom)
        }
    }

    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        if (mScrollChecker.isRunning) {
            mScrollChecker.abortIfWorking()
        }
        val actionMasked = e.actionMasked
        when (actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mIsInTouchProgress = false
                mIsBeingDragged = false
                stopNestedScroll()
                if (mPtrStateController!!.hasLeftStartPosition()) {
                    onRelease(false)
                }
            }
            MotionEvent.ACTION_DOWN -> mIsInTouchProgress = true
            else -> {
            }
        }
        return super.dispatchTouchEvent(e)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isEnabled || contentView == null || mHeaderView == null || mInVerticalNestedScrolling) {
            return false
        }
        val pointerIndex: Int
        val action = ev.actionMasked
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mIsBeingDragged = false
                mActivePointerId = ev.getPointerId(0)
                pointerIndex = ev.findPointerIndex(mActivePointerId)
                if (pointerIndex < 0) {
                    return false
                }
                mInitialTouch[ev.getX(pointerIndex)] = ev.getY(pointerIndex)
                mLastTouch[ev.getX(pointerIndex)] = ev.getY(pointerIndex)
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
            }
            MotionEvent.ACTION_MOVE -> {
                if (mActivePointerId == INVALID_POINTER) {
                    return false
                }
                pointerIndex = ev.findPointerIndex(mActivePointerId)
                if (pointerIndex < 0) {
                    return false
                }
                if (contentView !is NestedScrollingChild) {
                    // if content is a scrollable view and not implement NestedScrollingChild
                    // like ListView, we need enable nest scroll in intercept process
                    val y = (ev.getY(pointerIndex) + 0.5f).toInt()
                    val dy = (mLastTouch.y - y).toInt()
                    if (mPtrStateController!!.isInStartPosition || dy > 0) {
                        if (dispatchNestedPreScroll(0, dy, mScrollConsumed, mScrollOffset)) {
                            mLastTouch.y = (y - mScrollOffset[1]).toFloat()
                            // handle touch when parent not accept nest scroll
                            startDragging(PointF(ev.x, ev.y))
                            return mIsBeingDragged
                        }
                    }
                }
                startDragging(PointF(ev.x, ev.y))
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> mIsBeingDragged = false
        }
        return mIsBeingDragged
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled // || canChildScrollToUp()
            || mInVerticalNestedScrolling
        ) {
            return false
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> return true
            MotionEvent.ACTION_DOWN -> {
                val pointerIndex = event.findPointerIndex(mActivePointerId)
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.")
                    return false
                }
                mInitialTouch[event.getX(pointerIndex)] = event.getY(pointerIndex)
                mLastTouch[event.getX(pointerIndex)] = event.getY(pointerIndex)
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
                return !canChildScrollToUp()
            }
            MotionEvent.ACTION_MOVE -> {
                val  pointerIndex = event.findPointerIndex(mActivePointerId)
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.")
                    return false
                }
                val y = (event.getY(pointerIndex) + 0.5f).toInt()
                var dy = (y - mLastTouch.y).toInt()
                if (dy > 0 || mPtrStateController!!.isInStartPosition) {
                    if (dispatchNestedPreScroll(0, -dy, mScrollConsumed, mScrollOffset)) {
                        mLastTouch.y = (y - mScrollOffset[1]).toFloat()
                        // handle touch when parent not accept nest scroll
                        return mScrollConsumed[1] == 0
                    }
                }
                mLastTouch.y = y.toFloat()
                if (dy == 0) {
                    return true
                }
                if (dy > 0) {
                    dy = withFriction(dy.toFloat())
                }
                val moveDown = dy > 0
                val moveUp = !moveDown
                val canMoveUp = mPtrStateController!!.hasLeftStartPosition()
                if (mPtrStateController!!.currentPosY != 0) {
                    movePos(dy.toFloat())
                    return true
                }
                if (moveUp && canMoveUp || moveDown) {
                    movePos(dy.toFloat())
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * //if deltaY > 0, move the content down
     *
     * @param deltaY the y offset
     */
    private fun movePos(deltaY: Float): Int {
        // has reached the top
        if (deltaY < 0 && mPtrStateController!!.isInStartPosition) {
            return 0
        }
        var to = mPtrStateController!!.currentPosY + deltaY.toInt()

        // over top
        if (mPtrStateController!!.willOverTop(to)) {
            to = PtrStateController.POS_START
        }
        mPtrStateController!!.setCurrentPos(to)
        if (mPtrStateController!!.isInStartPosition && isRealRefreshing) {
            performRefreshComplete()
        }
        val change = to - mPtrStateController!!.lastPosY
        updatePos(change)
        return change
    }

    private fun updatePos(change: Int) {
        if (change == 0) {
            return
        }


        // leave initiated position or just refresh complete
        if (mPtrStateController!!.hasJustLeftStartPosition() && mStatus == PTR_STATUS_INIT) {
            changeStatusTo(PTR_STATUS_PREPARE)
        }

        // back to initiated position
        if (mPtrStateController!!.hasJustBackToStartPosition()) {
            tryToNotifyReset()
        }

        // Pull to Refresh
        if (mStatus == PTR_STATUS_PREPARE) {
            // reach fresh height while moving from top to bottom
            if (!isAutoRefresh && isPullToRefresh
                && mPtrStateController!!.crossRefreshLineFromTopToBottom()
            ) {
                tryToPerformRefresh()
            }
        }
        mHeaderView!!.offsetTopAndBottom(change)
        contentView!!.offsetTopAndBottom(change)
        invalidate()
        if (mPtrListenerHolder.hasHandler()) {
            mPtrListenerHolder.onPositionChange(this, mStatus, mPtrStateController)
        }
    }

    private fun withFriction(force: Float): Int {
        return (force * mPullFriction).toInt()
    }

    fun changeStatusTo(status: Int) {
        mStatus = status
        onStateChange(mStatus)
    }

    fun onStateChange(status: Int) {
        when (status) {
            PTR_STATUS_INIT -> {
                clearFlag()
                mPtrListenerHolder.onReset(this)
            }
            PTR_STATUS_COMPLETE -> {
                // if is auto refresh do nothing, wait scroller stop
                if (mScrollChecker.isRunning && isAutoRefresh) {
                    return
                }
                notifyUIRefreshComplete(false)
            }
            PTR_STATUS_PREPARE -> mPtrListenerHolder.onPrepare(this)
            PTR_STATUS_LOADING -> performRefresh()
        }
    }

    private fun onRelease(stayForLoading: Boolean) {
        tryToPerformRefresh()
        if (mStatus == PTR_STATUS_LOADING) {
            // keep header for fresh
            if (isKeepHeaderWhenRefresh) {
                // scroll header back
                if (mPtrStateController!!.isOverOffsetToKeepHeaderWhileLoading && !stayForLoading) {
                    mScrollChecker.tryToScrollTo(
                        mPtrStateController!!.offsetToKeepHeaderWhileLoading,
                        mDurationToLoadingPosition
                    )
                }
            } else {
                tryScrollBackToTopWhileLoading()
            }
        } else {
            if (mStatus == PTR_STATUS_COMPLETE) {
                notifyUIRefreshComplete(false)
            } else {
                tryScrollBackToTopAbortRefresh()
            }
        }
    }

    /**
     * Scroll back to to if is not under touch
     */
    private fun tryScrollBackToTop() {
        if (!mIsInTouchProgress) {
            mScrollChecker.tryToScrollTo(PtrStateController.POS_START, mDurationToCloseHeader)
        }
    }

    /**
     * just make easier to understand
     */
    private fun tryScrollBackToTopWhileLoading() {
        tryScrollBackToTop()
    }

    /**
     * just make easier to understand
     */
    private fun tryScrollBackToTopAfterComplete() {
        tryScrollBackToTop()
    }

    /**
     * just make easier to understand
     */
    private fun tryScrollBackToTopAbortRefresh() {
        tryScrollBackToTop()
    }

    private fun tryToPerformRefresh() {
        if (mStatus != PTR_STATUS_PREPARE) {
            return
        }
        if (mPtrStateController!!.isOverOffsetToKeepHeaderWhileLoading && isAutoRefresh
            || mPtrStateController!!.isOverOffsetToRefresh
        ) {
            changeStatusTo(PTR_STATUS_LOADING)
        }
    }

    private fun performRefresh() {
        mLoadingStartTime = System.currentTimeMillis()
        if (mPtrListenerHolder.hasHandler()) {
            mPtrListenerHolder.onBegin(this)
        }
    }

    /**
     * If at the top and not in loading, reset
     */
    private fun tryToNotifyReset() {
        if ((mStatus == PTR_STATUS_COMPLETE || mStatus == PTR_STATUS_PREPARE)
            && mPtrStateController!!.isInStartPosition
        ) {
            changeStatusTo(PTR_STATUS_INIT)
        }
    }

    protected fun onPtrScrollAbort() {
        if (mPtrStateController!!.hasLeftStartPosition() && isAutoRefresh) {
            onRelease(true)
        }
    }

    protected fun onPtrScrollFinish() {
        if (mPtrStateController!!.hasLeftStartPosition() && isAutoRefresh) {
            onRelease(true)
        }
    }

    /**
     * Detect whether is refreshing.
     *
     * @return isRefreshing
     */
    val isRealRefreshing: Boolean
        get() = mStatus == PTR_STATUS_LOADING
    var isRefreshing: Boolean
        get() = isRealRefreshing
        set(isRefresh) {
            if (isRefresh) {
                autoRefresh()
            } else {
                refreshComplete()
            }
        }

    /**
     * Call this when data is loaded.
     * The UI will perform complete at once or after a delay, depends on the time elapsed is greater then [.mLoadingMinTime] or not.
     */
    fun refreshComplete() {
        val delay = (mLoadingMinTime - (System.currentTimeMillis() - mLoadingStartTime)).toInt()
        if (delay <= 0) {
            performRefreshComplete()
        } else {
            postDelayed(mPerformRefreshCompleteDelay, delay.toLong())
        }
    }

    /**
     * Do refresh complete work when time elapsed is greater than [.mLoadingMinTime]
     */
    private fun performRefreshComplete() {
        changeStatusTo(PTR_STATUS_COMPLETE)
    }

    /**
     * Do real refresh work. If there is a hook, execute the hook first.
     *
     * @param ignoreHook is ignore hook
     */
    private fun notifyUIRefreshComplete(ignoreHook: Boolean) {
        if (mPtrListenerHolder.hasHandler()) {
            mPtrListenerHolder.onComplete(this)
        }
        mPtrStateController!!.onUIRefreshComplete()
        tryScrollBackToTopAfterComplete()
        tryToNotifyReset()
    }

    private fun clearFlag() {
        isAutoRefresh = false
    }

    @JvmOverloads
    fun autoRefresh(duration: Int = mDurationToCloseHeader) {
        if (mStatus != PTR_STATUS_INIT) {
            return
        }
        isAutoRefresh = true
        changeStatusTo(PTR_STATUS_PREPARE)
        mScrollChecker.tryToScrollTo(mPtrStateController!!.offsetToRefresh, duration)
        changeStatusTo(PTR_STATUS_LOADING)
    }

    /**
     * loading will last at least for so long
     *
     * @param time the loading min duration
     */
    fun setLoadingMinTime(time: Int) {
        mLoadingMinTime = time
    }

    fun addPtrListener(ptrListener: PtrListener?) {
        mPtrListenerHolder.addListener(ptrListener!!)
    }

    fun removePtrListener(ptrListener: PtrListener?) {
        mPtrListenerHolder.removeListener(ptrListener)
    }

    fun setPtrIndicator(indicator: PtrStateController) {
        if (mPtrStateController != null && mPtrStateController != indicator) {
            indicator.convertFrom(mPtrStateController!!)
        }
        mPtrStateController = indicator
    }

    var resistance: Float
        get() = mPtrStateController!!.resistance
        set(resistance) {
            mPtrStateController!!.resistance = resistance
        }

    /**
     * The duration to return back to the loading position
     *
     * @param duration to loading position duration
     */
    fun setDurationToLoadingPosition(duration: Int) {
        mDurationToLoadingPosition = duration
    }

    val durationToCloseHeader: Long
        get() = mDurationToCloseHeader.toLong()

    /**
     * The duration to close time
     *
     * @param duration to close duration
     */
    fun setDurationToCloseHeader(duration: Int) {
        mDurationToCloseHeader = duration
    }

    fun setRatioOfHeaderHeightToRefresh(ratio: Float) {
        mPtrStateController!!.setRatioOfHeaderHeightToRefresh(ratio)
    }

    var offsetToRefresh: Int
        get() = mPtrStateController!!.offsetToRefresh
        set(offset) {
            mPtrStateController!!.offsetToRefresh = offset
        }
    val ratioOfHeaderToHeightRefresh: Float
        get() = mPtrStateController!!.ratioOfHeaderToHeightRefresh
    var offsetToKeepHeaderWhileLoading: Int
        get() = mPtrStateController!!.offsetToKeepHeaderWhileLoading
        set(offset) {
            mPtrStateController!!.offsetToKeepHeaderWhileLoading = offset
        }
    var headerView: View?
        get() = mHeaderView
        set(header) {
            if (header == null) {
                return
            }
            if (mHeaderView != null && mHeaderView !== header) {
                removeView(mHeaderView)
            }
            var lp = header.layoutParams
            if (lp == null) {
                lp = LayoutParams(-1, -2)
                header.layoutParams = lp
            }
            mHeaderView = header
            addView(header)
        }

    // <editor-fold defaultstate="collapsed" desc="generate layout params">
    class LayoutParams : MarginLayoutParams {
        constructor(c: Context, attrs: AttributeSet) : super(c, attrs) {}
        constructor(width: Int, height: Int) : super(width, height) {}
        constructor(source: MarginLayoutParams?) : super(source) {}
        constructor(source: ViewGroup.LayoutParams) : super(source) {}
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        return p is LayoutParams
    }

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams): ViewGroup.LayoutParams {
        return LayoutParams(p)
    }

    override fun generateLayoutParams(attrs: AttributeSet): ViewGroup.LayoutParams {
        return LayoutParams(context, attrs)
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="nested scroll child">
    override fun setNestedScrollingEnabled(enabled: Boolean) {
        mNestedScrollingChildHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return mNestedScrollingChildHelper.startNestedScroll(axes)
    }

    override fun stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll()
    }

    override fun hasNestedScrollingParent(): Boolean {
        return mNestedScrollingChildHelper.hasNestedScrollingParent()
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
        dyUnconsumed: Int, offsetInWindow: IntArray?
    ): Boolean {
        return mNestedScrollingChildHelper.dispatchNestedScroll(
            dxConsumed, dyConsumed,
            dxUnconsumed, dyUnconsumed, offsetInWindow
        )
    }

    override fun dispatchNestedPreScroll(
        dx: Int, dy: Int, consumed: IntArray?,
        offsetInWindow: IntArray?
    ): Boolean {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(
            dx, dy, consumed, offsetInWindow
        )
    }

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun onNestedFling(
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        // Re-dispatch up the tree by default
        return dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        // Re-dispatch up the tree by default
        return dispatchNestedPreFling(velocityX, velocityY)
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="nested scroll parent">
    override fun onStartNestedScroll(child: View, target: View, axes: Int): Boolean {
        return (isEnabled
                && !isAutoRefresh // && !isRefreshing()
                && axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0)
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes)
        // Dispatch up to the nested parent
        startNestedScroll(axes and ViewCompat.SCROLL_AXIS_VERTICAL)
        mTotalUnconsumed = 0
        mInVerticalNestedScrolling = true
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        // If we are in the middle of consuming, a scroll, then we want to move the ptr back up
        // before allowing the list to scroll
        if (dy > 0 && !mPtrStateController!!.isInStartPosition) {
            consumed[1] = -movePos(-dy.toFloat())
        }
        if (dy > 0 && mTotalUnconsumed > 0 && isRealRefreshing) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = dy - mTotalUnconsumed
                mTotalUnconsumed = 0
            } else {
                mTotalUnconsumed -= dy
                consumed[1] = dy
            }
            movePos(-dy.toFloat())
        }

        // Now let our nested parent consume the leftovers
        val parentConsumed = mParentScrollConsumed
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0]
            consumed[1] += parentConsumed[1]
        }
        if (consumed[1] < 0) {
            consumed[1] = 0
        }
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int
    ) {
        // Dispatch up to the nested parent first
        var dyUnconsumed = dyUnconsumed
        if (mStatus != PTR_STATUS_INIT) {
            dyUnconsumed = withFriction(dyUnconsumed.toFloat())
        }
        dispatchNestedScroll(
            dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
            mParentOffsetInWindow
        )

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        val dy = dyUnconsumed + mParentOffsetInWindow[1]
        if (dy < 0 && !canChildScrollToUp()) {
            mTotalUnconsumed += Math.abs(dy)
        }
        movePos(-dy.toFloat())
    }

    override fun onStopNestedScroll(child: View) {
        mNestedScrollingParentHelper.onStopNestedScroll(child)
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (mTotalUnconsumed > 0) {
            //finishSpinner(mTotalUnconsumed);
            mTotalUnconsumed = 0
        }
        // Dispatch up our nested parent
        stopNestedScroll()
        mInVerticalNestedScrolling = false
    }

    override fun getNestedScrollAxes(): Int {
        return mNestedScrollingParentHelper.nestedScrollAxes
    }

    // </editor-fold>
    override fun requestDisallowInterceptTouchEvent(b: Boolean) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if (Build.VERSION.SDK_INT < 21 && contentView is AbsListView
            || contentView != null && !ViewCompat.isNestedScrollingEnabled(
                contentView!!
            )
        ) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(b)
        }
    }

    fun canChildScrollToUp(): Boolean {
        val canChildScrollUp: Boolean
        canChildScrollUp = if (contentView is ListView) {
            ListViewCompat.canScrollList(
                (contentView as ListView?)!!,
                -1
            )
        } else {
            contentView!!.canScrollVertically(-1)
        }
        return canChildScrollUp
    }

    private fun startDragging(move: PointF) {
        val yDiff = move.y - mInitialTouch.y
        val xDiff = Math.abs(move.x - mInitialTouch.x)
        if (yDiff > mTouchSlop && yDiff > xDiff && !mIsBeingDragged) {
            if (!canChildScrollToUp()) {
                mIsBeingDragged = true
            }
            // if in refreshing, scroll to up will make PtrFrameLayout handle the touch event
            // and offset content view.
        } else if (yDiff < 0 && Math.abs(yDiff) > mTouchSlop && isRealRefreshing) {
            mIsBeingDragged = true
        }
    }

    internal inner class ScrollChecker : AnimatorUpdateListener {
        private val mAnimator = ValueAnimator.ofFloat(0f, 1f)
        var isRunning = false
        private var mStart = 0
        private var mTo = 0
        private fun reset() {
            isRunning = false
            mAnimator.cancel()
        }

        fun abortIfWorking() {
            if (isRunning) {
                onPtrScrollAbort()
                reset()
            }
        }

        fun tryToScrollTo(to: Int, duration: Int) {
            if (mPtrStateController!!.isAlreadyHere(to) || to == mTo && isRunning) {
                return
            }
            mStart = mPtrStateController!!.currentPosY
            mTo = to
            if (mAnimator.isRunning) {
                mAnimator.cancel()
            }
            mAnimator.duration = duration.toLong()
            mAnimator.start()
            isRunning = true
        }

        override fun onAnimationUpdate(animation: ValueAnimator) {
            val fraction = animation.animatedFraction
            val target = mStart + ((mTo - mStart) * fraction).toInt()
            movePos((target - mPtrStateController!!.currentPosY).toFloat())
        }

        init {
            mAnimator.addUpdateListener(this)
            mAnimator.interpolator = AccelerateDecelerateInterpolator()
        }
    }
}