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

import android.content.Context
import android.util.AttributeSet
import android.view.View
import wtf.s1.ptr.nsptr.view.NSPtrEZLayout
import wtf.s1.ptr.nsptr.view.NSPtrLayout
import wtf.s1.ptr.nsptr.view.NSPtrListener
import wtf.s1.pudge.hugo2.DebugLog

/**
 * Created by s1rius on 15/03/2018.
 */
@DebugLog
class SwipeToRefreshLayout : NSPtrEZLayout {

    private var listener: OnPtrRefreshListener? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    companion object {
        val TAG = "PTR_"
    }

    init {
        this.addPtrListener(object: NSPtrListener {

            override fun onDrag(frame: NSPtrLayout) {}

            override fun onRefreshing(frame: NSPtrLayout) {
                listener?.onRefresh()
            }

            override fun onComplete(frame: NSPtrLayout) {}

            override fun onPositionChange(frame: NSPtrLayout, offset: Int) {}
        })
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
    }

    override fun internalOnNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray?
    ) {
        super.internalOnNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            type,
            consumed
        )
    }

    override fun onNestedFling(
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return super.onNestedFling(target, velocityX, velocityY, consumed)
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        return super.onNestedPreFling(target, velocityX, velocityY)
    }

    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int) {
        super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed)
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return super.startNestedScroll(axes)
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int): Boolean {
        return super.onStartNestedScroll(child, target, axes)
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        super.onStopNestedScroll(target, type)
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        super.onNestedPreScroll(target, dx, dy, consumed)
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        super.onNestedScrollAccepted(child, target, axes)
    }

    fun setPTRListener(listener: OnPtrRefreshListener) {
        this.listener = listener
    }

    interface OnPtrRefreshListener {
        fun onRefresh()
    }
}
