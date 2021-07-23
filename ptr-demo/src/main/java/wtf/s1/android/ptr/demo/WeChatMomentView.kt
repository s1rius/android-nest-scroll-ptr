package wtf.s1.android.ptr.demo

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.MultiTypeAdapter
import com.drakeet.multitype.ViewDelegate
import wtf.s1.ptr.nsptr.view.NSPtrHeader
import wtf.s1.ptr.nsptr.view.NSPtrLayout
import wtf.s1.ptr.nsptr.view.NSPtrListener
import wtf.s1.android.ptr.demo.util.dp
import wtf.s1.android.ptr.demo.util.screenRectPx
import wtf.s1.android.ptr_support_design.R

class WeChatMomentView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    companion object {

        class Avatar

        val list = arrayListOf<Any>(
            Avatar(),
        ).apply {
            addAll(DotaList)
        }
    }

    var avatarView: View

    init {
        setBackgroundResource(R.color.gray)
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

                        addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
            setImageResource(R.drawable.bg)
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
                parentWidthMeasureSpec,
                (ptrLayout.paddingTop + ptrLayout.paddingBottom
                        + lp.topMargin + lp.bottomMargin),
                lp.width
            )

            measure(childWidthMeasureSpec, childHeightMeasureSpec)
        }

        override fun ptrLayout(ptrLayout: NSPtrLayout) {
            val p = ptrLayout.contentTopPosition
            val lp = layoutParams as NSPtrLayout.LayoutParams
            val left = ptrLayout.paddingLeft + lp.leftMargin
            val top = lp.topMargin + ptrLayout.paddingTop - (measuredHeight / 2) + p
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
                setBackgroundResource(R.color.gray)
            }, LayoutParams(LayoutParams.MATCH_PARENT, 26.dp).apply {
                gravity = Gravity.BOTTOM
            })
            addView(AppCompatImageView(context).apply {
                setImageResource(R.drawable.pudge)
            }, LayoutParams(80.dp, 80.dp).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                setMargins(0, 0, 40, 0)
            })
        }
    }

    class MomentHeaderDelegate : ViewDelegate<Avatar, MomentHeader>() {
        override fun onBindView(view: MomentHeader, item: Avatar) {}

        override fun onCreateView(context: Context): MomentHeader {
            return MomentHeader(context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    screenRectPx.width() / 2
                )
            }
        }
    }

    class MomentBody @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
    ) : FrameLayout(context, attrs) {

        var imageView: ImageView
        var nameView: TextView
        var picView: ImageView
        var textView: TextView

        init {
            setPadding(12.dp, 8.dp, 22.dp, 8.dp)
            addView(AppCompatImageView(context).apply {
                imageView = this
            }, LayoutParams(50.dp, 50.dp))

            addView(TextView(context).apply {
                nameView = this
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTextColor(Color.WHITE)
            }, LayoutParams(LayoutParams.MATCH_PARENT, 30.dp).apply {
                setMargins(58.dp, 8.dp, 0, 0)
            })

            addView(TextView(context).apply {
                textView = this
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(Color.WHITE)
            }, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(58.dp, 46.dp, 0, 0)
            })

            addView(AppCompatImageView(context).apply {
                picView = this
                setImageResource(R.mipmap.ic_launcher)
            }, LayoutParams(100.dp, 100.dp).apply {
                setMargins(58.dp, 34.dp, 0, 0)
            })
        }

        fun bind(post: Post) {
            nameView.text = resources.getString(post.name)
            imageView.setImageResource(post.avatar)
            textView.setText(post.text)
            picView.visibility = View.GONE
        }

    }

    class MomentBodyDelegate : ViewDelegate<Post, MomentBody>() {
        override fun onBindView(view: MomentBody, item: Post) {
            view.bind(item)
        }

        override fun onCreateView(context: Context): MomentBody {
            return MomentBody(context).apply {
                layoutParams =
                    RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            }
        }

    }


}