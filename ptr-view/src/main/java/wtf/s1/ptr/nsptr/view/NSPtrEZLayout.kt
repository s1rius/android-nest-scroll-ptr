package wtf.s1.ptr.nsptr.view

import android.content.Context
import android.util.AttributeSet

/**
 * implement NSPtrEZLayout in easy way
 */
open class NSPtrEZLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
) : NSPtrLayout(context, attrs) {

    init {
        NSPtrEZHeader(context).apply { headerView = this }
    }
}