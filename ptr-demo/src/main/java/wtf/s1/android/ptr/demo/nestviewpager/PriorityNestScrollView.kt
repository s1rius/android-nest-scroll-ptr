package wtf.s1.android.ptr.demo.nestviewpager

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.widget.NestedScrollView

class PriorityNestScrollView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    companion object {
        const val TAG = "PriorityNestScrollView"
        const val DEBUG = true
    }


    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        super.onNestedPreScroll(target, dx, dy, consumed)

    }

    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?, type: Int): Boolean {
        if (dy > 0) {
            val oldScrollY = scrollY
            scrollBy(0, dy)
            val myConsumed = scrollY - oldScrollY

            if (consumed != null && myConsumed != 0) {
                consumed[1] += dy
                super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
                consumed[1] = dy
                return true
            }
            if (DEBUG) {
                Log.i(TAG, "oldScrollY = $oldScrollY dy = $dy pre scroll $myConsumed")
            }
        }
        return super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
    }
}