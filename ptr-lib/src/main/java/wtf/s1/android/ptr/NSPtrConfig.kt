package wtf.s1.android.ptr

interface NSPtrConfig {

    fun getLayout(): NSPtrLayout

    fun initPosition(): Int = 0

    fun atInitPosition(): Boolean = false

    fun startCrossRefreshLine(): Boolean = false

    fun startRefreshPosition(): Int = Int.MAX_VALUE

    fun endCrossRefreshLine(): Boolean = false

    fun endRefreshPosition(): Int = Int.MIN_VALUE

    fun pullFriction(type: Int): Float = 0.56f

    fun maxOffset(): Int = Int.MAX_VALUE

}