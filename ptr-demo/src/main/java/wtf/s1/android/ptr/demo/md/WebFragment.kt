package wtf.s1.android.ptr.demo.md

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.webkit.WebView
import android.widget.FrameLayout
import wtf.s1.android.ptr.demo.SwipeToRefreshLayout
import wtf.s1.android.ptr_support_design.R

class WebFragment @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    init {
        LayoutInflater.from(context).inflate(R.layout.fragment_web, this, true)
        findViewById<WebView>(R.id.webview).loadUrl("https://m.bilibili.com")
        findViewById<SwipeToRefreshLayout>(R.id.ptr_layout)?.let {
            it.setPTRListener(object: SwipeToRefreshLayout.OnPtrRefreshListener {
                override fun onRefresh() {
                    it.postDelayed({
                        it.isRefreshing = false
                    }, 3000)
                }
            })
        }
    }
}