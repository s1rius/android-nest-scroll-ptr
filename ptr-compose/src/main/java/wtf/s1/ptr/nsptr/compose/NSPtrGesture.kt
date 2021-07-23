package wtf.s1.ptr.nsptr.compose

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import kotlinx.coroutines.coroutineScope


suspend fun PointerInputScope.detectDownAndUp(
    onDown: (Offset) -> Unit,
    onUpOrCancel: (Offset?) -> Unit
) {
    forEachGesture {
        coroutineScope {
            awaitPointerEventScope {
                awaitFirstDown(false).also {
                    onDown(it.position)
                }

                val up = waitForUpOrCancel()
                onUpOrCancel.invoke(up?.position)
            }
        }
    }
}

suspend fun AwaitPointerEventScope.waitForUpOrCancel(): PointerInputChange? {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Main)
        if (event.changes.fastAll { it.changedToUpIgnoreConsumed() }) {
            // All pointers are up
            return event.changes[0]
        }

        if (event.changes.fastAny { it.isOutOfBounds(size) }) {
            return null // Canceled
        }

        val final = awaitPointerEvent(PointerEventPass.Final)
        if (final.changes.fastAny { !it.pressed }) {
            return final.changes[0]
        }
    }
}