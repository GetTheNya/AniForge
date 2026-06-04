package moe.GetTheNya.AniForge.ui.utils

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.disableSplitTouch(): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(pass = PointerEventPass.Initial)

        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)

            if (event.changes.size > 1) {
                event.changes.forEach { change -> change.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}