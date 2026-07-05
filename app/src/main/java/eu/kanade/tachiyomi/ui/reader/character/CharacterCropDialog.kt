package eu.kanade.tachiyomi.ui.reader.character

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A full-screen tool for selecting a square (1:1) portrait region from the current page.
 *
 * The whole page is shown fit-to-screen (independent of the reader's own zoom/pan) with a draggable,
 * corner-resizable square overlay that is strictly kept 1:1 and clamped inside the page image. On
 * confirm, the square is reported in **display-bitmap pixel** coordinates; the caller maps that to
 * full-resolution source pixels and crops there (so the portrait is crisp, not an upscaled screen
 * grab). Corner handles resize proportionally; the body drags to reposition; there are no edge
 * handles, so a non-square selection is impossible by construction.
 */
@Composable
fun CharacterCropDialog(
    target: CharacterCropTarget,
    onDismissRequest: () -> Unit,
    onConfirm: (leftBmpPx: Int, topBmpPx: Int, sideBmpPx: Int) -> Unit,
) {
    val image = remember(target) { target.displayBitmap.asImageBitmap() }
    val density = LocalDensity.current
    val handleRadiusPx = with(density) { 14.dp.toPx() }
    // Radial grab zone around each corner. Kept comfortably below half the min square size so the
    // centre of even the smallest square stays a "move" zone (corner-to-centre = size/√2 > grab).
    val cornerGrabPx = with(density) { 44.dp.toPx() }
    val minSizePx = with(density) { 96.dp.toPx() }
    val topReservePx = with(density) { 64.dp.toPx() }
    val bottomReservePx = with(density) { 104.dp.toPx() }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f)),
        ) {
            val containerW = constraints.maxWidth.toFloat()
            val containerH = constraints.maxHeight.toFloat()
            val bmpW = target.displayBitmap.width.toFloat()
            val bmpH = target.displayBitmap.height.toFloat()

            // Fit the page into the area between the top instruction and the bottom buttons.
            val availW = containerW
            val availH = (containerH - topReservePx - bottomReservePx).coerceAtLeast(1f)
            val displayScale = min(availW / bmpW, availH / bmpH)
            val dispW = bmpW * displayScale
            val dispH = bmpH * displayScale
            val imageLeft = (containerW - dispW) / 2f
            val imageTop = topReservePx + (availH - dispH) / 2f
            val imageRight = imageLeft + dispW
            val imageBottom = imageTop + dispH

            // Square state in absolute screen px (top-left corner + side length).
            var sqLeft by remember(target) { mutableStateOf(Float.NaN) }
            var sqTop by remember(target) { mutableStateOf(Float.NaN) }
            var sqSize by remember(target) { mutableStateOf(Float.NaN) }

            if (sqSize.isNaN()) {
                sqSize = min(dispW, dispH) * 0.6f
                sqLeft = imageLeft + (dispW - sqSize) / 2f
                sqTop = imageTop + (dispH - sqSize) / 2f
            }

            fun clampPosition() {
                sqSize = sqSize.coerceIn(minSizePx, min(dispW, dispH))
                sqLeft = sqLeft.coerceIn(imageLeft, imageRight - sqSize)
                sqTop = sqTop.coerceIn(imageTop, imageBottom - sqSize)
            }

            // Dim mask + square border.
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawImage(
                    image = image,
                    dstOffset = IntOffset(imageLeft.roundToInt(), imageTop.roundToInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(dispW.roundToInt(), dispH.roundToInt()),
                )
                drawSquareMask(
                    imageLeft = imageLeft,
                    imageTop = imageTop,
                    imageRight = imageRight,
                    imageBottom = imageBottom,
                    sqLeft = sqLeft,
                    sqTop = sqTop,
                    sqSize = sqSize,
                    handleRadiusPx = handleRadiusPx,
                )
            }

            // A single gesture layer over the whole page. The corner nearest the touch-down point
            // is resized (1:1 preserved) when the down lands within [cornerGrabPx] of it; otherwise
            // a down inside the square body moves it. One layer (instead of five overlapping ones)
            // makes the corner grab zones generous and finger-friendly on real devices.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(target, dispW, dispH) {
                        var mode = DragMode.NONE
                        detectDragGestures(
                            onDragStart = { start ->
                                mode = pickDragMode(start, sqLeft, sqTop, sqSize, cornerGrabPx)
                            },
                            onDragEnd = { mode = DragMode.NONE },
                            onDragCancel = { mode = DragMode.NONE },
                        ) { change, drag ->
                            if (mode == DragMode.NONE) return@detectDragGestures
                            change.consume()
                            val dx = drag.x
                            val dy = drag.y
                            when (mode) {
                                DragMode.MOVE -> {
                                    sqLeft += dx
                                    sqTop += dy
                                    clampPosition()
                                }
                                DragMode.RESIZE_TL -> {
                                    val brX = sqLeft + sqSize
                                    val brY = sqTop + sqSize
                                    val maxSize = min(brX - imageLeft, brY - imageTop)
                                    sqSize = (sqSize - (dx + dy) / 2f).coerceIn(minSizePx, maxSize)
                                    sqLeft = brX - sqSize
                                    sqTop = brY - sqSize
                                }
                                DragMode.RESIZE_TR -> {
                                    val blY = sqTop + sqSize
                                    val maxSize = min(imageRight - sqLeft, blY - imageTop)
                                    sqSize = (sqSize + (dx - dy) / 2f).coerceIn(minSizePx, maxSize)
                                    sqTop = blY - sqSize
                                }
                                DragMode.RESIZE_BL -> {
                                    val trX = sqLeft + sqSize
                                    val maxSize = min(trX - imageLeft, imageBottom - sqTop)
                                    sqSize = (sqSize + (-dx + dy) / 2f).coerceIn(minSizePx, maxSize)
                                    sqLeft = trX - sqSize
                                }
                                DragMode.RESIZE_BR -> {
                                    val maxSize = min(imageRight - sqLeft, imageBottom - sqTop)
                                    sqSize = (sqSize + (dx + dy) / 2f).coerceIn(minSizePx, maxSize)
                                }
                                DragMode.NONE -> Unit
                            }
                        }
                    },
            )

            // Instruction.
            Text(
                text = stringResource(MR.strings.character_crop_hint),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
            )

            // Cancel / Confirm.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(MR.strings.action_cancel), color = Color.White)
                }
                FilledTonalButton(
                    onClick = {
                        val leftBmp = ((sqLeft - imageLeft) / displayScale).roundToInt()
                        val topBmp = ((sqTop - imageTop) / displayScale).roundToInt()
                        val sideBmp = (sqSize / displayScale).roundToInt()
                        onConfirm(leftBmp, topBmp, sideBmp)
                    },
                ) {
                    Text(stringResource(MR.strings.action_ok))
                }
            }
        }
    }
}

