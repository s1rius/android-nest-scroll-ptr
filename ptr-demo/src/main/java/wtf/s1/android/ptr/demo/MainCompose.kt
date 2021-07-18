package wtf.s1.android.ptr.demo

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import wtf.s1.android.ptr.demo.damping.DampingCompose
import wtf.s1.android.ptr.demo.util.getActivity

class MainCompose @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AbstractComposeView(context, attrs) {

    init {
        setOnClickListener {
            getActivity()?.let {
                if (it is MainActivity) {
                    it.push( InsCompose(context))
                }
            }
        }
    }

    @Composable
    override fun Content() {
        DampingCompose()
    }
}