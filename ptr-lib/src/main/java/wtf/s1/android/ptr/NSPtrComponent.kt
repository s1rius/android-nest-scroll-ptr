package wtf.s1.android.ptr

interface NSPtrComponent {

    fun prtMeasure(ptrLayout: NSPtrLayout, widthMeasureSpec: Int, heightMeasureSpec: Int)

    fun ptrLayout(ptrLayout: NSPtrLayout)

    fun ptrOnContentOffsetTopAndBottom(offset: Int)
}