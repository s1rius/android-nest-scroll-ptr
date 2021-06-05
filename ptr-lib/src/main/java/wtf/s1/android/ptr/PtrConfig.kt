package wtf.s1.android.ptr

interface PtrConfig {

    fun startPosition(layout: PtrLayout): Int {
        return 0
    }

    fun atStartPosition(layout: PtrLayout): Boolean {
        return layout.contentTopPosition == startPosition(layout)
    }

    fun overToRefreshPosition(layout: PtrLayout): Boolean {
        return layout.contentTopPosition > layout.headerView?.height ?: 0
    }

    fun refreshPosition(layout: PtrLayout): Int {
        return layout.headerView?.height ?: 0
    }

}