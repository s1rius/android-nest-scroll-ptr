package wtf.s1.android.ptr.demo

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import wtf.s1.compose.nsptr.NSPtrLayout

class InsCompose @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AbstractComposeView(context, attrs) {

    init {
        setBackgroundColor(Color.WHITE)
    }

    @Composable
    override fun Content() {
        Ins()
    }
}


@Composable
fun Ins() {
    
    Row(modifier = Modifier.fillMaxSize()) {
        NSPtrLayout(modifier = Modifier.fillMaxSize()) {
            SimpleListCompose()
        }
    }
}