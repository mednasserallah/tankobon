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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
    val handleRadiusPx = with(density) { 12.dp.toPx() }
    val handleTouchPx = with(density) { 44.dp.toPx() }
    val minSizePx = with(density) { 64.dp.toPx() }
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

            // Move: drag anywhere inside the square body.
            Box(
                modifier = Modifier
                    .offset { IntOffset(sqLeft.roundToInt(), sqTop.roundToInt()) }
                    .sizePx(sqSize)
                    .pointerInput(target, dispW, dispH) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            sqLeft += drag.x
                            sqTop += drag.y
                            clampPosition()
                        }
                    },
            )

            // Four corner resize handles (proportional — 1:1 preserved).
            CornerHandle(sqLeft, sqTop, handleTouchPx, target, dispW, dispH) { dx, dy ->
                val brX = sqLeft + sqSize
                val brY = sqTop + sqSize
                val maxSize = min(brX - imageLeft, brY - imageTop)
                sqSize = (sqSize - (dx + dy) / 2f).coerceIn(minSizePx, maxSize)
                sqLeft = brX - sqSize
                sqTop = brY - sqSize
            }
            CornerHandle(sqLeft + sqSize, sqTop, handleTouchPx, target, dispW, dispH) { dx, dy ->
                val blY = sqTop + sqSize
                val maxSize = min(imageRight - sqLeft, blY - imageTop)
                sqSize = (sqSize + (dx - dy) / 2f).coerceIn(minSizePx, maxSize)
                sqTop = blY - sqSize
            }
            CornerHandle(sqLeft, sqTop + sqSize, handleTouchPx, target, dispW, dispH) { dx, dy ->
                val trX = sqLeft + sqSize
                val maxSize = min(trX - imageLeft, imageBottom - sqTop)
                sqSize = (sqSize + (-dx + dy) / 2f).coerceIn(minSizePx, maxSize)
                sqLeft = trX - sqSize
            }
            CornerHandle(sqLeft + sqSize, sqTop + sqSize, handleTouchPx, target, dispW, dispH) { dx, dy ->
                val maxSize = min(imageRight - sqLeft, imageBottom - sqTop)
                sqSize = (sqSize + (dx + dy) / 2f).coerceIn(minSizePx, maxSize)
            }

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

/** A transparent, draggable touch target centered on ([centerX], [centerY]); reports drag deltas. */
@Composable
private fun CornerHandle(
    centerX: Float,
    centerY: Float,
    touchSizePx: Float,
    key: Any,
    keyW: Float,
    keyH: Float,
    onDrag: (dx: Float, dy: Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (centerX - touchSizePx / 2f).roundToInt(),
                    (centerY - touchSizePx / 2f).roundToInt(),
                )
            }
            .sizePx(touchSizePx)
            .pointerInput(key, keyW, keyH) {
                detectDragGestures { change, drag ->
                    change.consume()
                    onDrag(drag.x, drag.y)
                }
            },
    )
}

/** px-sized [Modifier.size] helper (the crop math is all in raw pixels). */
@Composable
private fun Modifier.sizePx(px: Float): Modifier {
    val dp = with(LocalDensity.current) { px.toDp() }
    return this.size(dp)
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
