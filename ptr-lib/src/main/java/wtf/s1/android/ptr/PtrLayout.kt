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
import wtf.s1.android.core.StateMachine
import wtf.s1.android.ptr.PtrListenerHolder.Companion.create
import kotlin.math.abs

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
        private const val INVALID_POINTER = -1
        private const val DEBUG = true
        private var ID = 1
    }

    sealed class State {
        // 初始化状态
        object IDLE : State()
        // 刷新状态
        object REFRESHING : State()
        // 拖动状态
        object DRAG : State()
    }

    sealed class Event {
        object OnPull : Event()
        object OnReleaseToIdle : Event()
        object OnReleaseToRefreshing : Event()
        object OnComplete : Event()
        object OnAutoRefresh : Event()
    }

    sealed class SideEffect {
        object OnRefreshing : SideEffect()
        object OnDragBegin : SideEffect()
        object OnComplete : SideEffect()
    }

    private val mPullFriction = 0.56f
    private val LOG_TAG = "ptr-frame-" + ++ID
    var contentView: View? = null

    // optional config for define header and content in xml file
    private var mHeaderId = 0
    private var mContainerId = 0

    // ptr config
    private var mLoadingMinTime = 500
    private var mLoadingStartTime: Long = 0
    private var mDurationToLoadingPosition = 200
    private var mDurationToCloseHeader = 200
    private var mHeaderView: View? = null
    private val mPtrListenerHolder = create()

    // working parameters
    private val mScrollChecker = ScrollChecker()
    var headerHeight = 0
    private var mPtrStateController = PtrStateController()
    private var stateMachine =
        StateMachine.create<State, Event, SideEffect> {
            initialState(State.IDLE)

            state<State.IDLE> {
                on<Event.OnPull> {
                    transitionTo(
                        State.DRAG,
                        SideEffect.OnDragBegin
                    )
                }
                on<Event.OnAutoRefresh> {
                    transitionTo(
                        State.REFRESHING,
                        SideEffect.OnRefreshing
                    )
                }

                onEnter {
                    Log.i(LOG_TAG, "state idle")
                    mPtrListenerHolder.onComplete(this@PtrLayout)
                }
            }

            state<State.REFRESHING> {
                on<Event.OnComplete> {
                    transitionTo(
                        State.IDLE,
                        SideEffect.OnComplete
                    )
                }

                onEnter {
                    mScrollChecker.scrollToRefreshing()
                    Log.i(LOG_TAG, "enter refreshing")
                    performRefresh()
                }
            }

            state<State.DRAG> {
                on<Event.OnReleaseToIdle> {
                    transitionTo(
                        State.IDLE,
                        SideEffect.OnComplete
                    )
                }
                on<Event.OnReleaseToRefreshing> {
                    transitionTo(
                        State.REFRESHING,
                        SideEffect.OnRefreshing
                    )
                }
                on<Event.OnComplete> {
                    transitionTo(
                        State.IDLE,
                        SideEffect.OnComplete
                    )
                }

                onEnter {
                    mPtrListenerHolder.onDrag(this@PtrLayout)
                    Log.i(LOG_TAG, "state drag")
                }
            }

            onTransition {
                val validTransition = it as? StateMachine.Transition.Valid ?: return@onTransition
                when (validTransition.sideEffect) {
                    SideEffect.OnDragBegin -> {
                        Log.i(LOG_TAG, "drag begin")
                    }
                    SideEffect.OnComplete -> {
                        tryScrollBackToTop()
                        notifyUIRefreshComplete()
                        Log.i(LOG_TAG, "complete")
                    }
                    SideEffect.OnRefreshing -> {
                        Log.i(LOG_TAG, "refresh")
                    }
                }
            }
        }

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
        val arr = context.obtainStyledAttributes(attrs, R.styleable.PtrLayout, 0, 0)
        if (arr != null) {
            mHeaderId = arr.getResourceId(R.styleable.PtrLayout_ptr_header, mHeaderId)
            mContainerId = arr.getResourceId(R.styleable.PtrLayout_ptr_content, mContainerId)
            mPtrStateController.resistance =
                arr.getFloat(R.styleable.PtrLayout_ptr_resistance, mPtrStateController.resistance)
            mDurationToLoadingPosition = arr.getInt(
                R.styleable.PtrLayout_ptr_duration_to_loading_position,
                mDurationToLoadingPosition
            )
            mDurationToCloseHeader = arr.getInt(
                R.styleable.PtrLayout_ptr_duration_to_close_header,
                mDurationToCloseHeader
            )
            var ratio = mPtrStateController.ratioOfHeaderToHeightRefresh
            ratio = arr.getFloat(R.styleable.PtrLayout_ptr_ratio_of_header_height_to_refresh, ratio)
            mPtrStateController.setRatioOfHeaderHeightToRefresh(ratio)
            arr.recycle()
        }
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
        mHeaderView?.bringToFront()
        super.onFinishInflate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(mPerformRefreshCompleteDelay)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (mHeaderView != null) {
            measureChildWithMargins(mHeaderView, widthMeasureSpec, 0, heightMeasureSpec, 0)
            headerHeight = mHeaderView?.measuredHeight?:0
            mPtrStateController.headerHeight = headerHeight
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
        val offset = mPtrStateController!!.currentPos
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
        when (e.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mIsInTouchProgress = false
                mIsBeingDragged = false
                stopNestedScroll()
                onRelease()
            }
            MotionEvent.ACTION_DOWN -> mIsInTouchProgress = true
        }
        return super.dispatchTouchEvent(e)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isEnabled
            || contentView == null
            || mHeaderView == null
            || mInVerticalNestedScrolling) {
            return false
        }
        val pointerIndex: Int
        when (ev.actionMasked) {
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
                    if (mPtrStateController.isInStartPosition || dy > 0) {
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
                if (dy > 0 || mPtrStateController.isInStartPosition) {
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
                val canMoveUp = mPtrStateController.hasLeftStartPosition()
                if (mPtrStateController.currentPos != 0) {
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
        if (deltaY < 0 && mPtrStateController.isInStartPosition) {
            return 0
        }
        var to = mPtrStateController.currentPos + deltaY.toInt()

        // over top
        if (mPtrStateController.willOverTop(to)) {
            to = mPtrStateController.startPosition
        }
        mPtrStateController.currentPos = to
        val change = to - mPtrStateController.lastPos
        updatePos(change)

        if (stateMachine.state is State.IDLE
            && mIsInTouchProgress
            && abs(change) > 0) {
            stateMachine.transition(Event.OnPull)
        }
        return change
    }

    private fun updatePos(change: Int) {
        if (change == 0) {
            return
        }
        mHeaderView?.offsetTopAndBottom(change)
        contentView?.offsetTopAndBottom(change)
        if (mPtrListenerHolder.hasHandler()) {
            mPtrListenerHolder.onPositionChange(this, mPtrStateController)
        }
    }

    private fun withFriction(force: Float): Int {
        return (force * mPullFriction).toInt()
    }

    private fun onRelease() {
        when (stateMachine.state) {
            is State.IDLE -> {}
            is State.DRAG -> {
                if (mPtrStateController.isOverOffsetToRefresh) {
                    stateMachine.transition(Event.OnReleaseToRefreshing)
                } else {
                    stateMachine.transition(Event.OnReleaseToIdle)
                }
            }
            is State.REFRESHING -> {}
        }
    }

    /**
     * Scroll back to to if is not under touch
     */
    private fun tryScrollBackToTop() {
        if (!mIsInTouchProgress) {
            mScrollChecker.scrollToStart()
        }
    }

    private fun performRefresh() {
        mLoadingStartTime = System.currentTimeMillis()
        if (mPtrListenerHolder.hasHandler()) {
            mPtrListenerHolder.onRefreshing(this)
        }
    }

    var isRefreshing: Boolean
        get() = stateMachine.state is State.REFRESHING
        set(isRefresh) {
            if (isRefresh) {
                autoRefresh()
            } else {
                refreshComplete()
            }
        }

    var currentState: State = stateMachine.state
        get() = stateMachine.state
        private set

    /**
     * Call this when data is loaded.
     * The UI will perform complete at once or after a delay, depends on the time elapsed is greater then [.mLoadingMinTime] or not.
     */
    private fun refreshComplete() {
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
        stateMachine.transition(Event.OnComplete)
    }

    /**
     * Do real refresh work. If there is a hook, execute the hook first.
     */
    private fun notifyUIRefreshComplete() {
        if (mPtrListenerHolder.hasHandler()) {
            mPtrListenerHolder.onComplete(this)
        }
        tryScrollBackToTop()
    }

    @JvmOverloads
    fun autoRefresh(duration: Int = mDurationToCloseHeader) {
        if (stateMachine.state !is State.IDLE) {
            return
        }
        doOnLayout {
            stateMachine.transition(Event.OnAutoRefresh)
        }
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
        if (mPtrStateController != indicator) {
            indicator.convertFrom(mPtrStateController)
        }
        mPtrStateController = indicator
    }

    var resistance: Float
        get() = mPtrStateController.resistance
        set(resistance) {
            mPtrStateController.resistance = resistance
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
        mPtrStateController.setRatioOfHeaderHeightToRefresh(ratio)
    }

    var offsetToRefresh: Int
        get() = mPtrStateController.offsetToRefresh
        set(offset) {
            mPtrStateController.offsetToRefresh = offset
        }

    val ratioOfHeaderToHeightRefresh: Float
        get() = mPtrStateController.ratioOfHeaderToHeightRefresh

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
                // && !isAutoRefresh // && !isRefreshing()
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
        if (dy > 0 && mTotalUnconsumed > 0 && isRefreshing) {
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
        var dyUnconsumedCopy = dyUnconsumed
        // if (mStatus != PTR_STATUS_INIT) {
        if (stateMachine.state !is State.IDLE) {
            dyUnconsumedCopy = withFriction(dyUnconsumedCopy.toFloat())
        }
        dispatchNestedScroll(
            dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumedCopy,
            mParentOffsetInWindow
        )

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        val dy = dyUnconsumedCopy + mParentOffsetInWindow[1]
        if (dy < 0 && !canChildScrollToUp()) {
            mTotalUnconsumed += abs(dy)
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

    private fun canChildScrollToUp(): Boolean {
        return if (contentView is ListView) {
            ListViewCompat.canScrollList(
                (contentView as ListView?)!!,
                -1
            )
        } else {
            contentView!!.canScrollVertically(-1)
        }
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
        } else if (yDiff < 0 && abs(yDiff) > mTouchSlop && isRefreshing) {
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
                reset()
            }
        }

        fun scrollToRefreshing() {
            mScrollChecker.tryToScrollTo(mPtrStateController.offsetToRefresh)
        }

        fun scrollToStart() {
            mScrollChecker.tryToScrollTo(mPtrStateController.startPosition)
        }

        fun tryToScrollTo(to: Int, duration: Int = 200) {
            if (mPtrStateController.isAlreadyHere(to) || to == mTo && isRunning) {
                return
            }
            mStart = mPtrStateController.currentPos
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
            movePos((target - mPtrStateController.currentPos).toFloat())
        }

        init {
            mAnimator.addUpdateListener(this)
            mAnimator.interpolator = AccelerateDecelerateInterpolator()
        }
    }
}