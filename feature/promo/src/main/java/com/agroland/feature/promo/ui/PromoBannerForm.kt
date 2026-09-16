package com.agroland.feature.promo.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroTextField
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.theme.extendedColors

/**
 * Banner SKU формасы (Flutter PromoBannerForm, 1:1): /banners/main активациясы
 * үшін өрістер. Сурет таңдау → POST /banners/main/image → image_url, қалған
 * өрістер activate body-ға қосылады.
 */
@Composable
fun PromoBannerForm(
    uploading: Boolean,
    uploadedImageUrl: String?,
    submitting: Boolean,
    presetAnnouncementId: Long?,
    onPickImage: (android.net.Uri) -> Unit,
    onSubmit: (body: Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ext = extendedColors()

    var ctaUrl by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    // Өз жарнама контекстінде ашылса — id алдын ала толтырылады (Flutter-де қолмен).
    var announcementId by remember {
        mutableStateOf(presetAnnouncementId?.toString() ?: "")
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) onPickImage(uri)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Сурет таңдау / жүктелген превью.
        if (uploadedImageUrl != null) {
            Text(
                text = stringResource(L10nR.string.promo_banner_image_url_label),
                style = MaterialTheme.typography.bodySmall,
                color = ext.secondaryText,
            )
            CachedImage(
                url = uploadedImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            )
            TextButton(
                onClick = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                enabled = !uploading && !submitting,
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(L10nR.string.banner_image_remove))
            }
        } else {
            OutlinedButton(
                onClick = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                enabled = !uploading && !submitting,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (uploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(L10nR.string.banner_image_uploading))
                } else {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(L10nR.string.banner_pick_image))
                }
            }
        }

        AgroTextField(
            value = ctaUrl,
            onValueChange = { ctaUrl = it },
            label = stringResource(L10nR.string.promo_banner_cta_url_label),
            enabled = !submitting,
        )
        AgroTextField(
            value = title,
            onValueChange = { title = it },
            label = stringResource(L10nR.string.promo_banner_title_label),
            enabled = !submitting,
        )
        AgroTextField(
            value = announcementId,
            onValueChange = { announcementId = it.filter { ch -> ch.isDigit() } },
            label = stringResource(L10nR.string.promo_banner_announcement_label),
            enabled = !submitting,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number,
            ),
        )

        Spacer(Modifier.height(4.dp))
        AgroButton(
            text = stringResource(L10nR.string.promo_banner_submit),
            onClick = {
                onSubmit(
                    buildMap {
                        put("image_url", uploadedImageUrl)
                        ctaUrl.trim().takeIf { it.isNotEmpty() }?.let { put("cta_url", it) }
                        title.trim().takeIf { it.isNotEmpty() }?.let { put("title", it) }
                        announcementId.trim().toLongOrNull()?.let { put("announcement_id", it) }
                    },
                )
            },
            enabled = uploadedImageUrl != null && !uploading && !submitting,
            loading = submitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}