package com.program.braintrainer.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.program.braintrainer.chess.model.data.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackPress: () -> Unit
) {
    val settings by viewModel.settingsState.collectAsState()
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Podešavanja") },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            SoundSettingsRow(
                isSoundEnabled = settings.isSoundEnabled,
                onSoundToggle = viewModel::onSoundToggle
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ThemeSettingsGroup(
                selectedTheme = settings.appTheme,
                onThemeChange = viewModel::onThemeChange
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            PremiumSettingsRow(
                isPremium = settings.isPremiumUser,
                onPurchaseClick = {
                    (context as? Activity)?.let { activity ->
                        viewModel.onPurchasePremium(activity)
                    }
                }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ResetProgressRow(onResetClick = { showResetDialog = true })
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Potvrda") },
                text = { Text("Da li ste sigurni da želite da obrišete sav napredak i rezultate? Ova akcija je nepovratna.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.onResetProgressConfirmed()
                            showResetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Obriši")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Otkaži")
                    }
                }
            )
        }
    }
}

@Composable
private fun SoundSettingsRow(isSoundEnabled: Boolean, onSoundToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Zvučni efekti", style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = isSoundEnabled,
            onCheckedChange = onSoundToggle
        )
    }
}

@Composable
private fun ThemeSettingsGroup(selectedTheme: SettingsManager.AppTheme, onThemeChange: (SettingsManager.AppTheme) -> Unit) {
    Column(Modifier.selectableGroup()) {
        Text("Tema aplikacije", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))

        val themes = SettingsManager.AppTheme.entries.toTypedArray()
        themes.forEach { theme ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .selectable(
                        selected = (theme == selectedTheme),
                        onClick = { onThemeChange(theme) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (theme == selectedTheme),
                    onClick = null
                )
                Text(
                    text = when (theme) {
                        SettingsManager.AppTheme.LIGHT -> "Svetla"
                        SettingsManager.AppTheme.DARK -> "Tamna"
                        SettingsManager.AppTheme.SYSTEM -> "Sistemska podrazumevana"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun PremiumSettingsRow(isPremium: Boolean, onPurchaseClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        if (isPremium) {
            Text(
                text = "Aktivirana je Premium verzija bez reklama.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text("Ukloni reklame", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Kupovinom premium verzije uklanjate sve reklame i automatski dobijate duple XP poene za svaku rešenu zagonetku.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onPurchaseClick) {
                Text("Kupi Premium")
            }
        }
    }
}


@Composable
private fun ResetProgressRow(onResetClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Resetuj napredak", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Briše sve sačuvane rezultate.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = onResetClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text("Resetuj", color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}