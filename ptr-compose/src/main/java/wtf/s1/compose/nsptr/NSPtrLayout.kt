package wtf.s1.compose.nsptr

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.*

@Composable
fun NSPtrLayout(modifier: Modifier,
                content: @Composable () -> Unit) {

    Layout(content, modifier,
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