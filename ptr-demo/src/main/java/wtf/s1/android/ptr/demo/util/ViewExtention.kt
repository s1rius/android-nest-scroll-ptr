package wtf.s1.android.ptr.demo.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.View
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
    return (parent as? View)?.getActivity()
}