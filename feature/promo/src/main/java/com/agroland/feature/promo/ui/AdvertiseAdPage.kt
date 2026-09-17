package com.agroland.feature.promo.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroTextButton
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.marketplace.data.Announcement
import java.util.Locale

/**
 * AdvertiseAdPage (Flutter AdvertiseAdPage, 1:1): өз жарнамасын жылжыту —
 * баланс картасы, жарнама превьюсы, ағымдағы промо күйі және промо v2
 * каталогы (boost/vip/banner/auto_renew). Белсендіру оқиғалары:
 * сәтті → диалог, INSUFFICIENT_BALANCE → толтыру диалогы, жұмсақ
 * валидация → toast, қалғаны → қате диалогы.
 */
@Composable
fun AdvertiseAdPage(
    announcementId: Long,
    onBack: () -> Unit,
    onTopUp: () -> Unit,
    viewModel: AdvertiseAdViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val ext = extendedColors()

    val localeTag = Locale.getDefault().language

    // Оқиға диалогтарының күйі.
    var successDialog by remember { mutableStateOf(false) }
    var insufficientBalance by remember { mutableStateOf<Double?>(null) }
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }

    val genericErrorText = stringResource(L10nR.string.error_generic_message)
    val uploadFailedText = stringResource(L10nR.string.banner_image_upload_failed)

    LaunchedEffect(announcementId) { viewModel.refresh(announcementId) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                PromoEvent.Activated -> successDialog = true
                is PromoEvent.InsufficientBalance -> insufficientBalance = event.missingAmount
                is PromoEvent.SoftError -> {
                    val text = event.message ?: genericErrorText
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                }
                is PromoEvent.Error ->
                    errorDialogMessage =
                        event.failure.let { failure ->
                            (failure as? com.agroland.core.network.error.Failure.Http)
                                ?.error?.message ?: genericErrorText
                        }
                PromoEvent.BannerUploadFailed ->
                    Toast.makeText(context, uploadFailedText, Toast.LENGTH_SHORT).show()
            }
        }
    }

    AgroScaffold(
        topBar = {
            AgroAppBar(title = stringResource(L10nR.string.advertise), onBack = onBack)
        },
    ) { inner ->
        LazyColumn(
            modifier = inner.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp,
            ),
        ) {
            item { BalanceCard(balance = state.balance) }

            state.announcement?.let { announcement ->
                item { AnnouncementPreviewCard(announcement = announcement) }
            }

            item {
                PromoCurrentStatus(
                    boostMultiplier = state.announcement?.boostMultiplier,
                    isVip = state.announcement?.isVip == true,
                    isVipSeller = state.isVipSeller,
                    autoRenewEnabled = state.announcement?.autoRenewEnabled == true,
                    promotion = state.promotion,
                    promotionLoading = state.promotionLoading,
                    onToggleAutoRenewal = viewModel::toggleAutoRenewal,
                )
            }

            item {
                PromoCatalogSection(
                    items = state.catalog,
                    loading = state.catalogLoading,
                    announcementId = announcementId,
                    localeTag = localeTag,
                    activatingSku = state.activatingSku,
                    onActivate = { viewModel.activate(it, announcementId, emptyMap()) },
                    bannerForm = { item, submitting ->
                        PromoBannerForm(
                            uploading = state.uploadingImage,
                            uploadedImageUrl = state.uploadedImageUrl,
                            submitting = submitting,
                            presetAnnouncementId = announcementId,
                            onPickImage = viewModel::uploadBannerImage,
                            onSubmit = { body -> viewModel.activate(item, announcementId, body) },
                        )
                    },
                )
            }
        }
    }

    // Сәтті белсендіру — промо_activated диалогы.
    if (successDialog) {
        AlertDialog(
            onDismissRequest = { successDialog = false },
            title = { Text(stringResource(L10nR.string.promo_activated)) },
            confirmButton = {
                AgroTextButton(
                    text = stringResource(L10nR.string.common_ok),
                    onClick = { successDialog = false },
                )
            },
        )
    }

    // Баланс жетпейді — толтыру диалогы (INSUFFICIENT_BALANCE).
    insufficientBalance?.let { missing ->
        AlertDialog(
            onDismissRequest = { insufficientBalance = null },
            title = { Text(stringResource(L10nR.string.promo_insufficient_balance)) },
            text = {
                Text(
                    stringResource(
                        L10nR.string.promo_need_to_top_up,
                        PriceFormatter.format(missing, ""),
                    ),
                )
            },
            confirmButton = {
                AgroTextButton(
                    text = stringResource(L10nR.string.promo_replenish_balance),
                    onClick = {
                        insufficientBalance = null
                        onTopUp()
                    },
                )
            },
            dismissButton = {
                AgroTextButton(
                    text = stringResource(L10nR.string.common_cancel),
                    onClick = { insufficientBalance = null },
                )
            },
        )
    }

    // Қалған қателер — backend адам тіліндегі хабарламасы не generic.
    errorDialogMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorDialogMessage = null },
            title = { Text(stringResource(L10nR.string.error_generic_title)) },
            text = { Text(message) },
            confirmButton = {
                AgroTextButton(
                    text = stringResource(L10nR.string.common_ok),
                    onClick = { errorDialogMessage = null },
                )
            },
        )
    }
}

/**
 * Баланс картасы (Flutter _InfoView): «Балансыңыз: N бірлік» + әмиян иконкасы,
 * астында promote_visibility жолы.
 */
@Composable
private fun BalanceCard(balance: Double?) {
    val ext = extendedColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ext.card, RoundedCornerShape(16.dp))
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(L10nR.string.your_balance) + ": ",
                style = MaterialTheme.typography.bodyMedium,
                color = ext.primaryText,
            )
            Text(
                text = stringResource(
                    L10nR.string.units_count,
                    PriceFormatter.format(balance ?: 0.0, ""),
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.height(10.dp))
        androidx.compose.material3.HorizontalDivider(color = ext.divider)
        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = ext.secondaryText,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = stringResource(L10nR.string.promote_visibility),
                style = MaterialTheme.typography.bodyMedium,
                color = ext.secondaryText,
            )
        }
    }
}

/** Жарнама превьюсы (Flutter ProfileAnnouncementItemView, showActions: false). */
@Composable
private fun AnnouncementPreviewCard(announcement: Announcement) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ext.card, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CachedImage(
            url = announcement.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .background(ext.grey, RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = announcement.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (announcement.price != null) {
                Text(
                    text = PriceFormatter.format(
                        announcement.price,
                        currencySymbol(announcement.currency ?: "KZT"),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}