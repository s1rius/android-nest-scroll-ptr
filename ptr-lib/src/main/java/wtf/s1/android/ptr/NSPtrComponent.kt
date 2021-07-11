package wtf.s1.android.ptr

/**
 * if NSPtrLayout child want hold the measure and layout, will implement this interface
 */
interface NSPtrComponent {

    /**
     * NSPtrLayout will call this function in it's onMeasure() function.
     * dispatch measure event to this method, we need call child measure() function
     */
    fun prtMeasure(ptrLayout: NSPtrLayout,
                   parentWidthMeasureSpec: Int,
                   parentHeightMeasureSpec: Int){}

    /**
     * NSPtrLayout will call this function in it's onLayout() function.
     * implement this function and layout this view
     */
    fun ptrLayout(ptrLayout: NSPtrLayout){}
}

/**
 * Header View need implement this interface,
 * There is one-to-one correspondence between NSPtrLayout and NSPtrHeader
 * or implement NSPtrListener
 * @see {@link NSPtrListener}
 */
interface NSPtrHeader: NSPtrComponent

interface NSPtrFooter: NSPtrComponent