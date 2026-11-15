package bendev.addblock.feature.querylog.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bendev.addblock.core.di.appViewModel
import bendev.addblock.core.ui.CardTitle
import bendev.addblock.core.ui.EmptyState
import bendev.addblock.core.ui.IconBadge
import bendev.addblock.core.ui.ScreenHeader
import bendev.addblock.core.ui.SectionCard
import bendev.addblock.core.ui.asTime
import bendev.addblock.core.ui.formatted
import bendev.addblock.feature.querylog.domain.model.DnsQuery
import bendev.addblock.feature.querylog.domain.model.MinuteBucket
import bendev.addblock.feature.querylog.domain.model.QueryStats
import bendev.addblock.feature.querylog.presentation.components.ActivityChart
import bendev.addblock.feature.querylog.presentation.components.DomainRanking
import bendev.addblock.feature.querylog.presentation.components.ShareBar
import bendev.addblock.ui.theme.AddBlockTheme

private enum class Section(val label: String, val icon: ImageVector) {
    SUMMARY("Resumen", Icons.Rounded.QueryStats),
    LOG("Registro", Icons.Rounded.History),
}

@Composable
fun QueryLogScreen(
    modifier: Modifier = Modifier,
    viewModel: QueryLogViewModel = appViewModel {
        QueryLogViewModel(
            observeQueryLog, observeQueryStats, observeAllowlist, clearQueryLog,
            addToAllowlist, removeFromAllowlist,
        )
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val timeline by viewModel.timeline.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(Section.SUMMARY) }

    Column(modifier.padding(horizontal = 20.dp)) {
        ScreenHeader(
            title = "Actividad",
            subtitle = "Consultas DNS de esta sesión",
            icon = Icons.Rounded.Insights,
        )

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Section.entries.forEachIndexed { i, s ->
                SegmentedButton(
                    selected = section == s,
                    onClick = { section = s },
                    shape = SegmentedButtonDefaults.itemShape(i, Section.entries.size),
                    icon = {
                        Icon(
                            s.icon,
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                        )
                    },
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        activeBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) { Text(s.label, maxLines = 1) }
            }
        }

        AnimatedContent(
            targetState = section,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
            label = "section",
        ) { current ->
            when (current) {
                Section.SUMMARY -> SummaryContent(state.stats, timeline)
                Section.LOG -> LogContent(
                    state = state,
                    onSearch = viewModel::setSearch,
                    onFilter = viewModel::setFilter,
                    onClear = viewModel::clear,
                    onAllow = viewModel::allow,
                    onDisallow = viewModel::disallow,
                )
            }
        }
    }
}

// ---------- Resumen ----------

@Composable
private fun SummaryContent(stats: QueryStats, timeline: List<MinuteBucket>) {
    val colors = AddBlockTheme.extraColors
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Kpi(
                        Icons.Rounded.Dns, MaterialTheme.colorScheme.primary,
                        "Consultas", stats.total.formatted(), Modifier.weight(1f),
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Kpi(
                        Icons.Rounded.Block, colors.blocked,
                        "Bloqueadas", stats.blocked.formatted(), Modifier.weight(1f),
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Kpi(
                        Icons.Rounded.PieChart, colors.allowed,
                        "Bloqueo", "${stats.blockedPercent}%", Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(20.dp))
                ShareBar(blocked = stats.blocked, allowed = stats.allowed)
            }
        }

        ChartCard(
            icon = Icons.Rounded.BarChart,
            tint = MaterialTheme.colorScheme.primary,
            title = "Consultas por minuto",
            subtitle = "Últimos 30 minutos",
        ) {
            ActivityChart(timeline)
        }

        ChartCard(
            icon = Icons.Rounded.Leaderboard,
            tint = colors.blocked,
            title = "Más bloqueados",
            subtitle = "Dominios de publicidad y rastreo",
        ) {
            DomainRanking(
                items = stats.topBlocked,
                color = colors.blocked,
                emptyText = "Todavía no se ha bloqueado ningún dominio.",
            )
        }

        ChartCard(
            icon = Icons.Rounded.Public,
            tint = colors.allowed,
            title = "Más consultados",
            subtitle = "Dominios permitidos",
        ) {
            DomainRanking(
                items = stats.topAllowed,
                color = colors.allowed,
                emptyText = "Todavía no hay consultas permitidas.",
            )
        }
    }
}

