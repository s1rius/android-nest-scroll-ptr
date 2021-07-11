package wtf.s1.android.ptr

interface NSPtrListener {
    /**
     * perform dragging the content view
     *
     * @param ptrLayout
     */
    fun onDrag(ptrLayout: NSPtrLayout) {}

    /**
     * perform refreshing UI
     */
    fun onRefreshing(ptrLayout: NSPtrLayout) {}

    /**
     * perform UI after refresh
     */
    fun onComplete(ptrLayout: NSPtrLayout) {}

    /**
     * when state transition happen
     */
    fun onTransition(ptrLayout: NSPtrLayout, transition: StateMachine.Transition.Valid<NSPtrLayout.State, NSPtrLayout.Event, NSPtrLayout.SideEffect>) {
        when (transition.toState) {
            is NSPtrLayout.State.IDLE -> {
                if (transition.event == NSPtrLayout.Event.RefreshComplete) {
                    onComplete(ptrLayout)
                }
            }
            is NSPtrLayout.State.REFRESHING -> {
                onRefreshing(ptrLayout)
            }
            is NSPtrLayout.State.DRAG -> {
                onDrag(ptrLayout)
            }
        }
    }

    /**
     * drag or animate the content view and move it
     */
    fun onPositionChange(ptrLayout: NSPtrLayout, offset: Int) {}
}