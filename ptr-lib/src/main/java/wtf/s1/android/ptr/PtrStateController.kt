package wtf.s1.android.ptr

open class PtrStateController {

    /**
     * the Content View init position in pull orientation
     */
    var startPosition = 0

    /**
     * the Content View reach the position when refreshing
     */
    var mOffsetToRefresh = 0

    /**
     * Content View current position
     */
    var currentPos = 0
    set(value) {
        lastPos = currentPos
        field = value
    }

    /**
     * Content View last position
     */
    var lastPos = 0

    private var mHeaderHeight = 0

    var ratioOfHeaderToHeightRefresh = 1.2f

    var resistance = 1.7f

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

    var headerHeight: Int
        get() = mHeaderHeight
        set(height) {
            mHeaderHeight = height
            updateHeight()
        }

    private fun updateHeight() {
        mOffsetToRefresh = (ratioOfHeaderToHeightRefresh * mHeaderHeight).toInt()
    }

    fun convertFrom(ptrSlider: PtrStateController) {
        currentPos = ptrSlider.currentPos
        lastPos = ptrSlider.lastPos
        mHeaderHeight = ptrSlider.mHeaderHeight
    }

    fun hasLeftStartPosition(): Boolean {
        return currentPos > startPosition
    }

    val isOverOffsetToRefresh: Boolean
        get() = currentPos >= offsetToRefresh

    val isInStartPosition: Boolean
        get() = currentPos == startPosition

    fun isAlreadyHere(to: Int): Boolean {
        return currentPos == to
    }

    fun willOverTop(to: Int): Boolean {
        return to < startPosition
    }
}