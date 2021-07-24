package wtf.s1.ptr.nsptr.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import wtf.s1.ptr.nsptr.SideEffect
import wtf.s1.ptr.nsptr.State
import wtf.s1.ptr.nsptr.StateMachine

@Composable
fun NSPtrEZHeader(
    nsPtrState: NSPtrState,
    modifier: Modifier = Modifier,
    radius: Dp = 15.dp,
    strokeWidth: Dp = 3.dp
) {
    val transition = rememberInfiniteTransition()
    val currentRotation by transition.animateValue(
        0,
        290,
        Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 300,
                easing = LinearEasing
            )
        )
    )

    Canvas(modifier = modifier) {
        when (nsPtrState.state) {
            State.REFRESHING -> {
                val current = currentRotation % 360
                drawArc(
                    Color.Gray,
                    current * 1f,
                    sweepAngle = 300f,
                    useCenter = false,
                    size = Size(radius.toPx() * 2, radius.toPx() * 2),
                    style = Stroke(strokeWidth.toPx()),
                    topLeft = Offset(size.width / 2 - radius.toPx(), 0f)
                )
            }
            State.DRAG,
            State.IDLE -> {
                nsPtrState.lastTransition?.let {
                    if (nsPtrState.state == State.DRAG
                        || (it is StateMachine.Transition.Valid
                                && it.sideEffect == SideEffect.OnReleaseToIdle)
                    ) {
                        drawArc(
                            Color.Gray,
                            startAngle = -90f,
                            360f * nsPtrState.pullProgress(),
                            false,
                            size = Size(radius.toPx() * 2, radius.toPx() * 2),
                            style = Stroke(strokeWidth.toPx()),
                            topLeft = Offset(size.width / 2 - radius.toPx(), 0f)
                        )
                    }
                }
            }
        }
    }
}