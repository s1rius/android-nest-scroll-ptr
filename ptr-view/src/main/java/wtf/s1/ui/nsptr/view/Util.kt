package wtf.s1.ui.nsptr.view

import android.content.res.Resources

fun toDp(dp: Int, res: Resources): Int{
    return android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), res.displayMetrics).toInt()
}
