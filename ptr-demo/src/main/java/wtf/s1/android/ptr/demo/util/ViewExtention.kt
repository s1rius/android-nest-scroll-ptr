package wtf.s1.android.ptr.demo.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import kotlin.math.roundToInt

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

val Float.dp: Float                 // [xxhdpi](360 -> 1080)
    get() = android.util.TypedValue.applyDimension(
        android.util.TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics)

val Int.dp: Int
    get() = android.util.TypedValue.applyDimension(
        android.util.TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics).toInt()


private val displayMetrics: DisplayMetrics by lazy { Resources.getSystem().displayMetrics }

/**
 * Returns boundary of the screen in pixels (px).
 */
val screenRectPx: Rect
    get() = displayMetrics.run { Rect(0, 0, widthPixels, heightPixels) }

/**
 * Returns boundary of the screen in density independent pixels (dp).
 */
val screenRectDp: RectF
    get() = screenRectPx.run { RectF(0f, 0f, right.dp.toFloat(), bottom.dp.toFloat()) }

/**
 * Returns boundary of the physical screen including system decor elements (if any) like navigation
 * bar in pixels (px).
 */
val Context.physicalScreenRectPx: Rect
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        (applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .run { DisplayMetrics().also { defaultDisplay.getRealMetrics(it) } }
            .run { Rect(0, 0, widthPixels, heightPixels) }
    } else screenRectPx

/**
 * Returns boundary of the physical screen including system decor elements (if any) like navigation
 * bar in density independent pixels (dp).
 */
val Context.physicalScreenRectDp: RectF
    get() = physicalScreenRectPx.run { RectF(0f, 0f, right.dp.toFloat(), bottom.dp.toFloat()) }