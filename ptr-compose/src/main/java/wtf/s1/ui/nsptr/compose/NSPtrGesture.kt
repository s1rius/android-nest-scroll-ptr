package wtf.s1.ui.nsptr.compose

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.util.fastAll

suspend fun PointerInputScope.detectDownAndUp(
    onDown: (Offset) -> Unit,
    onUpOrCancel: (Offset?) -> Unit
) {
    forEachGesture {
        awaitPointerEventScope {
            awaitFirstDown(false).also {
                onDown(it.position)
            }

            val up = waitForUpOrCancel()
            onUpOrCancel(up?.position)
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

        if (event.changes.fastAll { it.isOutOfBounds(size) }) {
            return null // Canceled
        }

        val final = awaitPointerEvent(PointerEventPass.Final)
        if (final.changes.fastAll { !it.pressed }) {
            return final.changes[0]
        }
    }
}