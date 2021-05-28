package wtf.s1.android.ptr

import android.content.Context
import android.util.AttributeSet

open class PtrClassicFrameLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
) : PtrLayout(context, attrs) {

    private var header: PtrClassicDefaultHeader? = PtrClassicDefaultHeader(context)

    init {
        headerView = header
        addPtrListener(header)
    }
}