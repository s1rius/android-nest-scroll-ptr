package wtf.s1.android.ptr.demo.md

import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.auptr.ptr_support_design.R

class SimpleStringRecyclerViewAdapter(context: Context, values: List<String>) : RecyclerView.Adapter<SimpleStringRecyclerViewAdapter.ViewHolder>() {

    private val mTypedValue = TypedValue()
    private val mBackground: Int
    private val mValues = arrayListOf<String>()

    class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        var mBoundString: String? = null
        val mImageView: ImageView = mView.findViewById(R.id.avatar)
        val mTextView: TextView = mView.findViewById(android.R.id.text1)

        override fun toString(): String {
            return super.toString() + " '" + mTextView.text
        }
    }

    init {
        context.theme.resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true)
        mBackground = mTypedValue.resourceId
        mValues.addAll(values)
    }

    fun bind(data: List<String>) {
        mValues.clear()
        mValues.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item, parent, false)
        view.setBackgroundResource(mBackground)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mBoundString = mValues[position]
        holder.mTextView.text = mValues[position]

        holder.mView.setOnClickListener { v ->
            val context = v.context
            val intent = Intent(context, CheeseDetailActivity::class.java)
            intent.putExtra(CheeseDetailActivity.EXTRA_NAME, holder.mBoundString)

            context.startActivity(intent)
        }

        Glide.with(holder.mImageView.context)
                .load(Cheeses.randomCheeseDrawable)
                .fitCenter()
                .into(holder.mImageView)
    }

    override fun getItemCount(): Int {
        return mValues.size
    }
}