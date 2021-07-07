package wtf.s1.android.ptr.demo

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.drakeet.multitype.ViewDelegate
import wtf.s1.android.ptr.demo.md.TiDetailActivity
import wtf.s1.android.ptr_support_design.R

class SimpleItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val mImageView: ImageView by lazy { findViewById(R.id.avatar) }
    private val mTextView: TextView by lazy { findViewById(android.R.id.text1) }

    init {
        inflate(context, R.layout.list_item, this)
        setOnClickListener { v ->
            val context = v.context
            val intent = Intent(context, TiDetailActivity::class.java)

            context.startActivity(intent)
        }
    }

    fun bind(post: Post) {
        mTextView.text = resources.getString(post.name)
        Glide.with(context)
            .load(post.avatar)
            .fitCenter()
            .into(mImageView)
    }
}

class SimpleItemDelegate : ViewDelegate<Post, SimpleItemView>() {
    override fun onBindView(view: SimpleItemView, item: Post) {
        view.bind(item)
    }

    override fun onCreateView(context: Context): SimpleItemView {
        return SimpleItemView(context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

}