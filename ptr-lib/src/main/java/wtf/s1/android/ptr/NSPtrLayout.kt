package wtf.s1.android.ptr

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.os.Build
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.AbsListView
import android.widget.FrameLayout
import android.widget.ListView
import androidx.core.view.*
import androidx.core.widget.ListViewCompat
import kotlin.math.abs

/**
 * This layout view for "Pull to Refresh(Ptr)" support all of the view,
 * you can contain everything you want.
 * support: pull to refresh / release to refresh / auto refresh / keep header view
 * while refreshing / hide header view while refreshing
 * It defines [NSPtrListener], which allows you customize the UI easily.
 */
open class NSPtrLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle), NestedScrollingParent3, NestedScrollingChild3 {

    companion object {
        private const val INVALID_POINTER = -1
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
        // 下拉事件
        object Pull : Event()
        // 放手回到空闲
        object ReleaseToIdle : Event()
        // 放手开始刷新
        object ReleaseToRefreshing : Event()
        // 刷新完成
        object RefreshComplete : Event()
        // 自动刷新
        object AutoRefresh : Event()
    }

    sealed class SideEffect {
        // 手势取消回到顶部
        object OnCancelToIdle : SideEffect()
        // 刷新动作
        object OnRefreshing : SideEffect()
        // 开始拖动控件
        object OnDragBegin : SideEffect()
        // 刷新完成的动作
        object OnComplete : SideEffect()
    }

    private var stateMachine =
        StateMachine.create<State, Event, SideEffect> {
            initialState(State.IDLE)

            state<State.IDLE> {
                on<Event.Pull> {
                    transitionTo(State.DRAG, SideEffect.OnDragBegin)
                }
                on<Event.AutoRefresh> {
                    transitionTo(State.REFRESHING, SideEffect.OnRefreshing)
                }
            }

            state<State.REFRESHING> {
                on<Event.RefreshComplete> {
                    transitionTo(State.IDLE, SideEffect.OnComplete)
                }
            }

            state<State.DRAG> {
                on<Event.ReleaseToIdle> {
                    transitionTo(State.IDLE, SideEffect.OnCancelToIdle)
                }
                on<Event.ReleaseToRefreshing> {
                    transitionTo(State.REFRESHING, SideEffect.OnRefreshing)
                }
                on<Event.RefreshComplete> {
                    transitionTo(State.IDLE, SideEffect.OnComplete)
                }
            }

            onTransition {
                val validTransition = it as? StateMachine.Transition.Valid ?: return@onTransition
                when (validTransition.sideEffect) {
                    SideEffect.OnDragBegin -> {
                    }
                    SideEffect.OnComplete -> {
                        tryScrollBackToTop()
                        notifyUIRefreshComplete()
                    }
                    SideEffect.OnRefreshing -> {
                        performRefresh()
                    }
                    SideEffect.OnCancelToIdle -> {
                        tryScrollBackToTop()
                    }
                }
                mPtrListenerHolder.onTransition(this@NSPtrLayout, it)
            }

            onEvent {
                mPtrListenerHolder.onEvent(it)
            }
        }

    private val ptrId = "ptr-frame-" + ++ID
    private var contentView: View? = null

    var config = object :NSPtrConfig {

        override fun getLayout(): NSPtrLayout {
            return this@NSPtrLayout
        }

        override fun startPosition(): Int {
            return 0
        }

        override fun atStartPosition(): Boolean {
            return contentTopPosition == startPosition()
        }

        override fun overToRefreshPosition(): Boolean {
            return contentTopPosition > headerView?.height ?: measuredHeight
        }

        override fun refreshPosition(): Int {
            return headerView?.height ?: measuredHeight
        }

        override fun pullFriction(type: Int): Float {
            return if (type == ViewCompat.TYPE_TOUCH) super.pullFriction(type) else 2f
        }
    }

    // optional config for define header and content in xml file
    private var mHeaderId = 0
    private var mContainerId = 0

    // ptr config
    private var mLoadingMinTime = 500
    private var mLoadingStartTime: Long = 0
    private var mHeaderView: View? = null
    private val mPtrListenerHolder = NSPtrListenerHolder()

    // working parameters
    private val mScrollChecker = ScrollChecker()
    var contentTopPosition = 0
        private set

    //touch handle
    private var mActivePointerId = INVALID_POINTER
    private var mIsBeingDragged = false
    private val mInitialTouch = PointF((-1).toFloat(), (-1).toFloat())
    private val mLastTouch = PointF((-1).toFloat(), (-1).toFloat())
    private val mTouchSlop: Int
    private var mIsInTouchProgress = false

