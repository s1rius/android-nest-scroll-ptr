package wtf.s1.ptr.nsptr.view

import wtf.s1.ptr.nsptr.Event

interface NSPtrConfig {

    /**
     * Return the {@link NSPtrLayout} this NSPtrLayout is currently associated with.
     */
    fun requireLayout(): NSPtrLayout

    /**
     * The distance in pixels from the top edge of the NSPtrLayout
     * to the top edge of NSPtrLayout's content View.
     */
    fun contentInitPosition(): Int = 0

    /**
     * Is content View's top at the contentInitPosition()
     */
    fun isContentAtInitPosition(): Boolean = false

    /**
     * At trigger refreshing moment, the distance in pixels from the top edge
     * of the NSPtrLayout to the top edge of NSPtrLayout's content View.
     */
    fun contentRefreshPosition(): Int = Int.MAX_VALUE

    /**
     * Is content View's top at the contentRefreshPosition()
     */
    fun isContentOverRefreshPosition(): Boolean = false

    /**
     * Sets the friction for the drag offset. The greater the friction is, the sooner the
     * offset will grow up. When not set, the friction defaults to 0.56f.
     * @param type @see {@link ViewCompat.NestedScrollType}
     */
    fun pullFriction(type: Int): Float = 0.56f

    /**
     * generate the state-machine event to transition state when touch up,
     * cancel or stop nested-scroll
     * @return the NSPtrLayout.Event
     */
    fun generateTouchReleaseEvent(): Event? {
        return if (isContentOverRefreshPosition()) {
            Event.ReleaseToRefreshing
        } else {
            Event.ReleaseToIdle
        }
    }

}