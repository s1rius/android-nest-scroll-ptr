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

package wtf.s1.android.ptr.demo

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import wtf.s1.android.ptr.demo.damping.DampingView
import wtf.s1.android.ptr_support_design.R

class MainActivity : AppCompatActivity() {

    lateinit var contentContainer: FrameLayout

    val components = ArrayDeque<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<FrameLayout>(R.id.container).apply {
            contentContainer = this
        }

        push(DampingView(this))
        onBackPressedDispatcher.addCallback(backCallBack)
    }

    private val backCallBack = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            pop()
            if (components.isEmpty()) {
                finish()
            }
        }
    }

    fun push(view: View) {
        components.addFirst(view)
        contentContainer.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
    }

    fun pop(view: View? = null) {
        view?:components.firstOrNull()?.let {
            components.remove(it)
            contentContainer.removeView(it)
        }
    }
}
