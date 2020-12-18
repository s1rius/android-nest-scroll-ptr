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

import wtf.s1.android.ptr.PtrClassicFrameLayout
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View

/**
 * Created by s1rius on 15/03/2018.
 */

class SwipeToRefreshLayout : PtrClassicFrameLayout {
    companion object {
        val TAG = "PTR_"
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int) {
        Log.i(TAG, "---> onNestedScroll targetView = ${target::class.java.simpleName} " +
                "dxConsumed = $dxConsumed dyConsumed = $dyConsumed dxUnconsumed = $dxUnconsumed dyUnconsumed = $dyUnconsumed"  )
        super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed)
    }

    override fun startNestedScroll(axes: Int): Boolean {
        val start = super.startNestedScroll(axes)
        Log.i(TAG, "---> startNestedScroll -> ${start}")
        return start
    }

    override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int): Boolean {
        val start = super.onStartNestedScroll(child, target, nestedScrollAxes)
        Log.i(TAG, "---> onStartNestedScroll child = ${child::class.java.simpleName} target = ${child::class.java.simpleName} -> ${start}")
        return start
    }

    override fun onStopNestedScroll(child: View) {
        super.onStopNestedScroll(child)
        Log.i(TAG, "---> onStopNestedScroll")
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        super.onNestedPreScroll(target, dx, dy, consumed)
        Log.i(TAG, "---> onNestedPreScroll target = ${target::class.java.simpleName} dx = $dx dy = $dy consumed = [${consumed[0]},${consumed[1]}]")
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        super.onNestedScrollAccepted(child, target, axes)
        Log.i(TAG, "---> onNestedScrollAccepted child = ${child::class.java.simpleName} target = ${target::class.java.simpleName}")
    }

    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?): Boolean {
        val preScroll = super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
        Log.i(TAG, "---> dispatchNestedPreScroll dx = $dx dy = $dy " +
                "consumed = [${consumed?.get(0)}, ${consumed?.get(1)}] offsetInWindow = [${offsetInWindow?.get(0)}, ${offsetInWindow?.get(1)}]")
        return preScroll
    }
}
