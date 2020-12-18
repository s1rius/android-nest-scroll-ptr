/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wtf.s1.android.ptr.demo.md

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.bumptech.glide.Glide

import java.util.ArrayList
import java.util.Random

import wtf.s1.android.ptr.PtrLayout
import wtf.s1.android.ptr.PtrSimpleListener
import android.widget.BaseAdapter
import android.widget.ListView
import com.github.auptr.ptr_support_design.R
import wtf.s1.android.ptr.demo.SwipeToRefreshLayout

class CheeseListViewFragment : Fragment() {

    lateinit var mRefreshLayout: SwipeToRefreshLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(
                R.layout.fragment_cheese_list, container, false)
        mRefreshLayout = view.findViewById(R.id.ptr_layout)
        mRefreshLayout.addPtrListener(object : PtrSimpleListener() {

            override fun onBegin(frame: PtrLayout) {
                mRefreshLayout.postDelayed({ mRefreshLayout.refreshComplete() }, 3000)
            }
        })
        val listview = view.findViewById<ListView>(R.id.recyclerview)
        setupListView(listview)
        return view
    }

    private fun setupListView(listView: ListView) {
        listView.adapter = SimpleStringRecyclerViewAdapter(activity!!,
                getRandomSublist(Cheeses.sCheeseStrings, 30))
    }

    private fun getRandomSublist(array: Array<String>, amount: Int): List<String> {
        val list = ArrayList<String>(amount)
        val random = Random()
        while (list.size < amount) {
            list.add(array[random.nextInt(array.size)])
        }
        return list
    }

    class SimpleStringRecyclerViewAdapter(context: Context, private val mValues: List<String>) : BaseAdapter() {

        private val mTypedValue = TypedValue()
        private val mBackground: Int

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
        }

        private fun onBindViewHolder(holder: ViewHolder, position: Int) {
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

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            var tag: ViewHolder
            var tempView: View
            if (convertView == null) {
                tempView = LayoutInflater.from(parent?.context)
                        .inflate(R.layout.list_item, parent, false)

                tag = ViewHolder(tempView)
                tempView.tag = tag
                onBindViewHolder(tag, position)
                return tempView
            } else {
                tag = convertView.tag as ViewHolder
            }

            onBindViewHolder(tag, position)
            return convertView
        }

        override fun getItem(position: Int): Any {
            return mValues[position]
        }

        override fun getItemId(position: Int): Long {
            return mValues[position].hashCode().toLong()
        }

        override fun getCount(): Int {
            return mValues.size
        }
    }
}
