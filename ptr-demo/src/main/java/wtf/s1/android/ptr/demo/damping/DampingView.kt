package wtf.s1.android.ptr.demo.damping

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import wtf.s1.android.ptr.NSPtrConfig
import wtf.s1.android.ptr.NSPtrLayout
import wtf.s1.android.ptr.demo.SwipeToRefreshLayout
import wtf.s1.android.ptr.demo.SimpleTextListView

class DampingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    init {
        addView(SwipeToRefreshLayout(context).apply {
            config = object : NSPtrConfig {
                override fun getLayout(): NSPtrLayout = this@apply
                override fun overToRefreshPosition(): Boolean = false
                override fun refreshPosition(): Int = Int.MAX_VALUE
                override fun atStartPosition(): Boolean = getLayout().contentTopPosition == 0
                override fun pullFriction(type: Int): Float = if (type == ViewCompat.TYPE_TOUCH) 0.8f else 2f
            }
            addView(SimpleTextListView(context).apply {
                setBackgroundColor(Color.WHITE)
            }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }


}