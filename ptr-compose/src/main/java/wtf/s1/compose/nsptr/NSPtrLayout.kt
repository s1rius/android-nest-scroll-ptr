package wtf.s1.compose.nsptr

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.*

@Stable
sealed class RefreshState {
    // init state
    // 初始化状态
    object IDLE : RefreshState()
    // refreshing state
    // 刷新状态
    object REFRESHING : RefreshState()
    // drag state
    // 拖动状态
    object DRAG : RefreshState()
}

@Stable
class NSPtrState(pullFriction: Float, refreshState: RefreshState = RefreshState.IDLE) {

    var rememberState: RefreshState by mutableStateOf(refreshState)

    var contentInitPosition: Int by mutableStateOf(0)

}

private class NSPtrNestedScrollConnection(): NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        return super.onPreScroll(available, source)
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        return super.onPostScroll(consumed, available, source)
    }
}

@Composable
fun NSPtrLayout(nsPtrState: NSPtrState = NSPtrState(1.0f),
                modifier: Modifier,
                header: (@Composable () -> Unit)? = null,
                content: @Composable () -> Unit) {


    val nestedScrollConnection = remember() {
        NSPtrNestedScrollConnection()
    }

    Layout(content, modifier.nestedScroll(nestedScrollConnection),
        { measurables, constraints ->
            val placeables = measurables.map { measurable ->
                measurable.measure(constraints)
            }

            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEach {placeable ->
                    placeable.place(0, 0)
                }
            }
        }
    )
}