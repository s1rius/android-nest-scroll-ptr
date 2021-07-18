package wtf.s1.android.ptr.demo

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import wtf.s1.nsptr.Event
import wtf.s1.nsptr.State
import wtf.s1.nsptr.compose.NSPtrEZHeader
import wtf.s1.nsptr.compose.NSPtrLayout
import wtf.s1.nsptr.compose.NSPtrState

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
    Box(modifier = Modifier.fillMaxSize()) {
        val coroutine = rememberCoroutineScope()
        val s = remember {
            NSPtrState(
                coroutineScope = coroutine,
                contentInitPosition = 0.dp,
                contentRefreshPosition = 54.dp
            ) {
                delay(3000)
                it.dispatchPtrEvent(Event.RefreshComplete)
            }
        }
        NSPtrLayout(
            nsPtrState = s,
            modifier = Modifier.fillMaxSize(),
        ) {
            NSPtrEZHeader(
                modifier = Modifier
                    .size(100.dp)
                    .offset(0.dp, 12.dp),
                nsPtrState = s
            )
            SimpleListCompose(Modifier.ptrContent())
        }
    }
}