package wtf.s1.android.ptr

interface NSPtrComponent {

    fun prtMeasure(ptrLayout: NSPtrLayout,
                   parentWidthMeasureSpec: Int,
                   parentHeightMeasureSpec: Int){}

    fun ptrLayout(ptrLayout: NSPtrLayout){}
}

interface NSPtrHeader: NSPtrComponent

interface NSPtrFooter: NSPtrComponent