package wtf.s1.android.ptr.demo

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.updatePadding
import wtf.s1.android.ptr.NSPtrConfig
import wtf.s1.android.ptr.NSPtrLayout
import wtf.s1.android.ptr.demo.util.dp

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
                                text = "WeChat"
                                updatePadding(left = 12.dp)
                                textSize = 12.dp.toFloat()
                                gravity = Gravity.CENTER
                            },
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                50.dp
                            )
                        )
                        addView(
                            SimpleTextListView(context).apply {
                                count = 100
                            },
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

                    override fun refreshPosition(): Int {
                        return requireLayout().height - 80.dp
                    }

                    override fun overToRefreshPosition(): Boolean {
                        return requireLayout().contentTopPosition > 20.dp
                    }

                    override fun generateTouchReleaseEvent(): NSPtrLayout.Event? {
                        if (requireLayout().stateMachine.state == NSPtrLayout.State.REFRESHING) {
                            return NSPtrLayout.Event.ReleaseToIdle
                        }
                        return super.generateTouchReleaseEvent()
                    }

                }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }
}