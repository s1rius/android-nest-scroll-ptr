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
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import wtf.s1.android.ptr.demo.SwipeToRefreshLayout
import wtf.s1.android.ptr_support_design.R
import java.util.*

class CheeseRecyclerViewFragment @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    lateinit var mRefreshLayout: SwipeToRefreshLayout

    init {
        LayoutInflater.from(context).inflate(
            R.layout.fragment_cheese_recycler, this, true)
        mRefreshLayout = findViewById(R.id.ptr_layout)
        mRefreshLayout.setPTRListener(object: SwipeToRefreshLayout.OnPtrRefreshListener {
            override fun onRefresh() {
                mRefreshLayout.postDelayed({ mRefreshLayout.isRefreshing = false }, 3000)
            }
        })
        val rv = findViewById<RecyclerView>(R.id.recyclerview)
        setupRecyclerView(rv)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mRefreshLayout.isRefreshing = true
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
        recyclerView.adapter = SimpleStringRecyclerViewAdapter(context,
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

}
