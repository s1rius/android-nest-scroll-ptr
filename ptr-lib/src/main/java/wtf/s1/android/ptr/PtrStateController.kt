package wtf.s1.android.ptr

class PtrStateController {
    var mOffsetToRefresh = 0
    var currentPosY = 0
    var lastPosY = 0
    private var mHeaderHeight = 0
    var ratioOfHeaderToHeightRefresh = 1.2f
    var resistance = 1.7f
    private var mOffsetToKeepHeaderWhileLoading = -1

    // record the refresh complete position
    private var mRefreshCompleteY = 0
    fun onUIRefreshComplete() {
        mRefreshCompleteY = currentPosY
    }

    fun goDownCrossFinishPosition(): Boolean {
        return currentPosY >= mRefreshCompleteY
    }

    fun setRatioOfHeaderHeightToRefresh(ratio: Float) {
        ratioOfHeaderToHeightRefresh = ratio
        mOffsetToRefresh = (mHeaderHeight * ratio).toInt()
    }

    var offsetToRefresh: Int
        get() = mOffsetToRefresh
        set(offset) {
            ratioOfHeaderToHeightRefresh = mHeaderHeight * 1f / offset
            mOffsetToRefresh = offset
        }

    /**
     * Update current position before update the UI
     */
    fun setCurrentPos(current: Int) {
        lastPosY = currentPosY
        currentPosY = current
    }

    var headerHeight: Int
        get() = mHeaderHeight
        set(height) {
            mHeaderHeight = height
            updateHeight()
        }

    protected fun updateHeight() {
        mOffsetToRefresh = (ratioOfHeaderToHeightRefresh * mHeaderHeight).toInt()
    }

    fun convertFrom(ptrSlider: PtrStateController) {
        currentPosY = ptrSlider.currentPosY
        lastPosY = ptrSlider.lastPosY
        mHeaderHeight = ptrSlider.mHeaderHeight
    }

    fun hasLeftStartPosition(): Boolean {
        return currentPosY > POS_START
    }

    fun hasJustLeftStartPosition(): Boolean {
        return lastPosY == POS_START && hasLeftStartPosition()
    }

    fun hasJustBackToStartPosition(): Boolean {
        return lastPosY != POS_START && isInStartPosition
    }

    val isOverOffsetToRefresh: Boolean
        get() = currentPosY >= offsetToRefresh
    val isInStartPosition: Boolean
        get() = currentPosY == POS_START

    fun crossRefreshLineFromTopToBottom(): Boolean {
        return offsetToRefresh in (lastPosY + 1)..currentPosY
    }

    fun hasJustReachedHeaderHeightFromTopToBottom(): Boolean {
        return mHeaderHeight in (lastPosY + 1)..currentPosY
    }

    val isOverOffsetToKeepHeaderWhileLoading: Boolean
        get() = currentPosY > offsetToKeepHeaderWhileLoading
    var offsetToKeepHeaderWhileLoading: Int
        get() = if (mOffsetToKeepHeaderWhileLoading >= 0) mOffsetToKeepHeaderWhileLoading else mHeaderHeight
        set(offset) {
            mOffsetToKeepHeaderWhileLoading = offset
        }

    fun isAlreadyHere(to: Int): Boolean {
        return currentPosY == to
    }

    fun willOverTop(to: Int): Boolean {
        return to < POS_START
    }

    companion object {
        const val POS_START = 0
    }
}