package wtf.s1.android.ptr

import android.content.Context
import android.util.AttributeSet

open class NSPtrEZLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
) : NSPtrLayout(context, attrs) {

    init {
        NSPtrEZHeader(context).apply { headerView = this }
    }
}