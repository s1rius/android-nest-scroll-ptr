package wtf.s1.android.ptr;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
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

import wtf.s1.android.ptr.indicator.PtrStateController;

/**
 * This layout view for "Pull to Refresh(Ptr)" support all of the view,
 * you can contain everything you want.
 * support: pull to refresh / release to refresh / auto refresh / keep header view
 * while refreshing / hide header view while refreshing
 * It defines {@link PtrListener}, which allows you customize the UI easily.
 */
public class PtrLayout extends ViewGroup implements NestedScrollingParent, NestedScrollingChild {

    // status enum
    public final static int PTR_STATUS_INIT = 1;
    private static final int INVALID_POINTER = -1;
    private float mPullFriction = 0.56f;

    @IntDef({PTR_STATUS_INIT,
            PTR_STATUS_PREPARE,
            PTR_STATUS_LOADING,
            PTR_STATUS_COMPLETE})
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
    protected View mContent;
    // optional config for define header and content in xml file
    private int mHeaderId = 0;
    private int mContainerId = 0;
    // ptr config
    private int mLoadingMinTime = 500;
    private long mLoadingStartTime = 0;
    private boolean inAutoRefresh = false;
    private int mDurationToLoadingPosition = 200;
    private int mDurationToCloseHeader = 200;
    private boolean mKeepHeaderWhenRefresh = true;
    private boolean mPullToRefresh = false;
    private View mHeaderView;
    private final PtrListenerHolder mPtrListenerHolder = PtrListenerHolder.create();
    // working parameters
    private final ScrollChecker mScrollChecker;

    private int mHeaderHeight;

    private PtrStateController mPtrStateController;

    //touch handle
    private int mActivePointerId = INVALID_POINTER;
    private boolean mIsBeingDragged = false;
    private final PointF mInitialTouch = new PointF(-1, -1);
    private final PointF mLastTouch = new PointF(-1, -1);
    private final int mTouchSlop;
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

    public PtrLayout(Context context) {
        this(context, null);
    }

