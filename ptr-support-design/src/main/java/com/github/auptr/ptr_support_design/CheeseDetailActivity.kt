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

package com.github.auptr.ptr_support_design

import wtf.s1.android.ptr.PtrFrameLayout
import wtf.s1.android.ptr.PtrSimpleListener
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_detail.*

class CheeseDetailActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val intent = intent
        val cheeseName = intent.getStringExtra(EXTRA_NAME)

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        collapsing_toolbar.title = cheeseName
        ptr_layout.addPtrListener(object : PtrSimpleListener() {
            override fun onBegin(frame: PtrFrameLayout) {
                ptr_layout.postDelayed({ ptr_layout.refreshComplete() }, 3000)
            }
        })

        loadBackdrop()
    }

    private fun loadBackdrop() {
        Glide.with(this).load(Cheeses.randomCheeseDrawable).centerCrop().into(backdrop)
    }

    companion object {
        const val EXTRA_NAME = "cheese_name"
    }
}