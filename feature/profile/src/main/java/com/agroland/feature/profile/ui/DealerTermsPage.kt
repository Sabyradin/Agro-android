package com.agroland.feature.profile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.BottomActionContainer
import com.agroland.core.ui.theme.extendedColors

/**
 * Бизнес шарттар гейі (DealerTermsPage):
 * дилер шарттарды қабылдамағанша профиль/жариялау/сатуды жабады
 * (backend 403 DEALER_TERMS_NOT_ACCEPTED).
 * standalone=true — 403 арқылы кіргенде (артқа қайту мүмкін); embedded — ProfilePage ішінде.
 */
@Composable
fun DealerTermsPage(
    standalone: Boolean,
    onBack: (() -> Unit)? = null,
) {
    val viewModel = rememberProfileViewModel()
    val saving by viewModel.saving.collectAsState()
    val ext = extendedColors()

    val bullets = listOf(
        stringResource(L10nR.string.profile_terms_bullet_1),
        stringResource(L10nR.string.profile_terms_bullet_2),
        stringResource(L10nR.string.profile_terms_bullet_3),
        stringResource(L10nR.string.profile_terms_bullet_4),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.height(40.dp))
            Text(
                text = stringResource(L10nR.string.profile_terms_title),
                style = MaterialTheme.typography.titleLarge,
                color = ext.primaryText,
            )
            Box(modifier = Modifier.height(24.dp))
            bullets.forEach { bullet ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(20.dp),
                    )
                    Text(
                        text = bullet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ext.primaryText,
                    )
                }
            }
            Box(modifier = Modifier.height(140.dp))
        }

        BottomActionContainer(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            AgroButton(
                text = stringResource(L10nR.string.profile_terms_accept),
                onClick = { viewModel.acceptBusinessTerms() },
                enabled = !saving,
                loading = saving,
                modifier = Modifier.fillMaxWidth(),
            )
            if (standalone && onBack != null) {
                Box(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(L10nR.string.common_back),
                    style = MaterialTheme.typography.labelLarge,
                    color = ext.secondaryText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onBack)
                        .padding(8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}