package wtf.s1.android.ptr

interface NSPtrListener {
    /**
     * prepare for loading
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
     *
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

    fun onEvent(event: NSPtrLayout.Event) {}

    /**
     *
     */
    fun onPositionChange(frame: NSPtrLayout, offset: Int) {}
}