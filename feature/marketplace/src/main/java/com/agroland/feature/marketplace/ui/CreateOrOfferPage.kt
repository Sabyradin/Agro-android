package com.agroland.feature.marketplace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.theme.extendedColors

/**
 * CreateOrOfferPage — «+» табының таңдау экраны (create_or_offer_page.dart):
 * жарнама құру, сұраныс құру (баға ұсыну) немесе топтап жүктеу.
 */
@Composable
fun CreateOrOfferPage(
    onBack: () -> Unit,
    onCreateAd: () -> Unit,
    onMakeOffer: () -> Unit,
    onOpenBulkUpload: () -> Unit,
) {
    val ext = extendedColors()
    AgroScaffold(
        topBar = {
            AgroAppBar(title = stringResource(L10nR.string.create_or_offer_title), onBack = onBack)
        },
    ) { inner ->
        Column(
            modifier = inner
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChoiceCard(
                icon = Icons.Outlined.PostAdd,
                title = stringResource(L10nR.string.create_choice_ad_title),
                message = stringResource(L10nR.string.create_choice_ad_message),
                onClick = onCreateAd,
            )
            ChoiceCard(
                icon = Icons.Outlined.RequestQuote,
                title = stringResource(L10nR.string.create_choice_offer_title),
                message = stringResource(L10nR.string.create_choice_offer_message),
                onClick = onMakeOffer,
            )
            ChoiceCard(
                icon = Icons.Outlined.UploadFile,
                title = stringResource(L10nR.string.bulk_upload_title),
                message = stringResource(L10nR.string.bulk_upload_message),
                onClick = onOpenBulkUpload,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChoiceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    onClick: () -> Unit,
) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.card)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = ext.primaryText,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = ext.secondaryText,
            )
        }
    }
}