@Composable
private fun Kpi(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconBadge(icon, tint, size = 32.dp)
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ChartCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    SectionCard {
        Column(Modifier.padding(20.dp)) {
            CardTitle(icon = icon, title = title, subtitle = subtitle, tint = tint)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

// ---------- Registro ----------

@Composable
private fun LogContent(
    state: QueryLogUiState,
    onSearch: (String) -> Unit,
    onFilter: (QueryFilter) -> Unit,
    onClear: () -> Unit,
    onAllow: (String) -> Unit,
    onDisallow: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchPill(state.search, onSearch, Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            FilledTonalIconButton(
                onClick = onClear,
                enabled = !state.isLogEmpty,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = "Limpiar registro")
            }
        }

        Row(
            Modifier
                .padding(vertical = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QueryFilter.entries.forEach { f ->
                FilterChip(
                    selected = state.filter == f,
                    onClick = { onFilter(f) },
                    label = { Text(f.label, maxLines = 1) },
                    leadingIcon = {
                        Icon(
                            when (f) {
                                QueryFilter.ALL -> Icons.Rounded.AllInclusive
                                QueryFilter.BLOCKED -> Icons.Rounded.Block
                                QueryFilter.ALLOWED -> Icons.Rounded.CheckCircle
                            },
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }

        when {
            state.isLogEmpty -> EmptyState(
                icon = Icons.Rounded.History,
                title = "Sin actividad",
                message = "Activa el bloqueo y usa tus apps: aquí verás cada dominio que se consulta.",
                modifier = Modifier.fillMaxSize(),
            )

            state.queries.isEmpty() -> EmptyState(
                icon = Icons.Rounded.Search,
                title = "Sin resultados",
                message = "Ninguna consulta coincide con la búsqueda.",
                modifier = Modifier.fillMaxSize(),
            )

            else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                val last = state.queries.lastIndex
                itemsIndexed(state.queries, key = { i, q -> "${q.timestamp}-${q.domain}-$i" }) { i, query ->
                    QueryRow(
                        query = query,
                        allowlisted = query.domain in state.allowlist,
                        shape = groupShape(i, last),
                        showDivider = i < last,
                        onAllow = { onAllow(query.domain) },
                        onDisallow = { onDisallow(query.domain) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchPill(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(48.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        "Buscar dominio",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Borrar búsqueda",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/** Rows read as one grouped card: rounded only at the top of the first and bottom of the last. */
private fun groupShape(index: Int, last: Int): RoundedCornerShape {
    val r = 20.dp
    val top = if (index == 0) r else 0.dp
    val bottom = if (index == last) r else 0.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun QueryRow(
    query: DnsQuery,
    allowlisted: Boolean,
    shape: RoundedCornerShape,
    showDivider: Boolean,
    onAllow: () -> Unit,
    onDisallow: () -> Unit,
) {
    val colors = AddBlockTheme.extraColors
    Column(
        Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                icon = if (query.blocked) Icons.Rounded.Block else Icons.Rounded.CheckCircle,
                tint = if (query.blocked) colors.blocked else colors.allowed,
                size = 32.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    query.domain,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${if (query.blocked) "Bloqueado" else "Permitido"} · ${query.timestamp.asTime()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            when {
                allowlisted -> SmallAction("Quitar", onDisallow)
                query.blocked -> SmallAction("Permitir", onAllow)
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 58.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun SmallAction(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = Modifier.height(32.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}
