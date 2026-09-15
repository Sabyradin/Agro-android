package com.agroland.feature.payment.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.theme.extendedColors

/**
 * PaymentResultPage — Flutter payment_result_page: Halyk төлемінің нәтижесін
 * поллинг арқылы тексереді (2с/60с). WebView-тан қайтқанда, mock режимде және
 * суық старттан кейін қалпына келтірілгенде осы бет ашылады.
 */
@Composable
fun PaymentResultPage(
    orderId: Long,
    onBack: () -> Unit,
    viewModel: PaymentResultViewModel = hiltViewModel(),
) {
    val phase by viewModel.phase.collectAsState()
    LaunchedEffect(orderId) { viewModel.start(orderId) }

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(L10nR.string.payment_result),
                onBack = onBack,
            )
        },
    ) { inner ->
        Box(
            modifier = inner.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when (val current = phase) {
                PaymentResultPhase.Verifying -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(L10nR.string.payment_verifying),
                        style = MaterialTheme.typography.bodyMedium,
                        color = extendedColors().primaryText,
                    )
                }

                PaymentResultPhase.Success -> ResultState(
                    icon = Icons.Outlined.CheckCircle,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = stringResource(L10nR.string.payment_successful),
                    message = null,
                    buttonText = stringResource(L10nR.string.payment_view_orders),
                    onButton = onBack,
                )

                is PaymentResultPhase.Failed -> ResultState(
                    icon = Icons.Outlined.Error,
                    iconTint = MaterialTheme.colorScheme.error,
                    title = stringResource(L10nR.string.payment_failed),
                    message = current.error.displayText(),
                    buttonText = stringResource(L10nR.string.common_retry),
                    onButton = { viewModel.retry(orderId) },
                )

                PaymentResultPhase.TimedOut -> ResultState(
                    icon = Icons.Outlined.Schedule,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = stringResource(L10nR.string.payment_in_processing),
                    message = null,
                    buttonText = stringResource(L10nR.string.payment_view_orders),
                    onButton = onBack,
                )
            }
        }
    }
}

/** Ортақ нәтиже күйі: дөңгелек икона + тақырып (+өзекті мәтін) + батырма. */
@Composable
private fun ResultState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    message: String?,
    buttonText: String,
    onButton: () -> Unit,
) {
    val ext = extendedColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(48.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = ext.primaryText,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = ext.secondaryText,
                textAlign = TextAlign.Center,
            )
        }
        AgroButton(
            text = buttonText,
            onClick = onButton,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
    }
}