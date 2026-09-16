package com.agroland.feature.services.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agroland.core.common.validators.Validators
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroSearchField
import com.agroland.core.ui.components.AgroTextField
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.feature.services.data.EgovMachineryLookup
import com.agroland.feature.services.data.EgovService
import com.agroland.feature.services.data.egovServices

/**
 * EGOV сервистері (Flutter EgovServicesPage, 1:1) + VIN картасы:
 *  - VIN бойынша ауыл шаруашылығы техникасын тексеру (спек қосымшасы, GRST_TI)
 *  - Статикалық каталог — санаттарға топталған, code/name/category бойынша іздеу.
 */
@Composable
fun EgovServicesPage(
    onBack: () -> Unit,
    viewModel: EgovVinViewModel = hiltViewModel(),
) {
    val ext = extendedColors()
    var query by rememberSaveable { mutableStateOf("") }

    val filtered = if (query.isEmpty()) {
        egovServices
    } else {
        val lower = query.lowercase()
        egovServices.filter {
            it.code.lowercase().contains(lower) ||
                it.name.lowercase().contains(lower) ||
                it.category.lowercase().contains(lower)
        }
    }
    val categories = filtered.map { it.category }.distinct()

    AgroScaffold(
        topBar = { AgroAppBar(title = stringResource(L10nR.string.egov_services_title), onBack = onBack) },
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // VIN сұрау карточкасы
            item { VinLookupCard(viewModel) }

            if (categories.isEmpty()) {
                item {
                    Text(
                        text = stringResource(L10nR.string.egov_no_service),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ext.secondaryText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            } else {
                categories.forEach { category ->
                    item(key = "cat_$category") {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                        )
                    }
                    val categoryServices = filtered.filter { it.category == category }
                    items(categoryServices.size, key = { "svc_${category}_${categoryServices[it].code}" }) { index ->
                        val service = categoryServices[index]
                        EgovServiceTile(service = service)
                    }
                }
            }
        }
    }
}

/** Жалғыз сервис жолы — карточка аты (Flutter _EgovServiceTile). */
@Composable
private fun EgovServiceTile(service: EgovService) {
    val ext = extendedColors()
    Text(
        text = service.name,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = ext.primaryText,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ext.card)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

/** VIN сұрауы — формат тексеріліп, нәтиже generic кестеде көрсетіледі. */
@Composable
private fun VinLookupCard(viewModel: EgovVinViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val ext = extendedColors()

    var vin by rememberSaveable { mutableStateOf("") }
    var formatError by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(L10nR.string.egov_vin_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        AgroTextField(
            value = vin,
            onValueChange = { value ->
                vin = value.uppercase().take(17)
                formatError = false
                viewModel.reset()
            },
            label = stringResource(L10nR.string.egov_vin_hint),
            isError = formatError,
            errorText = stringResource(L10nR.string.egov_vin_invalid),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
                autoCorrect = false,
            ),
        )
        com.agroland.core.ui.components.AgroSmallButton(
            text = stringResource(L10nR.string.egov_vin_lookup),
            onClick = {
                if (Validators.isValidVin(vin)) {
                    viewModel.lookup(vin)
                } else {
                    formatError = true
                }
            },
            loading = state.loading,
            modifier = Modifier.align(Alignment.End),
        )

        when {
            state.loading -> Box(Modifier.height(40.dp)) {
                LoadingWidget()
            }
            state.result is EgovMachineryLookup.NotFound -> {
                val notFound = state.result as EgovMachineryLookup.NotFound
                Text(
                    text = notFound.message
                        ?: context.getString(L10nR.string.egov_vin_not_found),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.primaryText,
                )
            }
            state.result is EgovMachineryLookup.Found -> {
                val found = state.result as EgovMachineryLookup.Found
                Text(
                    text = stringResource(L10nR.string.egov_result_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ext.primaryText,
                )
                found.fields.forEach { (key, value) ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.secondaryText,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = ext.primaryText,
                            modifier = Modifier.width(180.dp),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            state.error != null -> {
                Text(
                    text = state.error!!.displayText(
                        context.getString(L10nR.string.error_no_internet),
                        context.getString(L10nR.string.error_generic_message),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            else -> Unit
        }
    }
}