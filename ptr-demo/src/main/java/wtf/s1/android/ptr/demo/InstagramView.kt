package wtf.s1.android.ptr.demo

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.updatePadding
import wtf.s1.android.ptr.NSPtrEZLayout
import wtf.s1.android.ptr.NSPtrLayout
import wtf.s1.android.ptr.NSPtrListener
import wtf.s1.android.ptr.demo.util.dp

class InstagramView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    init {
        setBackgroundColor(Color.WHITE)
        orientation = VERTICAL
        addView(
            TextView(context).apply {
                setTextColor(Color.BLACK)
                text = "Ins"
                updatePadding(left = 12.dp)
                textSize = 16.dp.toFloat()
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            },
            LayoutParams(LayoutParams.MATCH_PARENT, 60.dp)
        )

        addView(
            NSPtrEZLayout(context).apply {
                addView(
                    SimpleTextListView(context).apply {
                        count = 100
                    },
                    NSPtrLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                )

                addPtrListener(object : NSPtrListener {
                    override fun onRefreshing(ptrLayout: NSPtrLayout) {
                        super.onRefreshing(ptrLayout)
                        ptrLayout.postDelayed({
                            ptrLayout.isRefreshing = false
                        }, 3000)
                    }
                })
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }
}