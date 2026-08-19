package com.program.braintrainer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.program.braintrainer.R

/**
 * Šta korisnik vidi kad otvori zaključanu premium funkciju.
 *
 * Isti spisak na sva tri mesta i namerno: ranije se premium pominjao samo
 * jednom rečenicom u Podešavanjima, pa čovek koji naiđe na zaključan ekran nije
 * imao odakle da sazna šta uopšte dobija.
 */
@Composable
fun PremiumLockedNotice(
    introText: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.premium_locked_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = introText,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))
        PremiumBenefits()

        Spacer(Modifier.height(24.dp))
        Button(onClick = onOpenSettings) {
            Text(stringResource(R.string.premium_locked_action))
        }
    }
}

/** Sve što premium nosi, na jednom mestu — koristi ga i ekran Podešavanja. */
@Composable
fun PremiumBenefits(modifier: Modifier = Modifier) {
    val benefits = listOf(
        stringResource(R.string.premium_benefit_history),
        stringResource(R.string.premium_benefit_mistakes),
        stringResource(R.string.premium_benefit_training)
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        benefits.forEach { benefit ->
            Text(
                text = "• $benefit",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.premium_free_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
