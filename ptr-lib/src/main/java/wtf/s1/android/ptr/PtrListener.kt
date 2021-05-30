package wtf.s1.android.ptr

/**
 *
 */
interface PtrListener {
    /**
     * When the content view has reached top and refresh has been completed, view will be reset.
     *
     * @param frame
     */
    fun onReset(frame: PtrLayout?) {}

    /**
     * prepare for loading
     *
     * @param frame
     */
    fun onPrepare(frame: PtrLayout?) {}

    /**
     * perform refreshing UI
     */
    fun onBegin(frame: PtrLayout?) {}

    /**
     * perform UI after refresh
     */
    fun onComplete(frame: PtrLayout?) {}

    fun onPositionChange(frame: PtrLayout?, ptrStateController: PtrStateController?) {}
}