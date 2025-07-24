package com.program.braintrainer.ui.screens.settings

import android.app.Activity
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.program.braintrainer.R
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
                title = { Text(stringResource(id = R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.content_desc_back))
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
                title = { Text(stringResource(id = R.string.dialog_title_confirmation)) },
                text = { Text(stringResource(id = R.string.dialog_reset_progress_text)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.onResetProgressConfirmed()
                            showResetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(id = R.string.button_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(stringResource(id = R.string.button_cancel))
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
        Text(stringResource(id = R.string.settings_sound_effects), style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = isSoundEnabled,
            onCheckedChange = onSoundToggle
        )
    }
}

@Composable
private fun ThemeSettingsGroup(selectedTheme: SettingsManager.AppTheme, onThemeChange: (SettingsManager.AppTheme) -> Unit) {
    Column(Modifier.selectableGroup()) {
        Text(stringResource(id = R.string.settings_app_theme), style = MaterialTheme.typography.bodyLarge)
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
                        SettingsManager.AppTheme.LIGHT -> stringResource(id = R.string.settings_theme_light)
                        SettingsManager.AppTheme.DARK -> stringResource(id = R.string.settings_theme_dark)
                        SettingsManager.AppTheme.SYSTEM -> stringResource(id = R.string.settings_theme_system)
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
                text = stringResource(id = R.string.settings_premium_activated),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(stringResource(id = R.string.settings_remove_ads), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(id = R.string.settings_premium_description),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onPurchaseClick) {
                Text(stringResource(id = R.string.settings_buy_premium))
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
            Text(stringResource(id = R.string.settings_reset_progress), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(id = R.string.settings_reset_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = onResetClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text(stringResource(id = R.string.settings_button_reset), color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}