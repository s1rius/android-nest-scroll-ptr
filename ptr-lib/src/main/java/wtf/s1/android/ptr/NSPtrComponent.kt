package wtf.s1.android.ptr

interface NSPtrComponent: NSPtrListener {

    fun prtMeasure(ptrLayout: NSPtrLayout, widthMeasureSpec: Int, heightMeasureSpec: Int)

    fun ptrLayout(ptrLayout: NSPtrLayout)

    fun ptrOnContentOffsetTopAndBottom(offset: Int)
}