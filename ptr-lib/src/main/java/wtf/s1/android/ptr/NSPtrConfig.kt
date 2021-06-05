package wtf.s1.android.ptr

interface NSPtrConfig {

    fun startPosition(layout: NSPtrLayout): Int {
        return 0
    }

    fun atStartPosition(layout: NSPtrLayout): Boolean {
        return layout.contentTopPosition == startPosition(layout)
    }

    fun overToRefreshPosition(layout: NSPtrLayout): Boolean {
        return layout.contentTopPosition > layout.headerView?.height ?: 0
    }

    fun refreshPosition(layout: NSPtrLayout): Int {
        return layout.headerView?.height ?: 0
    }

}