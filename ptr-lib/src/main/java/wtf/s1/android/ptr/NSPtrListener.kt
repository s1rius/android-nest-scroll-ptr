package wtf.s1.android.ptr

/**
 *
 */
interface NSPtrListener {

    /**
     * prepare for loading
     *
     * @param frame
     */
    fun onDrag(frame: NSPtrLayout) {}

    /**
     * perform refreshing UI
     */
    fun onRefreshing(frame: NSPtrLayout) {}

    /**
     * perform UI after refresh
     */
    fun onComplete(frame: NSPtrLayout) {}

    fun onPositionChange(frame: NSPtrLayout) {}
}