    //NestScroll
    @Suppress("LeakingThis")
    private val mNestedScrollingParentHelper = NestedScrollingParentHelper(this)
    @Suppress("LeakingThis")
    private val mNestedScrollingChildHelper = NestedScrollingChildHelper(this)
    private val mScrollOffset = IntArray(2)
    private val mScrollConsumed = IntArray(2)
    private val mParentOffsetInWindow = IntArray(2)
    private val mParentScrollConsumed = IntArray(2)
    private var mInVerticalNestedScrollTouch = false
    private val mPerformRefreshCompleteDelay: Runnable = Runnable { performRefreshComplete() }


    init {
        val arr = context.obtainStyledAttributes(attrs, R.styleable.NSPtrLayout, 0, 0)
        if (arr != null) {
            mHeaderId = arr.getResourceId(R.styleable.NSPtrLayout_ptr_header, mHeaderId)
            mContainerId = arr.getResourceId(R.styleable.NSPtrLayout_ptr_content, mContainerId)
            arr.recycle()
        }
        val conf = ViewConfiguration.get(getContext())
        mTouchSlop = conf.scaledTouchSlop
        isNestedScrollingEnabled = true
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        super.addView(child, index, params)
        if (child is NSPtrListener) {
            mPtrListenerHolder.addListener(child)
        } else {
            if (contentView == null) {
                contentView = child
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onFinishInflate() {
        val childCount = childCount// both are not specified

        if (childCount == 1) {
            contentView = getChildAt(0)
        } else {
            if (mHeaderId != 0 && mHeaderView == null) {
                mHeaderView = findViewById(mHeaderId)
            }
            if (mContainerId != 0 && contentView == null) {
                contentView = findViewById(mContainerId)
            }

            // not specify header or content
            if (contentView == null || mHeaderView == null) {
                children.forEach {
                    if (it is NSPtrHeader && mHeaderView == null) {
                        mHeaderView = it
                    }
                    if (it !is NSPtrHeader
                        && it !is NSPtrFooter
                        && contentView == null) {
                        contentView = it
                    }
                }
            }
        }
        super.onFinishInflate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(mPerformRefreshCompleteDelay)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        children.forEach {child->
            if (!child.isGone) {
               if (child is NSPtrComponent) {
                    child.prtMeasure(this, widthMeasureSpec, heightMeasureSpec)
                } else {
                    measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
                }
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        layoutChildren(l, t, r, b)
    }

    private fun layoutChildren(l: Int, t: Int, r: Int, b: Int) {
        val offset = contentTopPosition
        val parentLeft: Int = paddingLeft
        val parentRight: Int = r - l - paddingRight
        val parentTop: Int = paddingTop
        val parentBottom: Int = b - t - paddingBottom

        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {

                if (child === contentView) {
                    val lp = child.layoutParams as MarginLayoutParams
                    val left = paddingLeft + lp.leftMargin
                    val top = paddingTop + lp.topMargin + offset
                    val right = left + child.measuredWidth
                    val bottom = top + child.measuredHeight
                    child.layout(left, top, right, bottom)
                } else if (child is NSPtrComponent) {
                    child.ptrLayout(this)
                } else {
                    val lp = child.layoutParams as FrameLayout.LayoutParams
                    val width = child.measuredWidth
                    val height = child.measuredHeight
                    var childLeft: Int
                    var childTop: Int
                    var gravity = lp.gravity
                    if (gravity == -1) {
                        gravity = Gravity.START or Gravity.TOP
                    }

                    val absoluteGravity =
                        Gravity.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(this))
                    val verticalGravity = gravity and Gravity.VERTICAL_GRAVITY_MASK
                    childLeft = when (absoluteGravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
                        Gravity.CENTER_HORIZONTAL ->
                            (paddingLeft
                                    + (parentRight - parentLeft - width) / 2
                                    + lp.leftMargin
                                    - lp.rightMargin)
                        Gravity.RIGHT ->
                            parentRight - width - lp.rightMargin
                        Gravity.LEFT ->
                            parentLeft + lp.leftMargin
                        else ->
                            parentLeft + lp.leftMargin
                    }
                    childTop = when (verticalGravity) {
                        Gravity.TOP -> parentTop + lp.topMargin
                        Gravity.CENTER_VERTICAL -> (parentTop
                                + (parentBottom - parentTop - height) / 2
                                + lp.topMargin - lp.bottomMargin)
                        Gravity.BOTTOM -> parentBottom - height - lp.bottomMargin
                        else -> parentTop + lp.topMargin
                    }
                    child.layout(childLeft, childTop, childLeft + width, childTop + height)
                }
            }
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
            MotionEvent.ACTION_DOWN -> {
                stopNestedScroll(ViewCompat.TYPE_NON_TOUCH)
                mIsInTouchProgress = true
            }
        }
        return super.dispatchTouchEvent(e)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isEnabled
            || contentView == null
            || mInVerticalNestedScrollTouch
        ) {
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
                mInitialTouch.set(ev.getX(pointerIndex), ev.getY(pointerIndex))
                mLastTouch.set(ev.getX(pointerIndex), ev.getY(pointerIndex))
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
                    if (config.atStartPosition() || dy > 0) {
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled
            || mInVerticalNestedScrollTouch) {
            return false
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> return true
            MotionEvent.ACTION_DOWN -> {
                val pointerIndex = event.findPointerIndex(mActivePointerId)
                if (pointerIndex < 0) {
                    Log.e(ptrId, "Got ACTION_MOVE event but have an invalid active pointer id.")
                    return false
                }
                mInitialTouch.set(event.getX(pointerIndex), event.getY(pointerIndex))
                mLastTouch.set(event.getX(pointerIndex), event.getY(pointerIndex))
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
                return !canChildScrollToUp()
            }
            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findPointerIndex(mActivePointerId)
                if (pointerIndex < 0) {
                    Log.e(ptrId, "Got ACTION_MOVE event but have an invalid active pointer id.")
                    return false
                }
                if (mLastTouch.x < 0 && mLastTouch.y < 0) {
                    mLastTouch.set(event.getX(pointerIndex), event.getY(pointerIndex))
                    return false
                }

                val y = (event.getY(pointerIndex) + 0.5f).toInt()
                var dy = (y - mLastTouch.y).toInt()
                if (dy > 0 || config.atStartPosition()) {
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
                    dy = withFriction(dy.toFloat(), ViewCompat.TYPE_TOUCH)
                }
                val moveDown = dy > 0
                val moveUp = !moveDown
                val canMoveUp = config.atStartPosition()
                if (contentTopPosition != 0) {
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
        if (deltaY < 0 && config.atStartPosition()) {
            return 0
        }
        var to = contentTopPosition + deltaY.toInt()

        // over top
        if (to < config.startPosition()) {
            to = config.startPosition()
        }

        val change = to - contentTopPosition
        contentTopPosition = to
        updatePos(change)

        if (stateMachine.state is State.IDLE
            && mIsInTouchProgress
            && abs(change) > 0
        ) {
            stateMachine.transition(Event.Pull)
        }
        return change
    }

    private fun updatePos(change: Int) {
        if (change == 0) {
            return
        }
        contentView?.let {
            ViewCompat.offsetTopAndBottom(it, change)
        }
        if (mPtrListenerHolder.hasHandler()) {
            mPtrListenerHolder.onPositionChange(this, change)
        }
    }

    private fun withFriction(force: Float, touchType: Int): Int {
        return (force * config.pullFriction(touchType)).toInt()
    }

    private fun onRelease() {
        when (stateMachine.state) {
            is State.IDLE -> {
                if (contentTopPosition != config.startPosition()) {
                    mScrollChecker.scrollToStart()
                }
            }
            is State.DRAG -> {
                if (config.overToRefreshPosition()) {
                    stateMachine.transition(Event.ReleaseToRefreshing)
                } else {
                    stateMachine.transition(Event.ReleaseToIdle)
                }
            }
            is State.REFRESHING -> {
                if (isOverToRefreshPosition) {
                    mScrollChecker.scrollToRefreshing()
                }
            }
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
        mScrollChecker.scrollToRefreshing()
        mLoadingStartTime = SystemClock.uptimeMillis()
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
        val delay = (mLoadingMinTime - (SystemClock.uptimeMillis() - mLoadingStartTime)).toInt()
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
        stateMachine.transition(Event.RefreshComplete)
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

    private fun autoRefresh() {
        if (stateMachine.state != State.IDLE) {
            return
        }
        doOnLayout {
            stateMachine.transition(Event.AutoRefresh)
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

    fun addPtrListener(ptrListener: NSPtrListener) {
        mPtrListenerHolder.addListener(ptrListener)
    }

    fun removePtrListener(ptrListener: NSPtrListener) {
        mPtrListenerHolder.removeListener(ptrListener)
    }

    var isOverToRefreshPosition: Boolean = false
        private set
        get() {
            return config.overToRefreshPosition()
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
            addView(header, 0)
        }

    // <editor-fold defaultstate="collapsed" desc="generate layout params">
    @Suppress("unused")
    class LayoutParams : FrameLayout.LayoutParams {
        constructor(c: Context, attrs: AttributeSet) : super(c, attrs)
        constructor(width: Int, height: Int) : super(width, height)
        constructor(source: MarginLayoutParams) : super(source)
        constructor(source: ViewGroup.LayoutParams) : super(source)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        return p is LayoutParams
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams): LayoutParams {
        return LayoutParams(p)
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
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
        return startNestedScroll(axes, ViewCompat.TYPE_TOUCH)
    }

    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        return mNestedScrollingChildHelper.startNestedScroll(axes, type)
    }

    override fun stopNestedScroll() {
        stopNestedScroll(ViewCompat.TYPE_TOUCH)
    }

    override fun stopNestedScroll(type: Int) {
        mNestedScrollingChildHelper.stopNestedScroll(type)
    }

    override fun hasNestedScrollingParent(): Boolean {
        return hasNestedScrollingParent(ViewCompat.TYPE_TOUCH)
    }

    override fun hasNestedScrollingParent(type: Int): Boolean {
        return mNestedScrollingChildHelper.hasNestedScrollingParent(type)
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

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int,
        consumed: IntArray
    ) {
        mNestedScrollingChildHelper.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            offsetInWindow,
            type,
            consumed
        )
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return mNestedScrollingChildHelper.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            offsetInWindow,
            type
        )
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ): Boolean {
        return dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, ViewCompat.TYPE_TOUCH)
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(
            dx, dy, consumed, offsetInWindow, type
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
        return onStartNestedScroll(child, target, axes, ViewCompat.TYPE_TOUCH)
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        return isEnabled && axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        onNestedScrollAccepted(child, target, axes, ViewCompat.TYPE_TOUCH)
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes, type)
        // Dispatch up to the nested parent
        startNestedScroll(axes and ViewCompat.SCROLL_AXIS_VERTICAL, type)
        if (type == ViewCompat.TYPE_TOUCH) {
            mInVerticalNestedScrollTouch = true
        }
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        onNestedPreScroll(target, dx, dy, consumed, ViewCompat.TYPE_TOUCH)
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        // If we are in the middle of consuming, a scroll, then we want to move the ptr back up
        // before allowing the list to scroll
        if (dy > 0 && !config.atStartPosition()) {
            consumed[1] = -movePos(-dy.toFloat())
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
        onNestedScroll(
            target,
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            ViewCompat.TYPE_TOUCH
        )
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        internalOnNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            type,
            null
        )
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        internalOnNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            type,
            consumed
        )
    }

    open fun internalOnNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray?
    ) {

        // Dispatch up to the nested parent first
        mNestedScrollingChildHelper.dispatchNestedScroll(
            dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
            mParentOffsetInWindow, type, consumed
        )

        // todo var dyUnconsumedCopy = dyUnconsumed + (consumed?.getOrElse(1) {0} ?:0)
        val dyUnconsumedCopy = withFriction(dyUnconsumed.toFloat(), type)
        val dy = dyUnconsumedCopy + mParentOffsetInWindow[1]
        movePos(-dy.toFloat())
    }

    override fun onStopNestedScroll(child: View) {
        onStopNestedScroll(child, ViewCompat.TYPE_TOUCH)
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        mNestedScrollingParentHelper.onStopNestedScroll(target, type)

        // Dispatch up our nested parent
        stopNestedScroll(type)
        if (type == ViewCompat.TYPE_TOUCH) {
            mInVerticalNestedScrollTouch = false
        }

        if (type == ViewCompat.TYPE_NON_TOUCH) {
            onRelease()
        } else {
            mLastTouch.set(-1f, -1f)
        }
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
        val xDiff = abs(move.x - mInitialTouch.x)
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
            mScrollChecker.tryToScrollTo(config.refreshPosition())
        }

        fun scrollToStart() {
            mScrollChecker.tryToScrollTo(config.startPosition())
        }

        private fun tryToScrollTo(to: Int, duration: Int = 200) {
            if (contentTopPosition == to || to == mTo && isRunning) {
                return
            }
            mStart = contentTopPosition
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
            movePos((target - contentTopPosition).toFloat())
        }

        init {
            mAnimator.addUpdateListener(this)
            mAnimator.interpolator = AccelerateDecelerateInterpolator()
        }
    }
}