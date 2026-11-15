package bendev.addblock.feature.querylog.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import bendev.addblock.core.ui.DotLabel
import bendev.addblock.feature.querylog.domain.model.MinuteBucket
import bendev.addblock.ui.theme.AddBlockTheme
import java.text.DateFormat
import java.util.Date

/**
 * Stacked columns per minute: allowed at the base, blocked on top.
 * Tap or drag across it to read a minute's exact numbers.
 */
@Composable
fun ActivityChart(buckets: List<MinuteBucket>, modifier: Modifier = Modifier) {
    val colors = AddBlockTheme.extraColors
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisText = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val measurer = rememberTextMeasurer()
    var selected by remember(buckets.size) { mutableStateOf<Int?>(null) }

    val yMax = niceCeil(buckets.maxOfOrNull { it.total } ?: 0)
    val hasData = buckets.any { it.total > 0 }

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DotLabel("Bloqueadas", colors.blocked)
            DotLabel("Permitidas", colors.allowed)
        }
        // Readout of the selected minute, the touch equivalent of a hover tooltip
        Text(
            text = selected?.let { buckets.getOrNull(it) }?.let { b ->
                "${minuteLabel(b.minute)} · ${b.blocked} bloqueadas · ${b.allowed} permitidas"
            } ?: "Toca o desliza sobre las barras para ver el detalle",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        Box(
            Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(132.dp),
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = "Gráfico de consultas por minuto en los últimos " +
                            "${buckets.size} minutos"
                    }
                    .pointerInput(buckets.size) {
                        detectTapGestures { offset ->
                            val i = indexAt(offset.x, AXIS_GUTTER.toPx(), size.width.toFloat(), buckets.size)
                            selected = if (selected == i) null else i
                        }
                    }
                    .pointerInput(buckets.size) {
                        detectHorizontalDragGestures { change, _ ->
                            selected = indexAt(
                                change.position.x, AXIS_GUTTER.toPx(), size.width.toFloat(), buckets.size,
                            )
                        }
                    },
            ) {
                val gutter = AXIS_GUTTER.toPx()
                val bottomPad = 18.dp.toPx()
                val plotW = size.width - gutter
                val plotH = size.height - bottomPad
                val hairline = 1.dp.toPx()

                // Recessive grid: baseline, middle, top
                for ((fraction, label) in listOf(0f to "0", 0.5f to "${yMax / 2}", 1f to "$yMax")) {
                    val y = plotH - plotH * fraction
                    drawLine(gridColor, Offset(gutter, y), Offset(size.width, y), hairline)
                    val text = measurer.measure(label, axisText)
                    drawText(text, topLeft = Offset(0f, y - text.size.height / 2f))
                }
                // X labels at both ends
                measurer.measure("hace ${buckets.size} min", axisText).let {
                    drawText(it, topLeft = Offset(gutter, size.height - it.size.height))
                }
                measurer.measure("ahora", axisText).let {
                    drawText(it, topLeft = Offset(size.width - it.size.width, size.height - it.size.height))
                }

                if (!hasData || buckets.isEmpty()) return@Canvas
                val slot = plotW / buckets.size
                val gap = 2.dp.toPx()
                val barW = minOf(slot - 3.dp.toPx(), 24.dp.toPx()).coerceAtLeast(2.dp.toPx())
                val radius = 4.dp.toPx()

                buckets.forEachIndexed { i, b ->
                    if (b.total == 0) return@forEachIndexed
                    val dim = selected != null && selected != i
                    val alpha = if (dim) 0.35f else 1f
                    val left = gutter + slot * i + (slot - barW) / 2
                    val allowedH = plotH * b.allowed / yMax
                    val blockedH = plotH * b.blocked / yMax
                    // Allowed segment from the baseline; it gets the rounded end only if it's on top
                    if (b.allowed > 0) {
                        roundedTopBar(
                            colors.allowed.copy(alpha = alpha), left, plotH - allowedH, barW, allowedH,
                            if (b.blocked == 0) radius else 0f,
                        )
                    }
                    if (b.blocked > 0) {
                        val bottom = plotH - allowedH - if (b.allowed > 0) gap else 0f
                        roundedTopBar(
                            colors.blocked.copy(alpha = alpha), left, bottom - blockedH, barW,
                            blockedH, radius,
                        )
                    }
                }
            }
            if (!hasData) {
                Text(
                    "Sin actividad todavía",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

private fun DrawScope.roundedTopBar(
    color: Color,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    radius: Float,
) {
    if (height <= 0f) return
    val r = minOf(radius, width / 2, height)
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = left, top = top, right = left + width, bottom = top + height,
                topLeftCornerRadius = CornerRadius(r),
                topRightCornerRadius = CornerRadius(r),
                bottomRightCornerRadius = CornerRadius.Zero,
                bottomLeftCornerRadius = CornerRadius.Zero,
            ),
        )
    }
    drawPath(path, color)
}

private val AXIS_GUTTER = 28.dp

private fun indexAt(x: Float, gutter: Float, width: Float, count: Int): Int =
    ((x - gutter) / (width - gutter) * count).toInt().coerceIn(0, count - 1)

/** Rounds up to a clean axis maximum: 4, 10, 20, 50, 100… */
private fun niceCeil(value: Int): Int {
    if (value <= 4) return 4
    var step = 1
    while (true) {
        for (m in intArrayOf(1, 2, 5)) {
            if (step * m * 10 >= value) return step * m * 10
        }
        step *= 10
    }
}

private fun minuteLabel(minute: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(minute * 60_000))