private enum class DragMode { NONE, MOVE, RESIZE_TL, RESIZE_TR, RESIZE_BL, RESIZE_BR }

/**
 * Decide what a drag starting at [start] should do: resize the nearest corner if the down landed
 * within [cornerGrabPx] of one, else move the square if the down was inside its body, else nothing.
 */
private fun pickDragMode(
    start: Offset,
    sqLeft: Float,
    sqTop: Float,
    sqSize: Float,
    cornerGrabPx: Float,
): DragMode {
    val corners = listOf(
        DragMode.RESIZE_TL to Offset(sqLeft, sqTop),
        DragMode.RESIZE_TR to Offset(sqLeft + sqSize, sqTop),
        DragMode.RESIZE_BL to Offset(sqLeft, sqTop + sqSize),
        DragMode.RESIZE_BR to Offset(sqLeft + sqSize, sqTop + sqSize),
    )
    var best = DragMode.NONE
    var bestDist = cornerGrabPx * cornerGrabPx
    for ((mode, c) in corners) {
        val ddx = start.x - c.x
        val ddy = start.y - c.y
        val d = ddx * ddx + ddy * ddy
        if (d <= bestDist) {
            bestDist = d
            best = mode
        }
    }
    if (best != DragMode.NONE) return best
    val insideBody = start.x in sqLeft..(sqLeft + sqSize) && start.y in sqTop..(sqTop + sqSize)
    return if (insideBody) DragMode.MOVE else DragMode.NONE
}

private fun DrawScope.drawSquareMask(
    imageLeft: Float,
    imageTop: Float,
    imageRight: Float,
    imageBottom: Float,
    sqLeft: Float,
    sqTop: Float,
    sqSize: Float,
    handleRadiusPx: Float,
) {
    val dim = Color.Black.copy(alpha = 0.55f)
    val sqRight = sqLeft + sqSize
    val sqBottom = sqTop + sqSize
    // Dim the four regions of the image around the square.
    drawRect(dim, topLeft = Offset(imageLeft, imageTop), size = Size(imageRight - imageLeft, sqTop - imageTop))
    drawRect(dim, topLeft = Offset(imageLeft, sqBottom), size = Size(imageRight - imageLeft, imageBottom - sqBottom))
    drawRect(dim, topLeft = Offset(imageLeft, sqTop), size = Size(sqLeft - imageLeft, sqSize))
    drawRect(dim, topLeft = Offset(sqRight, sqTop), size = Size(imageRight - sqRight, sqSize))
    // Square border.
    drawRect(
        color = Color.White,
        topLeft = Offset(sqLeft, sqTop),
        size = Size(sqSize, sqSize),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
    )
    // Corner handles.
    for (corner in listOf(
        Offset(sqLeft, sqTop),
        Offset(sqRight, sqTop),
        Offset(sqLeft, sqBottom),
        Offset(sqRight, sqBottom),
    )) {
        drawCircle(Color.White, radius = handleRadiusPx, center = corner)
        drawCircle(
            Color.Black.copy(alpha = 0.5f),
            radius = handleRadiusPx,
            center = corner,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
        )
    }
}