    public PtrLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PtrLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mPtrStateController = new PtrStateController();

        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.PtrLayout, 0, 0);
        if (arr != null) {

            mHeaderId = arr.getResourceId(R.styleable.PtrLayout_ptr_header, mHeaderId);
            mContainerId = arr.getResourceId(R.styleable.PtrLayout_ptr_content, mContainerId);

            mPtrStateController.setResistance(
                    arr.getFloat(R.styleable.PtrLayout_ptr_resistance, mPtrStateController.getResistance()));

            mDurationToLoadingPosition = arr.getInt(R.styleable.PtrLayout_ptr_duration_to_loading_position, mDurationToLoadingPosition);
            mDurationToCloseHeader = arr.getInt(R.styleable.PtrLayout_ptr_duration_to_close_header, mDurationToCloseHeader);

            float ratio = mPtrStateController.getRatioOfHeaderToHeightRefresh();
            ratio = arr.getFloat(R.styleable.PtrLayout_ptr_ratio_of_header_height_to_refresh, ratio);
            mPtrStateController.setRatioOfHeaderHeightToRefresh(ratio);

            mPullToRefresh = arr.getBoolean(R.styleable.PtrLayout_ptr_pull_to_fresh, mPullToRefresh);
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

        if (mHeaderView != null) {
            measureChildWithMargins(mHeaderView, widthMeasureSpec, 0, heightMeasureSpec, 0);
            mHeaderHeight = mHeaderView.getMeasuredHeight();
            mPtrStateController.setHeaderHeight(mHeaderHeight);
        }

        if (mContent != null) {
            measureContentView(mContent, widthMeasureSpec, heightMeasureSpec);
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
        int offset = mPtrStateController.getCurrentPosY();
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        if (mHeaderView != null) {
            LayoutParams lp = (LayoutParams) mHeaderView.getLayoutParams();
            final int left = paddingLeft + lp.leftMargin;
            // enhance readability(header is layout above screen when first init)
            final int top = -(mHeaderHeight - paddingTop - lp.topMargin - offset);
            final int right = left + mHeaderView.getMeasuredWidth();
            final int bottom = top + mHeaderView.getMeasuredHeight();
            mHeaderView.layout(left, top, right, bottom);
        }
        if (mContent != null) {
            MarginLayoutParams lp = (MarginLayoutParams) mContent.getLayoutParams();
            final int left = paddingLeft + lp.leftMargin;
            final int top = paddingTop + lp.topMargin + offset;
            final int right = left + mContent.getMeasuredWidth();
            final int bottom = top + mContent.getMeasuredHeight();
            mContent.layout(left, top, right, bottom);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
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
                if (mPtrStateController.hasLeftStartPosition()) {
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
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }

                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }

                if (!(mContent instanceof NestedScrollingChild)) {
                    // if content is a scrollable view and not implement NestedScrollingChild
                    // like ListView, we need enable nest scroll in intercept process
                    final int y = (int) (ev.getY(pointerIndex) + 0.5f);

                    int dy = (int) (mLastTouch.y - y);

                    if (mPtrStateController.isInStartPosition() || dy > 0) {
                        if (dispatchNestedPreScroll(0, dy, mScrollConsumed, mScrollOffset)) {
                            mLastTouch.y = y - mScrollOffset[1];
                            // handle touch when parent not accept nest scroll
                            startDragging(new PointF(ev.getX(), ev.getY()));
                            return mIsBeingDragged;
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
        if (!isEnabled()
                // || canChildScrollToUp()
                || mInVerticalNestedScrolling) {
            return false;
        }

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return true;
            case MotionEvent.ACTION_DOWN:
                int pointerIndex = event.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }
                mInitialTouch.set(event.getX(pointerIndex), event.getY(pointerIndex));
                mLastTouch.set(event.getX(pointerIndex), event.getY(pointerIndex));
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);

                if (!canChildScrollToUp()) {
                    return true;
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                pointerIndex = event.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }
                final int y = (int) (event.getY(pointerIndex) + 0.5f);

                int dy = (int) (mLastTouch.y - y);

                if (dy != 0 || mPtrStateController.isInStartPosition()) {
                    if (dispatchNestedPreScroll(0, dy, mScrollConsumed, mScrollOffset)) {
                        mLastTouch.y = y - mScrollOffset[1];
                        // handle touch when parent not accept nest scroll
                        return mScrollConsumed[1] == 0;
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
                boolean canMoveUp = mPtrStateController.hasLeftStartPosition();

                if (mPtrStateController.getCurrentPosY() != 0) {
                    movePos(offsetY);
                    return true;
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
     *
     * @param deltaY the y offset
     */
    private int movePos(float deltaY) {
        // has reached the top
        if ((deltaY < 0 && mPtrStateController.isInStartPosition())) {
            return 0;
        }

        int to = mPtrStateController.getCurrentPosY() + (int) deltaY;

        // over top
        if (mPtrStateController.willOverTop(to)) {
            to = PtrStateController.POS_START;
        }

        mPtrStateController.setCurrentPos(to);
        if (mPtrStateController.isInStartPosition() && isRefreshing()) {
            performRefreshComplete();
        }
        int change = to - mPtrStateController.getLastPosY();
        updatePos(change);
        return change;
    }

    private void updatePos(int change) {
        if (change == 0) {
            return;
        }


        // leave initiated position or just refresh complete
        if (mPtrStateController.hasJustLeftStartPosition() && mStatus == PTR_STATUS_INIT) {
            changeStatusTo(PTR_STATUS_PREPARE);
        }

        // back to initiated position
        if (mPtrStateController.hasJustBackToStartPosition()) {
            tryToNotifyReset();
        }

        // Pull to Refresh
        if (mStatus == PTR_STATUS_PREPARE) {
            // reach fresh height while moving from top to bottom
            if (!isAutoRefresh() && mPullToRefresh
                    && mPtrStateController.crossRefreshLineFromTopToBottom()) {
                tryToPerformRefresh();
            }
        }

        mHeaderView.offsetTopAndBottom(change);
        mContent.offsetTopAndBottom(change);
        invalidate();

        if (mPtrListenerHolder.hasHandler()) {
            mPtrListenerHolder.onPositionChange(this, mStatus, mPtrStateController);
        }
    }

    private int withFriction(float force) {
        return (int) (force * mPullFriction);
    }

    public void changeStatusTo(int status) {
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
                if (mPtrStateController.isOverOffsetToKeepHeaderWhileLoading() && !stayForLoading) {
                    mScrollChecker.tryToScrollTo(
                            mPtrStateController.getOffsetToKeepHeaderWhileLoading(),
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
     * Scroll back to to if is not under touch
     */
    private void tryScrollBackToTop() {
        if (!mIsInTouchProgress) {
            mScrollChecker.tryToScrollTo(PtrStateController.POS_START, mDurationToCloseHeader);
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

    private void tryToPerformRefresh() {
        if (mStatus != PTR_STATUS_PREPARE) {
            return;
        }

        if ((mPtrStateController.isOverOffsetToKeepHeaderWhileLoading() && isAutoRefresh())
                || mPtrStateController.isOverOffsetToRefresh()) {
            changeStatusTo(PTR_STATUS_LOADING);
        }
    }

    private void performRefresh() {
        mLoadingStartTime = System.currentTimeMillis();
        if (mPtrListenerHolder.hasHandler()) {
            mPtrListenerHolder.onBegin(this);
        }
    }

    /**
     * If at the top and not in loading, reset
     */
    private void tryToNotifyReset() {
        if ((mStatus == PTR_STATUS_COMPLETE || mStatus == PTR_STATUS_PREPARE)
                && mPtrStateController.isInStartPosition()) {
            changeStatusTo(PTR_STATUS_INIT);
        }
    }

    protected void onPtrScrollAbort() {
        if (mPtrStateController.hasLeftStartPosition() && isAutoRefresh()) {
            onRelease(true);
        }
    }

    @SuppressWarnings("unused")
    protected void onPtrScrollFinish() {
        if (mPtrStateController.hasLeftStartPosition() && isAutoRefresh()) {
            onRelease(true);
        }
    }

    /**
     * Detect whether is refreshing.
     *
     * @return isRefreshing
     */
    public boolean isRefreshing() {
        return mStatus == PTR_STATUS_LOADING;
    }

    /**
     * Call this when data is loaded.
     * The UI will perform complete at once or after a delay, depends on the time elapsed is greater then {@link #mLoadingMinTime} or not.
     */
    final public void refreshComplete() {

        int delay = (int) (mLoadingMinTime - (System.currentTimeMillis() - mLoadingStartTime));
        if (delay <= 0) {
            performRefreshComplete();
        } else {
            postDelayed(mPerformRefreshCompleteDelay, delay);
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
     * @param ignoreHook is ignore hook
     */
    private void notifyUIRefreshComplete(boolean ignoreHook) {
        if (mPtrListenerHolder.hasHandler()) {
            mPtrListenerHolder.onComplete(this);
        }
        mPtrStateController.onUIRefreshComplete();
        tryScrollBackToTopAfterComplete();
        tryToNotifyReset();
    }

    @SuppressWarnings("unused")
    public void autoRefresh() {
        autoRefresh(mDurationToCloseHeader);
    }

    private void clearFlag() {
        inAutoRefresh = false;
    }

    public void autoRefresh(int duration) {

        if (mStatus != PTR_STATUS_INIT) {
            return;
        }

        inAutoRefresh = true;
        changeStatusTo(PTR_STATUS_PREPARE);
        mScrollChecker.tryToScrollTo(mPtrStateController.getOffsetToRefresh(), duration);
        changeStatusTo(PTR_STATUS_LOADING);
    }

    public boolean isAutoRefresh() {
        return inAutoRefresh;
    }

    /**
     * loading will last at least for so long
     *
     * @param time the loading min duration
     */
    @SuppressWarnings("unused")
    public void setLoadingMinTime(int time) {
        mLoadingMinTime = time;
    }

    @SuppressWarnings("unused")
    public View getContentView() {
        return mContent;
    }

    public void addPtrListener(PtrListener ptrListener) {
        mPtrListenerHolder.addListener(ptrListener);
    }

    @SuppressWarnings("unused")
    public void removePtrListener(PtrListener ptrListener) {
        mPtrListenerHolder.removeListener(ptrListener);
    }

    @SuppressWarnings("unused")
    public void setPtrIndicator(PtrStateController indicator) {
        if (mPtrStateController != null && mPtrStateController != indicator) {
            indicator.convertFrom(mPtrStateController);
        }
        mPtrStateController = indicator;
    }

    @SuppressWarnings("unused")
    public float getResistance() {
        return mPtrStateController.getResistance();
    }

    @SuppressWarnings("unused")
    public void setResistance(float resistance) {
        mPtrStateController.setResistance(resistance);
    }

    /**
     * The duration to return back to the loading position
     *
     * @param duration to loading position duration
     */
    @SuppressWarnings("unused")
    public void setDurationToLoadingPosition(int duration) {
        mDurationToLoadingPosition = duration;
    }

    @SuppressWarnings("unused")
    public long getDurationToCloseHeader() {
        return mDurationToCloseHeader;
    }

    /**
     * The duration to close time
     *
     * @param duration to close duration
     */
    @SuppressWarnings("unused")
    public void setDurationToCloseHeader(int duration) {
        mDurationToCloseHeader = duration;
    }

    @SuppressWarnings("unused")
    public void setRatioOfHeaderHeightToRefresh(float ratio) {
        mPtrStateController.setRatioOfHeaderHeightToRefresh(ratio);
    }

    public int getOffsetToRefresh() {
        return mPtrStateController.getOffsetToRefresh();
    }

    @SuppressWarnings("unused")
    public float getRatioOfHeaderToHeightRefresh() {
        return mPtrStateController.getRatioOfHeaderToHeightRefresh();
    }

    @SuppressWarnings("unused")
    public int getOffsetToKeepHeaderWhileLoading() {
        return mPtrStateController.getOffsetToKeepHeaderWhileLoading();
    }

    @SuppressWarnings("unused")
    public void setOffsetToRefresh(int offset) {
        mPtrStateController.setOffsetToRefresh(offset);
    }

    @SuppressWarnings("unused")
    public void setOffsetToKeepHeaderWhileLoading(int offset) {
        mPtrStateController.setOffsetToKeepHeaderWhileLoading(offset);
    }

    @SuppressWarnings("unused")
    public boolean isKeepHeaderWhenRefresh() {
        return mKeepHeaderWhenRefresh;
    }

    @SuppressWarnings("unused")
    public void setKeepHeaderWhenRefresh(boolean keepOrNot) {
        mKeepHeaderWhenRefresh = keepOrNot;
    }

    public boolean isPullToRefresh() {
        return mPullToRefresh;
    }

    public void setPullToRefresh(boolean pullToRefresh) {
        mPullToRefresh = pullToRefresh;
    }

    @SuppressWarnings("unused")
    public View getHeaderView() {
        return mHeaderView;
    }

    public void setHeaderView(View header) {
        if (header == null) {
            return;
        }
        if (mHeaderView != null && mHeaderView != header) {
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
        return p instanceof LayoutParams;
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
    public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY, boolean consumed) {
        // Re-dispatch up the tree by default
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
        // Re-dispatch up the tree by default
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    // nested scroll parent
    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes) {
        return isEnabled()
                && !isAutoRefresh()
                // && !isRefreshing()
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
        if (dy > 0 && !mPtrStateController.isInStartPosition()) {
            consumed[1] = -movePos(-dy);
        }

        if (dy > 0 && mTotalUnconsumed > 0 && isRefreshing()) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = dy - mTotalUnconsumed;
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
    public void onStopNestedScroll(@NonNull View child) {
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
        boolean canChildScrollUp;
        if (mContent instanceof ListView) {
            canChildScrollUp = ListViewCompat.canScrollList((ListView) mContent, -1);
        } else {
            canChildScrollUp = mContent.canScrollVertically(-1);
        }
        return canChildScrollUp;
    }

    private void startDragging(PointF move) {
        final float yDiff = move.y - mInitialTouch.y;
        final float xDiff = Math.abs(move.x - mInitialTouch.x);
        if (yDiff > mTouchSlop && yDiff > xDiff && !mIsBeingDragged) {
            if (!canChildScrollToUp()) {
                mIsBeingDragged = true;
            }
            // if in refreshing, scroll to up will make PtrFrameLayout handle the touch event
            // and offset content view.
        } else if (yDiff < 0 && Math.abs(yDiff) > mTouchSlop && isRefreshing()) {
            mIsBeingDragged = true;
        }
    }

    public static class LayoutParams extends MarginLayoutParams {

        private LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        private LayoutParams(int width, int height) {
            super(width, height);
        }

        @SuppressWarnings("unused")
        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        private LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    class ScrollChecker implements ValueAnimator.AnimatorUpdateListener {

        private ValueAnimator mAnimator = ValueAnimator.ofFloat(0, 1f);
        private boolean mIsRunning = false;
        private int mStart;
        private int mTo;

        private ScrollChecker() {
            mAnimator.addUpdateListener(this);
            mAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        }

        private void reset() {
            mIsRunning = false;
            mAnimator.cancel();
        }

        private void abortIfWorking() {
            if (mIsRunning) {
                onPtrScrollAbort();
                reset();
            }
        }

        private void tryToScrollTo(int to, int duration) {
            if (mPtrStateController.isAlreadyHere(to) || (to == mTo && mIsRunning)) {
                return;
            }
            mStart = mPtrStateController.getCurrentPosY();
            mTo = to;

            if (mAnimator.isRunning()) {
                mAnimator.cancel();
            }

            mAnimator.setDuration(duration);

            mAnimator.start();
            mIsRunning = true;
        }

        private boolean isRunning() {
            return mIsRunning;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            float fraction = animation.getAnimatedFraction();
            int target = mStart + (int) ((mTo - mStart) * fraction);
            movePos(target - mPtrStateController.getCurrentPosY());
        }
    }
}
