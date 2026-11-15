package bendev.addblock.feature.querylog.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import bendev.addblock.core.ui.DotLabel
import bendev.addblock.core.ui.formatted
import bendev.addblock.feature.querylog.domain.model.DomainCount
import bendev.addblock.ui.theme.AddBlockTheme

/**
 * Part-to-whole as one horizontal stacked bar (blocked | allowed), separated by
 * a 2dp surface gap, with a legend that also carries the exact numbers.
 */
@Composable
fun ShareBar(blocked: Int, allowed: Int, modifier: Modifier = Modifier) {
    val colors = AddBlockTheme.extraColors
    val total = blocked + allowed
    val target = if (total == 0) 0f else blocked.toFloat() / total
    val fraction by animateFloatAsState(target, tween(700), label = "share")
    val blockedPct = (target * 100).toInt()

    Column(
        modifier.semantics {
            contentDescription = "$blockedPct% de las consultas fueron bloqueadas"
        },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(12.dp),
        ) {
            if (total == 0) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            } else {
                if (fraction > 0f) Segment(colors.blocked, fraction, start = true, end = fraction >= 1f)
                if (fraction > 0f && fraction < 1f) Spacer(Modifier.width(2.dp))
                if (fraction < 1f) Segment(colors.allowed, 1f - fraction, start = fraction <= 0f, end = true)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DotLabel("Bloqueadas ${blocked.formatted()} · $blockedPct%", colors.blocked)
            DotLabel(
                "Permitidas ${allowed.formatted()} · ${if (total == 0) 0 else 100 - blockedPct}%",
                colors.allowed,
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Segment(
    color: Color,
    weight: Float,
    start: Boolean,
    end: Boolean,
) {
    val r = 6.dp
    Box(
        Modifier
            .weight(weight)
            .height(12.dp)
            .clip(
                RoundedCornerShape(
                    topStart = if (start) r else 2.dp,
                    bottomStart = if (start) r else 2.dp,
                    topEnd = if (end) r else 2.dp,
                    bottomEnd = if (end) r else 2.dp,
                ),
            )
            .background(color),
    )
}

/**
 * Ranking of domains as horizontal bars, sorted, one series so no legend:
 * the card title names it. The value sits at the bar's tip.
 */
@Composable
fun DomainRanking(
    items: List<DomainCount>,
    color: Color,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        Text(
            emptyText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 8.dp),
        )
        return
    }
    val max = items.maxOf { it.count }.coerceAtLeast(1)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            val target = item.count.toFloat() / max
            val fraction by animateFloatAsState(target, tween(600), label = item.domain)
            Column {
                Text(
                    item.domain,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                // Bar, then its value right at the tip, then the leftover space
                val barWeight = fraction.coerceIn(0.02f, 1f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .weight(barWeight)
                            .height(8.dp)
                            .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                            .background(color),
                    )
                    Text(
                        item.count.formatted(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                    if (barWeight < 1f) Spacer(Modifier.weight(1f - barWeight))
                }
            }
        }
    }
}
