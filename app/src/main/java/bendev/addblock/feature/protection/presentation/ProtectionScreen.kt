package bendev.addblock.feature.protection.presentation

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bendev.addblock.core.di.appViewModel
import bendev.addblock.core.ui.CardTitle
import bendev.addblock.core.ui.IconBadge
import bendev.addblock.core.ui.ScreenHeader
import bendev.addblock.core.ui.SectionCard
import bendev.addblock.core.ui.formatted
import bendev.addblock.feature.protection.domain.model.ProtectionStatus
import bendev.addblock.feature.protection.presentation.components.PowerButton
import bendev.addblock.feature.querylog.presentation.components.ActivityChart
import bendev.addblock.ui.theme.AddBlockTheme

@Composable
fun ProtectionScreen(
    modifier: Modifier = Modifier,
    viewModel: ProtectionViewModel = appViewModel {
        ProtectionViewModel(
            observeProtection, observeBlockList, observeQueryStats, observeAllowlist, loadBlockList,
            startProtection, stopProtection, checkPrivateDns,
        )
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = AddBlockTheme.extraColors
    val vpnPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result -> if (result.resultCode == Activity.RESULT_OK) viewModel.start() }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPrivateDns()
        onPauseOrDispose { }
    }

    val toggle = {
        if (state.status == ProtectionStatus.STOPPED) {
            val request = VpnService.prepare(context)
            if (request == null) viewModel.start() else vpnPermission.launch(request)
        } else {
            viewModel.stop()
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(
            title = "AddBlock",
            subtitle = "Bloqueador de anuncios",
            icon = Icons.Rounded.Shield,
        ) {
            StatusPill(state.status)
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            PowerButton(state.status, onClick = toggle)
            AnimatedContent(
                targetState = state.status,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                label = "statusTitle",
            ) { status ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        when (status) {
                            ProtectionStatus.RUNNING -> "Estás protegido"
                            ProtectionStatus.STARTING -> "Activando…"
                            ProtectionStatus.STOPPED -> "Protección apagada"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when (status) {
                            ProtectionStatus.RUNNING ->
                                "Filtrando ${state.domainCount.formatted()} dominios de publicidad"
                            ProtectionStatus.STARTING -> "Preparando las listas de bloqueo"
                            ProtectionStatus.STOPPED -> "Toca el botón para bloquear anuncios"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        AnimatedVisibility(visible = state.error != null) {
            Notice(
                icon = Icons.Rounded.ErrorOutline,
                tint = MaterialTheme.colorScheme.error,
                title = "No se pudo activar",
                message = state.error.orEmpty(),
            )
        }

        AnimatedVisibility(visible = state.privateDnsStrict) {
            Notice(
                icon = Icons.Rounded.Dns,
                tint = MaterialTheme.colorScheme.error,
                title = "DNS privado activado",
                message = "Con un proveedor de \"DNS privado\" Android se salta este filtro. " +
                    "Cámbialo a Automático o Desactivado.",
                action = "Abrir ajustes de red" to {
                    context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                },
            )
        }

        BlockedCard(state)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat(
                icon = Icons.Rounded.Layers,
                tint = MaterialTheme.colorScheme.primary,
                value = state.domainCount.formatted(),
                label = "Dominios en lista",
                modifier = Modifier.weight(1f),
            )
            MiniStat(
                icon = Icons.Rounded.VerifiedUser,
                tint = colors.allowed,
                value = state.allowlistCount.formatted(),
                label = "Permitidos",
                modifier = Modifier.weight(1f),
            )
        }

        Notice(
            icon = Icons.Rounded.RestartAlt,
            tint = MaterialTheme.colorScheme.primary,
            title = "Actívalo siempre",
            message = "Activa \"VPN siempre activa\" para que el bloqueo se encienda solo al " +
                "reiniciar el celular.",
            action = "Abrir ajustes de VPN" to {
                context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
            },
        )
    }
}

@Composable
private fun StatusPill(status: ProtectionStatus) {
    val colors = AddBlockTheme.extraColors
    val (label, icon, tint) = when (status) {
        ProtectionStatus.RUNNING -> Triple("Activo", Icons.Rounded.CheckCircle, colors.allowed)
        ProtectionStatus.STARTING ->
            Triple("Iniciando", Icons.Rounded.HourglassTop, MaterialTheme.colorScheme.primary)
        ProtectionStatus.STOPPED ->
            Triple("Inactivo", Icons.Rounded.PauseCircle, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            Modifier.padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BlockedCard(state: ProtectionUiState) {
    val blocked by animateIntAsState(state.blockedCount, tween(600), label = "blocked")
    SectionCard {
        Column(Modifier.padding(20.dp)) {
            CardTitle(
                icon = Icons.Rounded.Block,
                title = "Anuncios bloqueados",
                subtitle = "Desde que activaste la protección",
                tint = AddBlockTheme.extraColors.blocked,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(blocked.formatted(), style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.width(10.dp))
                Text(
                    "de ${state.totalQueries.formatted()} consultas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            if (state.totalQueries > 0) {
                Text(
                    "${state.blockedPercent}% del tráfico DNS era publicidad o rastreo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            ActivityChart(state.timeline)
        }
    }
}

@Composable
private fun MiniStat(
    icon: ImageVector,
    tint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            IconBadge(icon, tint, size = 36.dp)
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Notice(
    icon: ImageVector,
    tint: Color,
    title: String,
    message: String,
    action: Pair<String, () -> Unit>? = null,
) {
    SectionCard {
        Row(Modifier.padding(16.dp)) {
            IconBadge(icon, tint, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                action?.let { (label, onClick) ->
                    // Pull the button left so its label lines up with the text above
                    TextButton(onClick = onClick, modifier = Modifier.offset(x = (-12).dp)) {
                        Text(label)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
