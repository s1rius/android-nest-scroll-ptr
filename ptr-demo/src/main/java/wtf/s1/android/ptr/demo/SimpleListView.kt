package wtf.s1.android.ptr.demo

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.NestedScrollingChild3
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.MultiTypeAdapter
import wtf.s1.pudge.hugo2.DebugLog

@DebugLog
class SimpleListView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
):
    RecyclerView(context, attrs, defStyleAttr),
    NestedScrollingChild3
{
    init {
        overScrollMode = OVER_SCROLL_NEVER
    }


    var currentAdapter: MultiTypeAdapter? = null

    init {
        layoutManager = LinearLayoutManager(context)

        currentAdapter = MultiTypeAdapter(arrayListOf<Post>()).apply {
            register(SimpleItemDelegate())
            items = DotaList
        }
        adapter = currentAdapter
    }


    override fun setNestedScrollingEnabled(enabled: Boolean) {
        super.setNestedScrollingEnabled(enabled)
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return super.isNestedScrollingEnabled()
    }

    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        return super.startNestedScroll(axes, type)
    }

    override fun stopNestedScroll(type: Int) {
        super.stopNestedScroll(type)
    }

    override fun hasNestedScrollingParent(type: Int): Boolean {
        return super.hasNestedScrollingParent(type)
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
    }

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return super.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return super.dispatchNestedPreFling(velocityX, velocityY)
    }

}