package wtf.s1.android.ptr

import android.content.Context
import android.util.AttributeSet

open class NSPtrClassicLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
) : NSPtrLayout(context, attrs) {

    private var header: NSPtrClassicHeader? = NSPtrClassicHeader(context)

    init {
        headerView = header
        addPtrListener(header!!)
    }
}