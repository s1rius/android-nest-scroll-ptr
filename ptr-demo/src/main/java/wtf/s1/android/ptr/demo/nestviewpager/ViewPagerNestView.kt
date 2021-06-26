package wtf.s1.android.ptr.demo.nestviewpager

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.github.auptr.ptr_support_design.R
import com.google.android.material.tabs.TabLayout
import wtf.s1.android.ptr.demo.SimpleTextListView
import wtf.s1.android.ptr.demo.SwipeToRefreshLayout

class ViewPagerNestView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var bannerView: FrameLayout? = null
    var tabView: TabLayout? = null
    var viewpager: ViewPager2? = null
    var ptrView: SwipeToRefreshLayout? = null
    var scrollView: NestedScrollView? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.viewpager_nest, this, true)

        scrollView = findViewById(R.id.scrollView)

        ptrView = findViewById(R.id.ptr)
        ptrView?.setPTRListener(object : SwipeToRefreshLayout.OnPtrRefreshListener {
            override fun onRefresh() {
                requestLayout()
                ptrView?.postDelayed({ ptrView?.isRefreshing = false }, 2000L)
            }
        })

        bannerView = findViewById(R.id.banner)

        tabView = findViewById(R.id.tab)

        viewpager = findViewById(R.id.viewpager)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            viewpager?.isNestedScrollingEnabled = false
        }

        viewpager?.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(SimpleTextListView(context).apply {
                    layoutParams =
                        LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                }) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val simpleView = holder.itemView
                if (simpleView is SimpleTextListView) {
                    simpleView.count = position * 10 + 1
                }
            }

            override fun getItemCount(): Int = 10
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val p = viewpager?.layoutParams ?: LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0)
        if (p.height != h) {
            p.height = h
            viewpager?.layoutParams = p
            requestLayout()
        }
    }
}