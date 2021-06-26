package wtf.s1.android.ptr.demo.nestviewpager

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView

class PriorityNestScrollView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    override fun dispatchNestedPreScroll(dx: Int,
                                         dy: Int,
                                         consumed: IntArray?,
                                         offsetInWindow: IntArray?,
                                         type: Int): Boolean {
        val superHandler = super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
        val unconsumedDy = dy - (consumed?.getOrElse(1) {0}?:0)
        if (!superHandler && unconsumedDy > 0) {
            val oldScrollY = scrollY
            scrollBy(0, unconsumedDy)
            val myConsumed = scrollY - oldScrollY

            if (consumed != null && myConsumed != 0) {
                consumed[1] = myConsumed
                return true
            }
        }
        return superHandler
    }
}