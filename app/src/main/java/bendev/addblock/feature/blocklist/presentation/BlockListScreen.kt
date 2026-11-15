package bendev.addblock.feature.blocklist.presentation

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Source
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bendev.addblock.core.di.appViewModel
import bendev.addblock.core.ui.CardShape
import bendev.addblock.core.ui.IconBadge
import bendev.addblock.core.ui.ScreenHeader
import bendev.addblock.core.ui.SectionCard
import bendev.addblock.core.ui.asDateTime
import bendev.addblock.core.ui.formatted
import bendev.addblock.feature.blocklist.domain.model.BlockListInfo
import bendev.addblock.feature.blocklist.domain.model.HostSource
import bendev.addblock.ui.theme.AddBlockTheme

@Composable
fun BlockListScreen(
    modifier: Modifier = Modifier,
    viewModel: BlockListViewModel = appViewModel {
        BlockListViewModel(observeBlockList, loadBlockList, updateBlockLists, toggleHostSource)
    },
) {
    val info by viewModel.info.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Listas",
                subtitle = "De dónde salen los dominios bloqueados",
                icon = Icons.Rounded.Layers,
            )
        }
        item { SummaryCard(info, onUpdate = viewModel::update) }
        item {
            AnimatedVisibility(visible = message != null) {
                Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        message.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Row(
                Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Source,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Fuentes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    "${info.sources.count { it.enabled }} de ${info.sources.size} activas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(info.sources, key = { it.id }) { source ->
            SourceRow(
                source = source,
                enabled = !info.isUpdating,
                onToggle = { viewModel.toggle(source.id, it) },
            )
        }
    }
}

@Composable
private fun SummaryCard(info: BlockListInfo, onUpdate: () -> Unit) {
    val colors = AddBlockTheme.extraColors
    val count by animateIntAsState(info.domainCount, tween(800), label = "count")
    Box(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Brush.linearGradient(listOf(colors.brandStart, colors.brandEnd))),
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Security,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Dominios bloqueados",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
            Text(count.formatted(), style = MaterialTheme.typography.displayMedium, color = Color.White)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (info.lastUpdated == 0L) "Aún no se han descargado las listas"
                    else "Actualizado el ${info.lastUpdated.asDateTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onUpdate,
                enabled = !info.isUpdating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = colors.brandStart,
                    disabledContainerColor = Color.White.copy(alpha = 0.7f),
                    disabledContentColor = colors.brandStart,
                ),
            ) {
                RefreshIcon(spinning = info.isUpdating)
                Spacer(Modifier.width(8.dp))
                Text(if (info.isUpdating) "Descargando…" else "Actualizar listas")
            }
        }
    }
}

@Composable
private fun RefreshIcon(spinning: Boolean) {
    val angle = if (spinning) {
        val transition = rememberInfiniteTransition(label = "refresh")
        val value by transition.animateFloat(
            0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "angle",
        )
        value
    } else {
        0f
    }
    Icon(
        Icons.Rounded.Refresh,
        contentDescription = null,
        modifier = Modifier
            .size(18.dp)
            .rotate(angle),
    )
}

@Composable
private fun SourceRow(source: HostSource, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    SectionCard {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                sourceIcon(source.id),
                if (source.enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(source.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    Uri.parse(source.url).host ?: source.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(
                checked = source.enabled,
                enabled = enabled,
                onCheckedChange = onToggle,
                thumbContent = {
                    Icon(
                        if (source.enabled) Icons.Rounded.Check else Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                },
            )
        }
    }
}

private fun sourceIcon(id: String): ImageVector = when (id) {
    "adaway" -> Icons.Rounded.Block
    "stevenblack" -> Icons.Rounded.Public
    "pgl" -> Icons.Rounded.FilterAlt
    else -> Icons.Rounded.Language
}
