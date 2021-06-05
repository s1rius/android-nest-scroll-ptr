package wtf.s1.android.ptr

interface PtrComponent {

    fun prtMeasure(ptrLayout: PtrLayout, widthMeasureSpec: Int, heightMeasureSpec: Int)

    fun ptrLayout(ptrLayout: PtrLayout)

    fun ptrOnContentOffsetTopAndBottom(offset: Int)
}