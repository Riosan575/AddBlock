package bendev.addblock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import bendev.addblock.core.ui.FloatingNavBar
import bendev.addblock.core.ui.NavItem
import bendev.addblock.feature.allowlist.presentation.AllowlistScreen
import bendev.addblock.feature.blocklist.presentation.BlockListScreen
import bendev.addblock.feature.protection.presentation.ProtectionScreen
import bendev.addblock.feature.querylog.presentation.QueryLogScreen
import bendev.addblock.ui.theme.AddBlockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AddBlockTheme {
                AddBlockNavigation()
            }
        }
    }
}

private enum class Tab(val item: NavItem) {
    PROTECTION(NavItem("Inicio", Icons.Rounded.Shield, Icons.Outlined.Shield)),
    LISTS(NavItem("Listas", Icons.Rounded.Layers, Icons.Outlined.Layers)),
    LOG(NavItem("Actividad", Icons.Rounded.Insights, Icons.Outlined.Insights)),
    ALLOWLIST(NavItem("Permitidos", Icons.Rounded.VerifiedUser, Icons.Outlined.VerifiedUser)),
}

@Composable
private fun AddBlockNavigation() {
    var current by rememberSaveable { mutableStateOf(Tab.PROTECTION) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            FloatingNavBar(
                items = Tab.entries.map { it.item },
                selectedIndex = current.ordinal,
                onSelect = { current = Tab.entries[it] },
            )
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.98f)) togetherWith
                    fadeOut(tween(120))
            },
            label = "tab",
        ) { tab ->
            val modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
            when (tab) {
                Tab.PROTECTION -> ProtectionScreen(modifier)
                Tab.LISTS -> BlockListScreen(modifier)
                Tab.LOG -> QueryLogScreen(modifier)
                Tab.ALLOWLIST -> AllowlistScreen(modifier)
            }
        }
    }
}
