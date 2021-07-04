package wtf.s1.android.ptr

interface NSPtrConfig {

    fun requireLayout(): NSPtrLayout

    fun startPosition(): Int = 0

    fun atStartPosition(): Boolean = false

    fun overToRefreshPosition(): Boolean = false

    fun refreshPosition(): Int = Int.MAX_VALUE

    fun pullFriction(type: Int): Float = 0.56f

    fun generateTouchReleaseEvent(): NSPtrLayout.Event? {
        return when (requireLayout().stateMachine.state) {
            is NSPtrLayout.State.DRAG -> {
                if (overToRefreshPosition()) {
                    NSPtrLayout.Event.ReleaseToRefreshing
                } else {
                    NSPtrLayout.Event.ReleaseToIdle
                }
            }
            is NSPtrLayout.State.REFRESHING -> {
                if (!overToRefreshPosition()) {
                    NSPtrLayout.Event.ReleaseToIdle
                } else {
                    null
                }
            }
            else -> null
        }
    }

}