package bendev.addblock.feature.allowlist.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddModerator
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bendev.addblock.core.di.appViewModel
import bendev.addblock.core.ui.CardTitle
import bendev.addblock.core.ui.EmptyState
import bendev.addblock.core.ui.IconBadge
import bendev.addblock.core.ui.ScreenHeader
import bendev.addblock.core.ui.SectionCard
import bendev.addblock.ui.theme.AddBlockTheme

@Composable
fun AllowlistScreen(
    modifier: Modifier = Modifier,
    viewModel: AllowlistViewModel = appViewModel {
        AllowlistViewModel(observeAllowlist, addToAllowlist, removeFromAllowlist)
    },
) {
    val domains by viewModel.domains.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }

    val submit = {
        if (viewModel.add(input)) input = "" else invalid = true
    }

    Column(modifier.padding(horizontal = 20.dp)) {
        ScreenHeader(
            title = "Permitidos",
            subtitle = "Dominios que nunca se bloquean",
            icon = Icons.Rounded.VerifiedUser,
        )

        SectionCard {
            Column(Modifier.padding(16.dp)) {
                CardTitle(
                    icon = Icons.Rounded.AddModerator,
                    title = "Agregar dominio",
                    subtitle = "Si una app deja de funcionar, permite su dominio y subdominios",
                )
                Row(
                    Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it; invalid = false },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("ejemplo.com") },
                        leadingIcon = { Icon(Icons.Rounded.Language, contentDescription = null) },
                        singleLine = true,
                        isError = invalid,
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                    )
                    Spacer(Modifier.width(10.dp))
                    FilledIconButton(
                        onClick = submit,
                        enabled = input.isNotBlank(),
                        modifier = Modifier.size(52.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Agregar")
                    }
                }
                if (invalid) {
                    Text(
                        "Escribe un dominio válido, por ejemplo ejemplo.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 6.dp, start = 16.dp),
                    )
                }
            }
        }

        if (domains.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.VerifiedUser,
                title = "Nada por aquí",
                message = "También puedes permitir dominios desde la pestaña Actividad.",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                "${domains.size} ${if (domains.size == 1) "dominio" else "dominios"}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp, start = 4.dp),
            )
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(domains.sorted(), key = { it }) { domain ->
                    AllowedRow(domain, onRemove = { viewModel.remove(domain) })
                }
            }
        }
    }
}

@Composable
private fun AllowedRow(domain: String, onRemove: () -> Unit) {
    SectionCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(Icons.Rounded.Language, AddBlockTheme.extraColors.allowed, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                domain,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Quitar $domain",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
