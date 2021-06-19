package wtf.s1.android.ptr

class NSPtrConfig(val layout: NSPtrLayout) {

    fun startPosition(): Int {
        return 0
    }

    fun atStartPosition(): Boolean {
        return layout.contentTopPosition == startPosition()
    }

    fun overToRefreshPosition(): Boolean {
        return layout.contentTopPosition > layout.headerView?.height ?: 0
    }

    fun refreshPosition(): Int {
        return layout.headerView?.height ?: 0
    }

}