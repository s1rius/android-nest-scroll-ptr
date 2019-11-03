package in.srain.cube.views.ptr;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.NestedScrollingParent;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ListViewCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import in.srain.cube.views.ptr.indicator.PtrIndicator;
import in.srain.cube.views.ptr.util.PtrCLog;

/**
 * This layout view for "Pull to Refresh(Ptr)" support all of the view, you can contain everything you want.
 * support: pull to refresh / release to refresh / auto refresh / keep header view while refreshing / hide header view while refreshing
 * It defines {@link PtrListener}, which allows you customize the UI easily.
 */
public class PtrFrameLayout extends ViewGroup implements NestedScrollingParent,
        NestedScrollingChild {

    // status enum
    public final static int PTR_STATUS_INIT = 1;
    private static final int INVALID_POINTER = -1;
    private float mPullFriction = 0.56f;

    @IntDef({PTR_STATUS_INIT, PTR_STATUS_PREPARE, PTR_STATUS_LOADING, PTR_STATUS_COMPLETE})
    @Retention(RetentionPolicy.SOURCE)
    @interface PlayState {

    }

    private @PlayState
    int mStatus = PTR_STATUS_INIT;
    public final static int PTR_STATUS_PREPARE = 2;
    public final static int PTR_STATUS_LOADING = 3;
    public final static int PTR_STATUS_COMPLETE = 4;
    public static boolean DEBUG = true;
    private static int ID = 1;
    protected final String LOG_TAG = "ptr-frame-" + ++ID;
    // auto refresh status
    private final static byte FLAG_AUTO_REFRESH_AT_ONCE = 0x01;
    private final static byte FLAG_AUTO_REFRESH_BUT_LATER = 0x01 << 1;
    private final static byte FLAG_ENABLE_NEXT_PTR_AT_ONCE = 0x01 << 2;
    private final static byte FLAG_PIN_CONTENT = 0x01 << 3;
    private final static byte MASK_AUTO_REFRESH = 0x03;
    protected View mContent;
    // optional config for define header and content in xml file
    private int mHeaderId = 0;
    private int mContainerId = 0;
    // ptr config
    private int mLoadingMinTime = 500;
    private long mLoadingStartTime = 0;
    private int mFlag = 0x00;
    private int mDurationToLoadingPosition = 200;
    private int mDurationToCloseHeader = 200;
    private boolean mKeepHeaderWhenRefresh = true;
    private boolean mPullToRefresh = false;
    private View mHeaderView;
    private PtrListenerHolder mPtrListenerHolder = PtrListenerHolder.create();
    private PtrHandler mPtrHandler;
    // working parameters
    private ScrollChecker mScrollChecker;

    private int mHeaderHeight;
    // enable margin property on layout or not
    private boolean mHeaderLayoutMarginEnable = false;
    private PtrUIHandlerHook mRefreshCompleteHook;

    private PtrIndicator mPtrIndicator;

    //touch handle
    private int mActivePointerId = INVALID_POINTER;
    private boolean mIsBeingDragged = false;
    private PointF mInitialTouch = new PointF(-1, -1);
    private PointF mLastTouch = new PointF(-1, -1);
    private int mTouchSlop;
    private boolean mIsInTouchProgress;

    //NestScroll
    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];
    private final int[] mParentScrollConsumed = new int[2];
    private int mTotalUnconsumed;
    private boolean mInVerticalNestedScrolling;

    private Runnable mPerformRefreshCompleteDelay = new Runnable() {
        @Override
        public void run() {
            performRefreshComplete();
        }
    };

    public PtrFrameLayout(Context context) {
        this(context, null);
    }

    public PtrFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PtrFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mPtrIndicator = new PtrIndicator();

        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.PtrFrameLayout, 0, 0);
        if (arr != null) {

            mHeaderId = arr.getResourceId(R.styleable.PtrFrameLayout_ptr_header, mHeaderId);
            mContainerId = arr.getResourceId(R.styleable.PtrFrameLayout_ptr_content, mContainerId);

            mPtrIndicator.setResistance(
                    arr.getFloat(R.styleable.PtrFrameLayout_ptr_resistance, mPtrIndicator.getResistance()));

            mDurationToLoadingPosition = arr.getInt(R.styleable.PtrFrameLayout_ptr_duration_to_loading_position, mDurationToLoadingPosition);
            mDurationToCloseHeader = arr.getInt(R.styleable.PtrFrameLayout_ptr_duration_to_close_header, mDurationToCloseHeader);

            float ratio = mPtrIndicator.getRatioOfHeaderToHeightRefresh();
            ratio = arr.getFloat(R.styleable.PtrFrameLayout_ptr_ratio_of_header_height_to_refresh, ratio);
            mPtrIndicator.setRatioOfHeaderHeightToRefresh(ratio);

            mKeepHeaderWhenRefresh = arr.getBoolean(R.styleable.PtrFrameLayout_ptr_keep_header_when_refresh, mKeepHeaderWhenRefresh);

            mPullToRefresh = arr.getBoolean(R.styleable.PtrFrameLayout_ptr_pull_to_fresh, mPullToRefresh);
            arr.recycle();
        }

        mScrollChecker = new ScrollChecker();

        final ViewConfiguration conf = ViewConfiguration.get(getContext());
        mTouchSlop = conf.getScaledTouchSlop();
        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
    }

    @Override
    protected void onFinishInflate() {
        final int childCount = getChildCount();
        if (childCount > 2) {
            throw new IllegalStateException("PtrFrameLayout can only contains 2 children");
        } else if (childCount == 2) {
            if (mHeaderId != 0 && mHeaderView == null) {
                mHeaderView = findViewById(mHeaderId);
            }
            if (mContainerId != 0 && mContent == null) {
                mContent = findViewById(mContainerId);
            }

            // not specify header or content
            if (mContent == null || mHeaderView == null) {

                View child1 = getChildAt(0);
                View child2 = getChildAt(1);
                if (child1 instanceof PtrListener) {
                    mHeaderView = child1;
                    mContent = child2;
                } else if (child2 instanceof PtrListener) {
                    mHeaderView = child2;
                    mContent = child1;
                } else {
                    // both are not specified
                    if (mContent == null && mHeaderView == null) {
                        mHeaderView = child1;
                        mContent = child2;
                    }
                    // only one is specified
                    else {
                        if (mHeaderView == null) {
                            mHeaderView = mContent == child1 ? child2 : child1;
                        } else {
                            mContent = mHeaderView == child1 ? child2 : child1;
                        }
                    }
                }
            }
        } else if (childCount == 1) {
            mContent = getChildAt(0);
        } else {
            TextView errorView = new TextView(getContext());
            errorView.setClickable(true);
            errorView.setTextColor(0xffff6600);
            errorView.setGravity(Gravity.CENTER);
            errorView.setTextSize(20);
            errorView.setText("The content view in PtrFrameLayout is empty. Do you forget to specify its id in xml layout file?");
            mContent = errorView;
            addView(mContent);
        }
        if (mHeaderView != null) {
            mHeaderView.bringToFront();
        }
        super.onFinishInflate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mPerformRefreshCompleteDelay != null) {
            removeCallbacks(mPerformRefreshCompleteDelay);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (DEBUG) {
            PtrCLog.d(LOG_TAG, "onMeasure frame: width: %s, height: %s, padding: %s %s %s %s",
                    getMeasuredHeight(), getMeasuredWidth(),
                    getPaddingLeft(), getPaddingRight(), getPaddingTop(), getPaddingBottom());

        }

        if (mHeaderView != null) {
            measureChildWithMargins(mHeaderView, widthMeasureSpec, 0, heightMeasureSpec, 0);
            MarginLayoutParams lp = (MarginLayoutParams) mHeaderView.getLayoutParams();
            mHeaderHeight = mHeaderView.getMeasuredHeight() + (isHeaderLayoutMarginEnable() ? 0 : (lp.topMargin + lp.bottomMargin));
            mPtrIndicator.setHeaderHeight(mHeaderHeight);
        }

        if (mContent != null) {
            measureContentView(mContent, widthMeasureSpec, heightMeasureSpec);
            if (DEBUG) {
                ViewGroup.MarginLayoutParams lp = (MarginLayoutParams) mContent.getLayoutParams();
                PtrCLog.d(LOG_TAG, "onMeasure content, width: %s, height: %s, margin: %s %s %s %s",
                        getMeasuredWidth(), getMeasuredHeight(),
                        lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin);
                PtrCLog.d(LOG_TAG, "onMeasure, currentPos: %s, lastPos: %s, top: %s",
                        mPtrIndicator.getCurrentPosY(), mPtrIndicator.getLastPosY(), mContent.getTop());
            }
        }
    }

    private void measureContentView(View child,
                                    int parentWidthMeasureSpec,
                                    int parentHeightMeasureSpec) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin, lp.width);
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                getPaddingTop() + getPaddingBottom() + lp.topMargin, lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean flag, int i, int j, int k, int l) {
        layoutChildren();
    }

    private void layoutChildren() {
        int offset = mPtrIndicator.getCurrentPosY();
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        if (mHeaderView != null) {
            MarginLayoutParams lp = (MarginLayoutParams) mHeaderView.getLayoutParams();
            final int left = paddingLeft + lp.leftMargin;
            // enhance readability(header is layout above screen when first init)
            final int top = -(mHeaderHeight - paddingTop - lp.topMargin - offset);
            final int right = left + mHeaderView.getMeasuredWidth();
            final int bottom = top + mHeaderView.getMeasuredHeight();
            mHeaderView.layout(left, top, right, bottom);
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "onLayout header: %s %s %s %s", left, top, right, bottom);
            }
        }
        if (mContent != null) {
            if (isPinContent()) {
                offset = 0;
            }
            MarginLayoutParams lp = (MarginLayoutParams) mContent.getLayoutParams();
            final int left = paddingLeft + lp.leftMargin;
            final int top = paddingTop + lp.topMargin + offset;
            final int right = left + mContent.getMeasuredWidth();
            final int bottom = top + mContent.getMeasuredHeight();
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "onLayout content: %s %s %s %s", left, top, right, bottom);
            }
            mContent.layout(left, top, right, bottom);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (DEBUG) PtrCLog.d(LOG_TAG, "dispatch Touch = " + e.toString());
        if (mScrollChecker.isRunning()) {
            mScrollChecker.abortIfWorking();
        }
        int actionMasked = e.getActionMasked();
        switch (actionMasked) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsInTouchProgress = false;
                mIsBeingDragged = false;
                stopNestedScroll();
                if (mPtrIndicator.hasLeftStartPosition()) {
                    if (DEBUG) {
                        PtrCLog.d(LOG_TAG, "call onRelease when user release");
                    }
                    onRelease(false);
                }
                break;
            case MotionEvent.ACTION_DOWN:
                mIsInTouchProgress = true;
            default:
                break;
        }
        return super.dispatchTouchEvent(e);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isEnabled() || mContent == null || mHeaderView == null || mInVerticalNestedScrolling) {
            return false;
        }

        int pointerIndex;

        final int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                mIsBeingDragged = false;
                if (pointerIndex < 0) {
                    return false;
                }
                mInitialTouch.set(ev.getX(pointerIndex), ev.getY(pointerIndex));
                mLastTouch.set(ev.getX(pointerIndex), ev.getY(pointerIndex));
                break;
            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    if (DEBUG)
                        PtrCLog.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }

                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }

                // if content is a scrollable view and not implement NestedScrollingChild
                // like ListView, we need enable nest scroll in intercept process
                final int y = (int) (ev.getY(pointerIndex) + 0.5f);

                int dy = (int) (mLastTouch.y - y);

                if (dy != 0 || mPtrIndicator.isInStartPosition()) {
                    if (startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)) {
                        if (dispatchNestedPreScroll(0, dy, mScrollConsumed, mScrollOffset)) {
                            mLastTouch.y = dy - mScrollOffset[1];
                            // handle touch when parent not accept nest scroll
                            return true;
                        }
                    }
                }

                startDragging(new PointF(ev.getX(), ev.getY()));
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mIsBeingDragged = false;
                break;
        }
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || canChildScrollToUp() || mInVerticalNestedScrolling) {
            return false;
        }

        int action = event.getActionMasked();
        int pointerIndex = event.findPointerIndex(mActivePointerId);
        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return true;
            case MotionEvent.ACTION_DOWN:
                mInitialTouch.set(event.getX(pointerIndex), event.getY(pointerIndex));
                mLastTouch.set(event.getX(pointerIndex), event.getY(pointerIndex));

                if (!canChildScrollToUp()) {
                    if (DEBUG) {
                        PtrCLog.d(LOG_TAG, "handle action down touch event");
                    }
                    return true;
                }
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                return false;
            case MotionEvent.ACTION_MOVE:
                final int y = (int) (event.getY(pointerIndex) + 0.5f);

                int dy = (int) (mLastTouch.y - y);

                if (dy < 0 || mPtrIndicator.isInStartPosition()) {
                    if (startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)) {
                        if (dispatchNestedPreScroll(0, dy, mScrollConsumed, mScrollOffset)) {
                            mLastTouch.y = y - mScrollOffset[1];
                            // handle touch when parent not accept nest scroll
                            return true;
                        }
                    }
                }
                mLastTouch.y = y;

                float offsetY = -dy;
                if (offsetY == 0) {
                    return true;
                }

                if (offsetY > 0) {
                    offsetY = withFriction(offsetY);
                }

                boolean moveDown = offsetY > 0;
                boolean moveUp = !moveDown;
                boolean canMoveUp = mPtrIndicator.hasLeftStartPosition();

                if (DEBUG) {
                    boolean canMoveDown = mPtrHandler != null && mPtrHandler.checkCanDoRefresh(this, mContent, mHeaderView);
                    PtrCLog.v(LOG_TAG, "ACTION_MOVE: offsetY:%s, currentPos: %s, moveUp: %s, canMoveUp: %s, moveDown: %s: canMoveDown: %s", offsetY, mPtrIndicator.getCurrentPosY(), moveUp, canMoveUp, moveDown, canMoveDown);
                }

                if (mPtrIndicator.getCurrentPosY() != 0) {
                    movePos(offsetY);
                    return true;
                }

                // disable move when header not reach top
                if (moveDown && mPtrHandler != null && !mPtrHandler.checkCanDoRefresh(this, mContent, mHeaderView)) {
                    return false;
                }

                if ((moveUp && canMoveUp) || moveDown) {
                    movePos(offsetY);
                    return true;
                }
        }
        return super.onTouchEvent(event);
    }

    /**
     * //if deltaY > 0, move the content down
     * @param deltaY the y offset
     */
    private void movePos(float deltaY) {
        // has reached the top
        if ((deltaY < 0 && mPtrIndicator.isInStartPosition())) {
            if (DEBUG) {
                PtrCLog.e(LOG_TAG, "has reached the top");
            }
            return;
        }

        int to = mPtrIndicator.getCurrentPosY() + (int) deltaY;

        // over top
        if (mPtrIndicator.willOverTop(to)) {
            if (DEBUG) {
                PtrCLog.e(LOG_TAG, "over top");
            }
            to = PtrIndicator.POS_START;
        }

        mPtrIndicator.setCurrentPos(to);
        if (mPtrIndicator.isInStartPosition() && isRefreshing()) {
            performRefreshComplete();
        }
        int change = to - mPtrIndicator.getLastPosY();
        updatePos(change);
    }

    private void updatePos(int change) {
        if (change == 0) {
            return;
        }


        // leave initiated position or just refresh complete
        if ((mPtrIndicator.hasJustLeftStartPosition() && mStatus == PTR_STATUS_INIT) ||
                (mPtrIndicator.goDownCrossFinishPosition() && mStatus == PTR_STATUS_COMPLETE && isEnabledNextPtrAtOnce())) {
            changeStatusTo(PTR_STATUS_PREPARE);
        }

        // back to initiated position
        if (mPtrIndicator.hasJustBackToStartPosition()) {
            tryToNotifyReset();
        }

        // Pull to Refresh
        if (mStatus == PTR_STATUS_PREPARE) {
            // reach fresh height while moving from top to bottom
            if (!isAutoRefresh() && mPullToRefresh
                    && mPtrIndicator.crossRefreshLineFromTopToBottom()) {
                tryToPerformRefresh();
            }
            // reach header height while auto refresh
            if (performAutoRefreshButLater() && mPtrIndicator.hasJustReachedHeaderHeightFromTopToBottom()) {
                tryToPerformRefresh();
            }
        }

        if (DEBUG) {
            PtrCLog.v(LOG_TAG, "updatePos: change: %s, current: %s last: %s, top: %s, headerHeight: %s",
                    change, mPtrIndicator.getCurrentPosY(), mPtrIndicator.getLastPosY(), mContent.getTop(), mHeaderHeight);
        }

        mHeaderView.offsetTopAndBottom(change);
        if (!isPinContent()) {
            mContent.offsetTopAndBottom(change);
        }
        invalidate();

        if (mPtrListenerHolder.hasHandler()) {
            mPtrListenerHolder.onPositionChange(this, mStatus, mPtrIndicator);
        }
    }

    private int withFriction(float force) {
        return (int) (force * mPullFriction);
    }

    public void changeStatusTo(int status) {
        if (DEBUG) {
            PtrCLog.d(LOG_TAG, "status change from " + mStatus + " to " + status);
        }
        mStatus = status;
        onStateChange(mStatus);
    }

    public void onStateChange(int status) {
        switch (status) {
            case PTR_STATUS_INIT:
                clearFlag();
                mPtrListenerHolder.onReset(this);
                break;
            case PTR_STATUS_COMPLETE:
                // if is auto refresh do nothing, wait scroller stop
                if (mScrollChecker.mIsRunning && isAutoRefresh()) {
                    // do nothing
                    if (DEBUG) {
                        PtrCLog.d(LOG_TAG, "performRefreshComplete do nothing, scrolling: %s, auto refresh: %s",
                                mScrollChecker.mIsRunning, mFlag);
                    }
                    return;
                }
                notifyUIRefreshComplete(false);
                break;
            case PTR_STATUS_PREPARE:
                mPtrListenerHolder.onPrepare(this);
                break;
            case PTR_STATUS_LOADING:
                performRefresh();
                break;
        }
    }

    @SuppressWarnings("unused")
    public int getHeaderHeight() {
        return mHeaderHeight;
    }

    private void onRelease(boolean stayForLoading) {

        tryToPerformRefresh();

        if (mStatus == PTR_STATUS_LOADING) {
            // keep header for fresh
            if (mKeepHeaderWhenRefresh) {
                // scroll header back
                if (mPtrIndicator.isOverOffsetToKeepHeaderWhileLoading() && !stayForLoading) {
                    mScrollChecker.tryToScrollTo(
                            mPtrIndicator.getOffsetToKeepHeaderWhileLoading(),
                            mDurationToLoadingPosition);
                }
            } else {
                tryScrollBackToTopWhileLoading();
            }
        } else {
            if (mStatus == PTR_STATUS_COMPLETE) {
                notifyUIRefreshComplete(false);
            } else {
                tryScrollBackToTopAbortRefresh();
            }
        }
    }

    /**
     * please DO REMEMBER resume the hook
     *
     * @param hook prt refresh complete condition hook
     */

    public void setRefreshCompleteHook(PtrUIHandlerHook hook) {
        mRefreshCompleteHook = hook;
        hook.setResumeAction(new Runnable() {
            @Override
            public void run() {
                if (DEBUG) {
                    PtrCLog.d(LOG_TAG, "mRefreshCompleteHook resume.");
                }
                notifyUIRefreshComplete(true);
            }
        });
    }

    /**
     * Scroll back to to if is not under touch
     */
    private void tryScrollBackToTop() {
        if (!mIsInTouchProgress) {
            mScrollChecker.tryToScrollTo(PtrIndicator.POS_START, mDurationToCloseHeader);
        }
    }

    /**
     * just make easier to understand
     */
    private void tryScrollBackToTopWhileLoading() {
        tryScrollBackToTop();
    }

    /**
     * just make easier to understand
     */
    private void tryScrollBackToTopAfterComplete() {
        tryScrollBackToTop();
    }

    /**
     * just make easier to understand
     */
    private void tryScrollBackToTopAbortRefresh() {
        tryScrollBackToTop();
    }

    private boolean tryToPerformRefresh() {
        if (mStatus != PTR_STATUS_PREPARE) {
            return false;
        }

        //
        if ((mPtrIndicator.isOverOffsetToKeepHeaderWhileLoading() && isAutoRefresh())
                || mPtrIndicator.isOverOffsetToRefresh()) {
            changeStatusTo(PTR_STATUS_LOADING);
        }
        return false;
    }

    private void performRefresh() {
        mLoadingStartTime = System.currentTimeMillis();
        if (mPtrListenerHolder.hasHandler()) {
            mPtrListenerHolder.onBegin(this);
            if (DEBUG) {
                PtrCLog.i(LOG_TAG, "PtrUIHandler: onBegin");
            }
        }
        if (mPtrHandler != null) {
            mPtrHandler.onRefreshBegin(this);
        }
    }

    /**
     * If at the top and not in loading, reset
     */
    private boolean tryToNotifyReset() {
        if ((mStatus == PTR_STATUS_COMPLETE || mStatus == PTR_STATUS_PREPARE)
                && mPtrIndicator.isInStartPosition()) {
            changeStatusTo(PTR_STATUS_INIT);
            return true;
        }
        return false;
    }

    protected void onPtrScrollAbort() {
        if (mPtrIndicator.hasLeftStartPosition() && isAutoRefresh()) {
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "call onRelease after scroll abort");
            }
            onRelease(true);
        }
    }

    protected void onPtrScrollFinish() {
        if (mPtrIndicator.hasLeftStartPosition() && isAutoRefresh()) {
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "call onRelease after scroll finish");
            }
            onRelease(true);
        }
    }

    /**
     * Detect whether is refreshing.
     *
     * @return
     */
    public boolean isRefreshing() {
        return mStatus == PTR_STATUS_LOADING;
    }

    /**
     * Call this when data is loaded.
     * The UI will perform complete at once or after a delay, depends on the time elapsed is greater then {@link #mLoadingMinTime} or not.
     */
    final public void refreshComplete() {
        if (DEBUG) {
            PtrCLog.i(LOG_TAG, "refreshComplete");
        }

        if (mRefreshCompleteHook != null) {
            mRefreshCompleteHook.reset();
        }

        int delay = (int) (mLoadingMinTime - (System.currentTimeMillis() - mLoadingStartTime));
        if (delay <= 0) {
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "performRefreshComplete at once");
            }
            performRefreshComplete();
        } else {
            postDelayed(mPerformRefreshCompleteDelay, delay);
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "performRefreshComplete after delay: %s", delay);
            }
        }
    }

    /**
     * Do refresh complete work when time elapsed is greater than {@link #mLoadingMinTime}
     */
    private void performRefreshComplete() {
        changeStatusTo(PTR_STATUS_COMPLETE);
    }

    /**
     * Do real refresh work. If there is a hook, execute the hook first.
     *
     * @param ignoreHook
     */
    private void notifyUIRefreshComplete(boolean ignoreHook) {
        /**
         * After hook operation is done, {@link #notifyUIRefreshComplete} will be call in resume action to ignore hook.
         */
        if (mPtrIndicator.hasLeftStartPosition() && !ignoreHook && mRefreshCompleteHook != null) {
            if (DEBUG) {
                PtrCLog.d(LOG_TAG, "notifyUIRefreshComplete mRefreshCompleteHook run.");
            }

            mRefreshCompleteHook.takeOver();
            return;
        }
        if (mPtrListenerHolder.hasHandler()) {
            if (DEBUG) {
                PtrCLog.i(LOG_TAG, "PtrUIHandler: onComplete");
            }
            mPtrListenerHolder.onComplete(this);
        }
        mPtrIndicator.onUIRefreshComplete();
        tryScrollBackToTopAfterComplete();
        tryToNotifyReset();
    }

    public void autoRefresh() {
        autoRefresh(true, mDurationToCloseHeader);
    }

    public void autoRefresh(boolean atOnce) {
        autoRefresh(atOnce, mDurationToCloseHeader);
    }

    private void clearFlag() {
        // remove auto fresh flag
        mFlag = mFlag & ~MASK_AUTO_REFRESH;
    }

    public void autoRefresh(boolean atOnce, int duration) {

        if (mStatus != PTR_STATUS_INIT) {
            return;
        }

        mFlag |= atOnce ? FLAG_AUTO_REFRESH_AT_ONCE : FLAG_AUTO_REFRESH_BUT_LATER;

        changeStatusTo(PTR_STATUS_PREPARE);
        mScrollChecker.tryToScrollTo(mPtrIndicator.getOffsetToRefresh(), duration);
        if (atOnce) {
            changeStatusTo(PTR_STATUS_LOADING);
        }
    }

    public boolean isAutoRefresh() {
        return (mFlag & MASK_AUTO_REFRESH) > 0;
    }

    private boolean performAutoRefreshButLater() {
        return (mFlag & MASK_AUTO_REFRESH) == FLAG_AUTO_REFRESH_BUT_LATER;
    }

    public boolean isEnabledNextPtrAtOnce() {
        return (mFlag & FLAG_ENABLE_NEXT_PTR_AT_ONCE) > 0;
    }

    /**
     * If @param enable has been set to true. The user can perform next PTR at once.
     *
     * @param enable
     */
    public void setEnabledNextPtrAtOnce(boolean enable) {
        if (enable) {
            mFlag = mFlag | FLAG_ENABLE_NEXT_PTR_AT_ONCE;
        } else {
            mFlag = mFlag & ~FLAG_ENABLE_NEXT_PTR_AT_ONCE;
        }
    }

    public boolean isPinContent() {
        return (mFlag & FLAG_PIN_CONTENT) > 0;
    }

    /**
     * The content view will now move when {@param pinContent} set to true.
     *
     * @param pinContent
     */
    public void setPinContent(boolean pinContent) {
        if (pinContent) {
            mFlag = mFlag | FLAG_PIN_CONTENT;
        } else {
            mFlag = mFlag & ~FLAG_PIN_CONTENT;
        }
    }

    /**
     * loading will last at least for so long
     *
     * @param time
     */
    public void setLoadingMinTime(int time) {
        mLoadingMinTime = time;
    }

    /**
     * Not necessary any longer. Once moved, cancel event will be sent to child.
     *
     * @param yes
     */
    @Deprecated
    public void setInterceptEventWhileWorking(boolean yes) {
    }

    @SuppressWarnings({"unused"})
    public View getContentView() {
        return mContent;
    }

    @SuppressWarnings({"unused"})
    public void setPtrHandler(PtrHandler ptrHandler) {
        mPtrHandler = ptrHandler;
    }

    public void addPtrListener(PtrListener ptrListener) {
        mPtrListenerHolder.addListener(ptrListener);
    }

    @SuppressWarnings({"unused"})
    public void removePtrListener(PtrListener ptrListener) {
        mPtrListenerHolder.removeListener(ptrListener);
    }

    @SuppressWarnings({"unused"})
    public void setPtrIndicator(PtrIndicator indicator) {
        if (mPtrIndicator != null && mPtrIndicator != indicator) {
            indicator.convertFrom(mPtrIndicator);
        }
        mPtrIndicator = indicator;
    }

    @SuppressWarnings({"unused"})
    public float getResistance() {
        return mPtrIndicator.getResistance();
    }

    @SuppressWarnings({"unused"})
    public void setResistance(float resistance) {
        mPtrIndicator.setResistance(resistance);
    }

    /**
     * The duration to return back to the loading position
     *
     * @param duration
     */
    @SuppressWarnings({"unused"})
    public void setDurationToLoadingPosition(int duration) {
        mDurationToLoadingPosition = duration;
    }

    @SuppressWarnings({"unused"})
    public long getDurationToCloseHeader() {
        return mDurationToCloseHeader;
    }

    /**
     * The duration to close time
     *
     * @param duration
     */
    @SuppressWarnings({"unused"})
    public void setDurationToCloseHeader(int duration) {
        mDurationToCloseHeader = duration;
    }

    public void setRatioOfHeaderHeightToRefresh(float ratio) {
        mPtrIndicator.setRatioOfHeaderHeightToRefresh(ratio);
    }

    public int getOffsetToRefresh() {
        return mPtrIndicator.getOffsetToRefresh();
    }

    @SuppressWarnings({"unused"})
    public void setOffsetToRefresh(int offset) {
        mPtrIndicator.setOffsetToRefresh(offset);
    }

    @SuppressWarnings({"unused"})
    public float getRatioOfHeaderToHeightRefresh() {
        return mPtrIndicator.getRatioOfHeaderToHeightRefresh();
    }

    @SuppressWarnings({"unused"})
    public int getOffsetToKeepHeaderWhileLoading() {
        return mPtrIndicator.getOffsetToKeepHeaderWhileLoading();
    }

    @SuppressWarnings({"unused"})
    public void setOffsetToKeepHeaderWhileLoading(int offset) {
        mPtrIndicator.setOffsetToKeepHeaderWhileLoading(offset);
    }

    @SuppressWarnings({"unused"})
    public boolean isKeepHeaderWhenRefresh() {
        return mKeepHeaderWhenRefresh;
    }

    public void setKeepHeaderWhenRefresh(boolean keepOrNot) {
        mKeepHeaderWhenRefresh = keepOrNot;
    }

    public boolean isPullToRefresh() {
        return mPullToRefresh;
    }

    public void setPullToRefresh(boolean pullToRefresh) {
        mPullToRefresh = pullToRefresh;
    }

    public boolean isHeaderLayoutMarginEnable() {
        return mHeaderLayoutMarginEnable;
    }

    public void setHeaderLayoutMarginEnable(boolean enable) {
        mHeaderLayoutMarginEnable = enable;
    }

    @SuppressWarnings({"unused"})
    public View getHeaderView() {
        return mHeaderView;
    }

    public void setHeaderView(View header) {
        if (mHeaderView != null && header != null && mHeaderView != header) {
            removeView(mHeaderView);
        }
        ViewGroup.LayoutParams lp = header.getLayoutParams();
        if (lp == null) {
            lp = new LayoutParams(-1, -2);
            header.setLayoutParams(lp);
        }
        mHeaderView = header;
        addView(header);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p != null && p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    // nested scroll child
    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, @Nullable int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed,
                                           @Nullable int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(
                dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        // Re-dispatch up the tree by default
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        // Re-dispatch up the tree by default
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    // nested scroll parent
    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes) {
        return isEnabled() && !isAutoRefresh() && !isRefreshing()
                && (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        // Dispatch up to the nested parent
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mTotalUnconsumed = 0;

        mInVerticalNestedScrolling = true;
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed) {
        // If we are in the middle of consuming, a scroll, then we want to move the ptr back up
        // before allowing the list to scroll
        if (dy > 0 && mTotalUnconsumed > 0) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = dy - (int) mTotalUnconsumed;
                mTotalUnconsumed = 0;
            } else {
                mTotalUnconsumed -= dy;
                consumed[1] = dy;
            }
            movePos(-dy);
        }

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        // Dispatch up to the nested parent first
        if (mStatus != PTR_STATUS_INIT) {
            dyUnconsumed = withFriction(dyUnconsumed);
        }

        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                mParentOffsetInWindow);

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
        if (dy < 0 && !canChildScrollToUp()) {
            mTotalUnconsumed += Math.abs(dy);
        }

        movePos(-dy);
    }

    @Override
    public void onStopNestedScroll(View child) {
        mNestedScrollingParentHelper.onStopNestedScroll(child);
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (mTotalUnconsumed > 0) {
            //finishSpinner(mTotalUnconsumed);
            mTotalUnconsumed = 0;
        }
        // Dispatch up our nested parent
        stopNestedScroll();

        mInVerticalNestedScrolling = false;

    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if ((android.os.Build.VERSION.SDK_INT < 21 && mContent instanceof AbsListView)
                || (mContent != null && !ViewCompat.isNestedScrollingEnabled(mContent))) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }

    public boolean canChildScrollToUp() {
        boolean canChildScrollUp = true;
        if (mContent instanceof ListView) {
            canChildScrollUp = ListViewCompat.canScrollList((ListView) mContent, -1);
        } else {
            canChildScrollUp = mContent.canScrollVertically(-1);
        }
        if (DEBUG) PtrCLog.d(LOG_TAG, "canChildScrollToUp = " + canChildScrollUp);
        return canChildScrollUp;
    }

    private void startDragging(PointF move) {
        final float yDiff = move.y - mInitialTouch.y;
        final float xDiff = Math.abs(move.x - mInitialTouch.x);
        if (yDiff > mTouchSlop && yDiff > xDiff && !mIsBeingDragged) {
            if (!canChildScrollToUp()) {
                mIsBeingDragged = true;
                if (DEBUG) PtrCLog.d(LOG_TAG, "ptr is beingDragged");
            }
            // if in refreshing, scroll to up will make PtrFrameLayout handle the touch event
            // and offset content view.
        } else if (yDiff < 0 && Math.abs(yDiff) > mTouchSlop && isRefreshing()) {
            mIsBeingDragged = true;
        }
    }

    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        @SuppressWarnings({"unused"})
        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    class ScrollChecker implements ValueAnimator.AnimatorUpdateListener {

        private ValueAnimator mAnimator = ValueAnimator.ofFloat(0, 1f);
        private boolean mIsRunning = false;
        private int mStart;
        private int mTo;
        private int mDuration;

        public ScrollChecker() {
            mAnimator.addUpdateListener(this);
            mAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        }

        private void reset() {
            mIsRunning = false;
            mAnimator.cancel();
        }

        public void abortIfWorking() {
            if (mIsRunning) {
                onPtrScrollAbort();
                reset();
            }
        }

        public void tryToScrollTo(int to, int duration) {
            if (mPtrIndicator.isAlreadyHere(to) || (to == mTo && mIsRunning)) {
                return;
            }
            mStart = mPtrIndicator.getCurrentPosY();
            mTo = to;
            mDuration = duration;

            if (mAnimator.isRunning()) {
                mAnimator.cancel();
            }

            mAnimator.setDuration(mDuration);

            if (DEBUG) {
                PtrCLog.d(LOG_TAG,
                        "tryToScrollTo: start: %s, distance:%s, to:%s", mStart, to - mStart, to);
            }

            mAnimator.start();
            mIsRunning = true;
        }

        public boolean isRunning() {
            return mIsRunning;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            float fraction = animation.getAnimatedFraction();
            int target = mStart + (int) ((mTo - mStart) * fraction);
            movePos(target - mPtrIndicator.getCurrentPosY());
        }
    }
}
