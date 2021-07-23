package wtf.s1.android.ptr.demo

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.updatePadding
import wtf.s1.ptr.nsptr.Event
import wtf.s1.ptr.nsptr.view.NSPtrConfig
import wtf.s1.ptr.nsptr.view.NSPtrLayout
import wtf.s1.ptr.nsptr.State
import wtf.s1.android.ptr.demo.util.dp
import wtf.s1.android.ptr_support_design.R

class WeChatMainView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    init {
        setBackgroundColor(Color.WHITE)
        addView(
            NSPtrLayout(context).apply {
                addView(
                    LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(
                            TextView(context).apply {
                                setTextColor(Color.BLACK)
                                text = context.getString(R.string.wechat)
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                                updatePadding(left = 12.dp)
                                gravity = Gravity.CENTER
                            },
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                50.dp
                            )
                        )
                        addView(
                            SimpleListView(context),
                            LinearLayout.LayoutParams(
                                LayoutParams.MATCH_PARENT,
                                LayoutParams.MATCH_PARENT
                            )
                        )
                    },
                    NSPtrLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                )

                config = object: NSPtrConfig {

                    override fun requireLayout(): NSPtrLayout {
                        return this@apply
                    }

                    override fun contentRefreshPosition(): Int {
                        return requireLayout().height - 80.dp
                    }

                    override fun isContentOverRefreshPosition(): Boolean {
                        return requireLayout().contentTopPosition > 20.dp
                    }

                    override fun generateTouchReleaseEvent(): Event? {
                        if (requireLayout().stateMachine.state == State.REFRESHING) {
                            return Event.ReleaseToIdle
                        }
                        return super.generateTouchReleaseEvent()
                    }

                }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }
}