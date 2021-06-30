package wtf.s1.android.ptr.demo

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.MultiTypeAdapter
import com.drakeet.multitype.ViewDelegate
import wtf.s1.android.ptr.NSPtrHeader
import wtf.s1.android.ptr.NSPtrLayout
import wtf.s1.android.ptr.NSPtrListener
import wtf.s1.android.ptr.demo.util.dp
import wtf.s1.android.ptr_support_design.R

class WeChatMomentView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    companion object {

        class Avatar
        class Moment

        val list = arrayListOf(
            Avatar(),
            Moment(),
            Moment(),
            Moment(),
            Moment(),
            Moment(),
            Moment(),
        )
    }

    lateinit var avatarView: View

    init {
        setBackgroundColor(Color.WHITE)
        addView(
            NSPtrLayout(context).apply {
                addView(
                    MomentPic(context).apply {
                        avatarView = this
                    },
                    LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT
                    )
                )

                addView(
                    RecyclerView(context).apply {
                        overScrollMode = OVER_SCROLL_NEVER
                        layoutManager = LinearLayoutManager(context)
                        adapter = MultiTypeAdapter(list).apply {
                            register(MomentHeaderDelegate())
                            register(MomentBodyDelegate())
                        }

                        addOnScrollListener(object: RecyclerView.OnScrollListener() {
                            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                                super.onScrolled(recyclerView, dx, dy)
                                avatarView.offsetTopAndBottom(-dy)
                            }

                        })
                    },
                    LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT
                    )
                )
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }


    class MomentPic @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
    ) : androidx.appcompat.widget.AppCompatImageView(context, attrs), NSPtrHeader, NSPtrListener {

        init {
            setImageResource(R.mipmap.ic_launcher)
        }

        override fun prtMeasure(
            ptrLayout: NSPtrLayout,
            parentWidthMeasureSpec: Int,
            parentHeightMeasureSpec: Int
        ) {
            val lp = layoutParams as NSPtrLayout.LayoutParams
            val childWidthMeasureSpec = getChildMeasureSpec(
                parentWidthMeasureSpec,
                ptrLayout.paddingLeft + ptrLayout.paddingRight
                        + lp.leftMargin + lp.rightMargin,
                lp.width
            )
            val childHeightMeasureSpec = getChildMeasureSpec(
                parentHeightMeasureSpec,
                (ptrLayout.paddingTop + ptrLayout.paddingBottom
                        + lp.topMargin + lp.bottomMargin),
                190.dp
            )

            measure(childWidthMeasureSpec, childHeightMeasureSpec)
        }

        override fun ptrLayout(ptrLayout: NSPtrLayout) {
            val p = ptrLayout.contentTopPosition
            val lp = layoutParams as NSPtrLayout.LayoutParams
            val left = ptrLayout.paddingLeft + lp.leftMargin
            val top = lp.topMargin + ptrLayout.paddingTop - 40.dp + p
            val right = left + measuredWidth
            val bottom = top + measuredHeight
            layout(left, top, right, bottom)
        }

        override fun onPositionChange(frame: NSPtrLayout, offset: Int) {
            offsetTopAndBottom(offset)
        }
    }

    class MomentHeader @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
    ) : FrameLayout(context, attrs) {

        init {
            addView(View(context).apply {
                setBackgroundColor(Color.GRAY)
            }, LayoutParams(LayoutParams.MATCH_PARENT, 12.dp).apply {
                gravity = Gravity.BOTTOM
            })
            addView(AppCompatImageView(context).apply {
                setImageResource(R.mipmap.ic_launcher)
            }, LayoutParams(50.dp, 50.dp).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                setMargins(0, 0, 12, 0)
            })
        }
    }

    class MomentHeaderDelegate : ViewDelegate<Avatar, MomentHeader>() {
        override fun onBindView(view: MomentHeader, item: Avatar) {}

        override fun onCreateView(context: Context): MomentHeader {
            return MomentHeader(context).apply {
                layoutParams = RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT, 162.dp)
            }
        }
    }

    class MomentBody @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
    ) : FrameLayout(context, attrs) {

        init {
            setBackgroundColor(Color.GRAY)
            setPadding(8.dp, 8.dp, 8.dp, 8.dp)
            addView(AppCompatImageView(context).apply {
                setImageResource(R.mipmap.ic_launcher)
            }, LayoutParams(30.dp, 30.dp))

            addView(TextView(context).apply {
                text = "Jack"
            }, LayoutParams(LayoutParams.WRAP_CONTENT, 30.dp).apply {
                setMargins(38.dp, 0, 0, 0)
            })

            addView(AppCompatImageView(context).apply {
                setImageResource(R.mipmap.ic_launcher)
            }, LayoutParams(100.dp, 100.dp).apply {
                setMargins(38.dp, 28.dp, 0, 0)
            })
        }

    }

    class MomentBodyDelegate : ViewDelegate<Moment, MomentBody>() {
        override fun onBindView(view: MomentBody, item: Moment) {}

        override fun onCreateView(context: Context): MomentBody {
            return MomentBody(context).apply {
                layoutParams =
                    RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            }
        }

    }


}