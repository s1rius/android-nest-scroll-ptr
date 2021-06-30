package wtf.s1.android.ptr.demo

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.NestedScrollingChild3
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import wtf.s1.android.ptr.demo.md.Cheeses
import wtf.s1.android.ptr.demo.md.SimpleStringRecyclerViewAdapter
import wtf.s1.pudge.hugo2.DebugLog
import java.util.*

@DebugLog
class SimpleTextListView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
):
    RecyclerView(context, attrs, defStyleAttr),
    NestedScrollingChild3
{

    companion object {
        private fun getRandomSublist(array: Array<String>, amount: Int): List<String> {
            val list = ArrayList<String>(amount)
            val random = Random()
            while (list.size < amount) {
                list.add(array[random.nextInt(array.size)])
            }
            return list
        }

        fun getRandomSubList(count: Int): List<String> {
            return getRandomSublist(Cheeses.sCheeseStrings, count)
        }
    }

    init {
        overScrollMode = OVER_SCROLL_NEVER
    }

    var count: Int = 100
        set(value) {
            field = value
            currentAdapter?.bind(getRandomSubList(count))
        }


    var currentAdapter: SimpleStringRecyclerViewAdapter? = null

    init {
        layoutManager = LinearLayoutManager(context)

        currentAdapter = SimpleStringRecyclerViewAdapter(context, arrayListOf())
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