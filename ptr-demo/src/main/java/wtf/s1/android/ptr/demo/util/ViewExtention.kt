package wtf.s1.android.ptr.demo.util

import android.animation.Animator
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Point
import android.util.Log
import android.view.View
import androidx.appcompat.widget.ContentFrameLayout
import androidx.fragment.app.FragmentActivity


fun View.getActivity(): FragmentActivity? {

    var context: Context? = context
    while (context is ContextWrapper) {
        if (context is FragmentActivity) {
            return context
        } else if (context is Activity) {
            Log.w("getActivity",
                    "this Activity need be a FragmentActivity ${context.javaClass.simpleName}")
        }
        context = context.baseContext
    }

    /**
     * addView(View(context.applicationContext))
     * 上述情况添加的 View 的 Context 并不是一个 Activity 或者 Activity 的 wrapper，在这种情况下
     * 通过父布局去寻找对应的 Activity
     */
    return (parent as? View)?.getActivity()
}