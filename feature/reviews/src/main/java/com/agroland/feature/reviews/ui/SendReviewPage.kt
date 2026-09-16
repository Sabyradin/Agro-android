package com.agroland.feature.reviews.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroTextField
import com.agroland.core.ui.theme.extendedColors
import kotlinx.coroutines.launch

/**
 * SendReviewPage (Flutter 1:1): FeedbackAdCard + «Жарнаманы бағалау»
 * жұлдыз қатары + мәтін ≤1000 таңба (maxCharsCount) + «Жіберу».
 * Сәтті жіберілсе — Sent event → onBack.
 */
@Composable
fun SendReviewPage(
    announcementId: Long,
    onBack: () -> Unit,
    viewModel: SendReviewViewModel = hiltViewModel(),
) {
    val rating by viewModel.rating.collectAsState()
    val text by viewModel.text.collectAsState()
    val sending by viewModel.sending.collectAsState()

    val networkError = stringResource(L10nR.string.error_no_internet)
    val genericError = stringResource(L10nR.string.error_generic_message)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ReviewEvent.ShowError -> scope.launch {
                    snackbar.showSnackbar(event.error.displayText(networkError, genericError))
                }
                ReviewEvent.Sent -> onBack()
            }
        }
    }

    val ext = extendedColors()
    val maxChars = stringResource(L10nR.string.max_chars_count, SendReviewViewModel.MAX_TEXT_LENGTH)

    AgroScaffold(
        topBar = {
            AgroAppBar(title = stringResource(L10nR.string.leave_feedback), onBack = onBack)
        },
        bottomBar = {
            Column(modifier = Modifier.imePadding()) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    AgroButton(
                        text = stringResource(L10nR.string.common_send),
                        onClick = viewModel::send,
                        enabled = rating != 0 && !sending,
                        loading = sending,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                SnackbarHost(hostState = snackbar)
            }
        },
    ) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            FeedbackAdCard(
                announcementId = announcementId,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ext.card)
                    .padding(16.dp),
            )

            // «Жарнаманы бағалау» — жұлдыздар
            Column(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ext.card)
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(L10nR.string.rate_announcement),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = ext.primaryText,
                )
                Spacer(Modifier.height(10.dp))
                RatingInput(rating = rating, onSelect = viewModel::selectRating)
            }

            // «Пікір қалдыру» — мәтін енгізу
            Column(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ext.card)
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(L10nR.string.leave_feedback),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = ext.primaryText,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = maxChars,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.secondaryText,
                    )
                }
                Spacer(Modifier.height(16.dp))
                AgroTextField(
                    value = text,
                    onValueChange = viewModel::updateText,
                    label = stringResource(L10nR.string.feedback_enter_hint),
                    singleLine = false,
                    maxLines = 5,
                )
            }
        }
    }
}