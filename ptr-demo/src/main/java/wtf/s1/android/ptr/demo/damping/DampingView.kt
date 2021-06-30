package wtf.s1.android.ptr.demo.damping

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.MultiTypeAdapter
import com.drakeet.multitype.ViewDelegate
import wtf.s1.android.ptr.NSPtrConfig
import wtf.s1.android.ptr.NSPtrLayout
import wtf.s1.android.ptr.demo.*
import wtf.s1.android.ptr.demo.md.MDPtrView
import wtf.s1.android.ptr.demo.util.dp
import wtf.s1.android.ptr.demo.util.getActivity

class DampingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    companion object {
        val INS = "Instagram"
        val WECHAT = "Wechat"
        val WECHAT_M = "Wechat Moment"
        val NESTEDSCRLL = "NestedScroll Sample"


        val samples = arrayListOf(
            INS,
            WECHAT,
            WECHAT_M,
            NESTEDSCRLL
        )
    }

    init {

        addView(
            SwipeToRefreshLayout(context).apply {
                config = object : NSPtrConfig {
                    override fun getLayout(): NSPtrLayout = this@apply
                    override fun overToRefreshPosition(): Boolean = false
                    override fun refreshPosition(): Int = Int.MAX_VALUE
                    override fun atStartPosition(): Boolean = getLayout().contentTopPosition == 0
                    override fun pullFriction(type: Int): Float =
                        if (type == ViewCompat.TYPE_TOUCH) 0.8f else 2f
                }
                addView(
                    RecyclerView(context).apply {
                        overScrollMode = OVER_SCROLL_NEVER
                        setBackgroundColor(Color.WHITE)
                        layoutManager = LinearLayoutManager(context)
                        adapter = MultiTypeAdapter(samples).apply {
                            register(SampleItemViewDelegate())
                        }
                    },
                    LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT
                    )
                )
            },
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )
    }


    class SampleItemView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
    ) : FrameLayout(context, attrs, defStyleAttr) {

        var textView: TextView
        var item: String? = null

        init {
            addView(
                TextView(context).apply {
                    textView = this
                    gravity = Gravity.CENTER
                    textSize = 20f
                    setTextColor(Color.parseColor("#80000000"))
                },
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

            setOnClickListener {
                it.getActivity()?.let { a->
                    if (a is MainActivity) {
                        val target = when (item) {
                            INS -> {
                                InstagramView(context)
                            }
                            WECHAT -> {
                                WeChatMainView(context)
                            }
                            WECHAT_M -> {
                                WeChatMomentView(context)
                            }
                            NESTEDSCRLL -> {
                                MDPtrView(context)
                            }
                            else -> null
                        }

                        target?.let {v->
                            a.push(v)
                        }
                    }
                }

            }
        }

        fun bind(name: String) {
            item = name
            textView.text = name
        }

    }

    class SampleItemViewDelegate: ViewDelegate<String, SampleItemView>() {

        override fun onBindView(view: SampleItemView, item: String) {
            view.bind(item)
        }

        override fun onCreateView(context: Context): SampleItemView {
            return SampleItemView(context).apply {
                layoutParams = RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT, 80.dp)
            }
        }
    }

}