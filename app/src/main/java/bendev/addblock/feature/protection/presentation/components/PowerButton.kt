package bendev.addblock.feature.protection.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import bendev.addblock.feature.protection.domain.model.ProtectionStatus
import bendev.addblock.ui.theme.AddBlockTheme

@Composable
fun PowerButton(
    status: ProtectionStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AddBlockTheme.extraColors
    val running = status == ProtectionStatus.RUNNING
    val starting = status == ProtectionStatus.STARTING

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale",
    )
    val onAmount by animateFloatAsState(
        targetValue = if (running) 1f else 0f,
        animationSpec = tween(500),
        label = "on",
    )
    val iconTint by animateColorAsState(
        targetValue = if (running) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "iconTint",
    )

    val transition = rememberInfiniteTransition(label = "power")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "pulse",
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "spin",
    )

    Box(modifier.size(260.dp), contentAlignment = Alignment.Center) {
        // Expanding rings while protected
        Canvas(Modifier.fillMaxSize()) {
            val maxR = size.minDimension / 2
            if (onAmount > 0f) {
                for (i in 0..2) {
                    val p = (pulse + i / 3f) % 1f
                    drawCircle(
                        color = colors.brandEnd.copy(alpha = (1f - p) * 0.22f * onAmount),
                        radius = maxR * (0.6f + 0.4f * p),
                    )
                }
            }
            if (starting) {
                val stroke = 4.dp.toPx()
                val r = maxR * 0.66f
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color.Transparent, colors.brandEnd)),
                    startAngle = spin,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = center.copy(x = center.x - r, y = center.y - r),
                    size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(156.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .shadow(
                    elevation = (6 + 18 * onAmount).dp,
                    shape = CircleShape,
                    ambientColor = colors.brandEnd,
                    spotColor = colors.brandEnd,
                )
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.brandStart.copy(alpha = onAmount),
                            colors.brandEnd.copy(alpha = onAmount),
                        ),
                    ),
                )
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(color = Color.White),
                    role = Role.Switch,
                    onClick = onClick,
                )
                .semantics {
                    contentDescription = if (running) "Desactivar bloqueo" else "Activar bloqueo"
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.PowerSettingsNew,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(60.dp),
            )
        }
    }
}
