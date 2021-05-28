package wtf.s1.android.ptr.demo.nestviewpager

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SimpleTextListView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {


    var count: Int = 100
    set(value) {
        field = value
        currentAdapter?.notifyDataSetChanged()
        parent?.requestLayout()
    }


    var currentAdapter: Adapter<ViewHolder>? = null

    init {


        //isNestedScrollingEnabled = false
        layoutManager = LinearLayoutManager(context)

        currentAdapter = object: Adapter<ViewHolder>() {

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                return object: ViewHolder(TextView(context).apply {
                    gravity = Gravity.CENTER
                    minHeight = 60
                    setTextColor(Color.WHITE)
                }) {
                }
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                val text = holder.itemView
                if (text is TextView) {
                    text.text = "$position"
                }
            }

            override fun getItemCount(): Int = count

            override fun onViewAttachedToWindow(holder: ViewHolder) {
                super.onViewAttachedToWindow(holder)
                Log.i("simple text", "onViewAttachedToWindow ${holder.adapterPosition}")
            }

            override fun onViewDetachedFromWindow(holder: ViewHolder) {
                super.onViewDetachedFromWindow(holder)
                Log.i("simple text", "onViewDetachedFromWindow ${holder.adapterPosition}")
            }
        }
        adapter = currentAdapter

        addOnScrollListener(object: OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                Log.i("simple text", "onScrolled $dx $dy")
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                Log.i("simple text", "onScrollStateChanged $newState")
            }
        })
    }
}