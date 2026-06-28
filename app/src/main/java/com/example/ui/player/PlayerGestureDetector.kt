package com.example.ui.player

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

enum class DragDirection {
    VERTICAL, HORIZONTAL
}

@Composable
fun PlayerGestureDetector(
    modifier: Modifier = Modifier,
    onSingleTap: () -> Unit,
    onDoubleTap: (xPercent: Float) -> Unit,
    onBrightnessChange: (Float) -> Unit, // percentage change (-1f to 1f)
    onVolumeChange: (Float) -> Unit, // percentage change (-1f to 1f)
    onSeekDrag: (Float) -> Unit, // drag distance fraction
    onDragEnd: () -> Unit,
    content: @Composable () -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var dragStartPos by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    var dragDirection by remember { mutableStateOf<DragDirection?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { offset ->
                        val percent = offset.x / size.width
                        onDoubleTap(percent)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStartPos = offset
                        dragDirection = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val startPos = dragStartPos ?: return@detectDragGestures
                        val screenWidth = size.width
                        val screenHeight = size.height
                        
                        if (screenWidth == 0 || screenHeight == 0) return@detectDragGestures

                        // Lock direction if not set yet
                        if (dragDirection == null) {
                            dragDirection = if (kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y)) {
                                DragDirection.HORIZONTAL
                            } else {
                                DragDirection.VERTICAL
                            }
                        }

                        when (dragDirection) {
                            DragDirection.HORIZONTAL -> {
                                // Horizontal swipe -> seek
                                // Scale the seek sensitivity by screen width
                                val fraction = dragAmount.x / screenWidth
                                onSeekDrag(fraction)
                            }
                            DragDirection.VERTICAL -> {
                                // Vertical swipe -> Brightness (Left half) vs Volume (Right half)
                                val isLeftHalf = startPos.x < screenWidth / 2f
                                val fraction = -dragAmount.y / screenHeight // negative because swipe UP is positive adjustment

                                if (isLeftHalf) {
                                    onBrightnessChange(fraction)
                                } else {
                                    onVolumeChange(fraction)
                                }
                            }
                            null -> {}
                        }
                    },
                    onDragEnd = {
                        dragStartPos = null
                        dragDirection = null
                        onDragEnd()
                    },
                    onDragCancel = {
                        dragStartPos = null
                        dragDirection = null
                        onDragEnd()
                    }
                )
            }
    ) {
        content()
    }
}
