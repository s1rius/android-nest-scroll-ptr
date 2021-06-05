package wtf.s1.android.ptr

/**
 *
 */
interface PtrListener {

    /**
     * prepare for loading
     *
     * @param frame
     */
    fun onDrag(frame: PtrLayout) {}

    /**
     * perform refreshing UI
     */
    fun onRefreshing(frame: PtrLayout) {}

    /**
     * perform UI after refresh
     */
    fun onComplete(frame: PtrLayout) {}

    fun onPositionChange(frame: PtrLayout) {}
}