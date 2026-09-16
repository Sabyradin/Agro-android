package com.agroland.feature.china.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.common.validators.Validators
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroTextField
import com.agroland.core.ui.theme.extendedColors
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.feature.china.data.ChinaCartItem

/** MercuryX тапсырысының ең аз сомасы (спек §3.2: 500 000 ₸). */
private const val CHINA_MIN_ORDER_TOTAL = 500_000.0

/**
 * Қытай checkout — ModalBottomSheet (Flutter ChinaCheckoutSheet):
 * таңдалған позициялар, жалпы сома + 500 000 ₸ минимум индикаторы,
 * телефон (11 цифр, 77-…), БСН (12 цифр), consent чекбоксы.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChinaCheckoutSheet(
    items: List<ChinaCartItem>,
    selectedIds: Set<Long>,
    onDismiss: () -> Unit,
    viewModel: ChinaOrdersViewModel = hiltViewModel(),
) {
    val submitting by viewModel.submitting.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var phone by remember { mutableStateOf("") }
    var bin by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    var showValidation by remember { mutableStateOf(false) }
    var forwardedNumber by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChinaEvent.ShowError -> snackbarHostState.showSnackbar(
                    event.error.displayText(
                        context.getString(L10nR.string.error_no_internet),
                        context.getString(L10nR.string.error_generic_message),
                    ),
                )
                is ChinaEvent.OrderForwarded -> forwardedNumber = event.mercuryxNumber
                else -> Unit
            }
        }
    }

    val selected = items.filter { it.id in selectedIds }
    val total = selected.sumOf { it.lineTotal }
    val meetsMin = total >= CHINA_MIN_ORDER_TOTAL
    val phoneValid = Validators.isValidChinaPhone(phone)
    val binValid = Validators.isValidBin(bin)
    val canSubmit = phoneValid && binValid && consent && meetsMin && !submitting

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ext = extendedColors()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ext.card,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(L10nR.string.china_checkout),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ext.primaryText,
                )

                // Позициялар
                selected.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ext.grey),
                        ) {
                            ChinaThumb(
                                imageUrl = item.imageUrl,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = item.titleSnapshot ?: "—",
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.primaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = PriceFormatter.format(item.lineTotal),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ext.primaryText,
                        )
                    }
                }

                // Жалпы сома
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(L10nR.string.cart_total_amount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ext.secondaryText,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = PriceFormatter.format(total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                // Ең аз сома индикаторы
                if (!meetsMin) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(L10nR.string.china_min_order_total),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(
                                L10nR.string.china_min_order_remaining,
                                PriceFormatter.format(CHINA_MIN_ORDER_TOTAL - total),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.primaryText,
                        )
                        LinearProgressIndicator(
                            progress = { (total / CHINA_MIN_ORDER_TOTAL).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Телефон: 11 цифр, 77-мен басталады
                AgroTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() }.take(11) },
                    label = stringResource(L10nR.string.china_phone_label),
                    isError = showValidation && !phoneValid,
                    errorText = stringResource(L10nR.string.china_invalid_phone),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                // БСН: 12 цифр
                AgroTextField(
                    value = bin,
                    onValueChange = { bin = it.filter { c -> c.isDigit() }.take(12) },
                    label = stringResource(L10nR.string.china_bin_label),
                    isError = showValidation && !binValid,
                    errorText = stringResource(L10nR.string.china_invalid_bin),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                // Consent чекбоксы
                Row(verticalAlignment = Alignment.Top) {
                    Checkbox(
                        checked = consent,
                        onCheckedChange = { consent = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    Text(
                        text = stringResource(L10nR.string.china_checkout_consent),
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.primaryText,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 12.dp),
                    )
                }

                AgroButton(
                    text = stringResource(L10nR.string.china_checkout),
                    onClick = {
                        if (canSubmit) {
                            viewModel.createOrder(
                                phone = phone,
                                bin = bin,
                                cartItemIds = selected.map { it.id },
                            )
                        } else {
                            showValidation = true
                        }
                    },
                    enabled = canSubmit || !showValidation,
                    loading = submitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    // Сәтті forward — MercuryX нөмірімен диалог, жабылғанда sheet те жабылады.
    if (forwardedNumber != null) {
        AlertDialog(
            onDismissRequest = { forwardedNumber = null; onDismiss() },
            confirmButton = {
                TextButton(onClick = { forwardedNumber = null; onDismiss() }) {
                    Text(stringResource(L10nR.string.common_close))
                }
            },
            title = {
                Text(stringResource(L10nR.string.china_order_forwarded))
            },
            text = {
                Column {
                    val number = forwardedNumber
                    if (!number.isNullOrBlank()) {
                        Text(stringResource(L10nR.string.china_order_number, number))
                    }
                }
            },
        )
    }
}