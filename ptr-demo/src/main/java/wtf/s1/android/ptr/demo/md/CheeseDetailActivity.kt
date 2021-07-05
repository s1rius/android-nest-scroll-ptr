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

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.CollapsingToolbarLayout
import wtf.s1.android.ptr.demo.SwipeToRefreshLayout
import wtf.s1.android.ptr_support_design.R

class CheeseDetailActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val intent = intent
        val cheeseName = intent.getStringExtra(EXTRA_NAME)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar).title = cheeseName
        findViewById<SwipeToRefreshLayout>(R.id.ptr_layout).let {
            it.setPTRListener(object: SwipeToRefreshLayout.OnPtrRefreshListener {
                override fun onRefresh() {
                    it.postDelayed({ it.isRefreshing = false }, 3000)
                }
            })
        }

        loadBackdrop()
    }

    private fun loadBackdrop() {
        findViewById<ImageView>(R.id.backdrop)?.let {
            Glide.with(this).load(Cheeses.randomCheeseDrawable).centerCrop().into(it)
        }
    }

    companion object {
        const val EXTRA_NAME = "cheese_name"
    }
}
