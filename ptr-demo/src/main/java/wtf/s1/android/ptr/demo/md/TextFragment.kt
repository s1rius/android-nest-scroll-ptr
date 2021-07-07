package wtf.s1.android.ptr.demo.md

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import wtf.s1.android.ptr.demo.SwipeToRefreshLayout
import wtf.s1.android.ptr_support_design.R
import android.text.method.ScrollingMovementMethod




class TextFragment @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    init {
        LayoutInflater.from(context).inflate(R.layout.fragment_text, this, true)
        findViewById<SwipeToRefreshLayout>(R.id.ptr_layout)?.let {
            it.setPTRListener(object: SwipeToRefreshLayout.OnPtrRefreshListener {
                override fun onRefresh() {
                    it.postDelayed({
                        it.isRefreshing = false
                    }, 3000)
                }
            })
        }
        findViewById<TextView>(R.id.wiki).movementMethod = ScrollingMovementMethod()
    }
}
