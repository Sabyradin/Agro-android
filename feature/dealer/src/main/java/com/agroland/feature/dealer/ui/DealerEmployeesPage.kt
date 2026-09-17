package com.agroland.feature.dealer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroChip
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroTextField
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.dealer.data.DealerEmployee
import com.agroland.feature.dealer.data.displayText
import com.agroland.feature.dealer.ui.DealerEmployeesViewModel.Event

/**
 * Қызметкерлер — Flutter DealerEmployeesPage (1:1): команда мүшелерінің
 * тізімі, қосу диалогы (аты / телефон 11 цифр / ЖСН 12 цифр / рөл чиптері)
 * және жою растыруымен. Backend қате денелері жай string болуы мүмкін —
 * репозиторий оны көтере алады.
 */
@Composable
fun DealerEmployeesPage(
    onBack: () -> Unit,
    viewModel: DealerEmployeesViewModel = hiltViewModel(),
) {
    val employees by viewModel.employees.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val addInProgress by viewModel.addInProgress.collectAsState()
    val deleteInProgress by viewModel.deleteInProgress.collectAsState()

    val networkError = stringResource(L10nR.string.error_no_internet)
    val genericError = stringResource(L10nR.string.error_generic_message)
    val addedTemplate = stringResource(L10nR.string.dealer_employee_added)
    val deletedTemplate = stringResource(L10nR.string.dealer_employee_deleted)
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<DealerEmployee?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is Event.ShowError ->
                    snackbar.showSnackbar(event.error.displayText(networkError, genericError))
                is Event.EmployeeAdded -> {
                    showAddDialog = false
                    snackbar.showSnackbar(addedTemplate.format(event.name))
                }
                is Event.EmployeeDeleted ->
                    snackbar.showSnackbar(deletedTemplate.format(event.name))
            }
        }
    }

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(L10nR.string.dealer_employees),
                onBack = onBack,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = extendedColors().white,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(imageVector = Icons.Rounded.GroupAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(L10nR.string.dealer_employee_add), fontWeight = FontWeight.W600)
            }
        },
    ) { modifier ->
        Box(modifier = modifier) {
            when {
                loading -> LoadingWidget()
                error != null -> CenteredContent {
                    ErrorWithRetry(
                        onRetry = viewModel::load,
                        message = error!!.displayText(networkError, genericError),
                    )
                }
                employees.isEmpty() -> EmptyView(
                    icon = Icons.Rounded.Groups,
                    title = stringResource(L10nR.string.dealer_employees_empty),
                    message = stringResource(L10nR.string.dealer_employees_empty_hint),
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(employees, key = { it.id }) { employee ->
                        EmployeeCard(
                            employee = employee,
                            deleteInProgress = deleteInProgress == employee.id,
                            onDelete = { deleteCandidate = employee },
                        )
                    }
                }
            }
            androidx.compose.material3.SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showAddDialog) {
        AddEmployeeDialog(
            addInProgress = addInProgress,
            onDismiss = { showAddDialog = false },
            onAdd = { name, phone, role, iin -> viewModel.add(name, phone, role, iin) },
        )
    }

    deleteCandidate?.let { employee ->
        ConfirmDialog(
            title = stringResource(L10nR.string.dealer_employee_delete),
            message = stringResource(L10nR.string.dealer_employee_delete_confirm, employee.name),
            confirmLabel = stringResource(L10nR.string.common_delete),
            onDismiss = { deleteCandidate = null },
            onConfirm = {
                viewModel.delete(employee)
                deleteCandidate = null
            },
        )
    }
}

/** Қызметкер карточкасы — бастауыш әріптер аватары + аты + рөл · телефон · ЖСН + жою. */
@Composable
private fun EmployeeCard(
    employee: DealerEmployee,
    deleteInProgress: Boolean,
    onDelete: () -> Unit,
) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ext.card)
            .border(0.5.dp, ext.divider, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = employee.name.trim().split(Regex("\\s+")).take(2)
                    .mapNotNull { it.firstOrNull()?.uppercase() }.joinToString(""),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = employee.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W600,
                color = ext.primaryText,
            )
            val roleLabel = employee.dealerRole?.let { dealerRoleLabelRes(it) }?.let { stringResource(it) }
            val subtitle = listOfNotNull(
                roleLabel,
                employee.phone,
                employee.iin,
            ).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.secondaryText,
                )
            }
        }
        TextButton(
            onClick = onDelete,
            enabled = !deleteInProgress,
        ) {
            if (deleteInProgress) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    text = stringResource(L10nR.string.common_delete),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

/** Қосу диалогы — валидациясымен (аты, телефон 11 цифр, ЖСН 12 цифр, рөл). */
@Composable
private fun AddEmployeeDialog(
    addInProgress: Boolean,
    onDismiss: () -> Unit,
    onAdd: (name: String, phone: String, role: String, iin: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var iin by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("manager") }
    var nameError by remember { mutableStateOf<Int?>(null) }
    var phoneError by remember { mutableStateOf<Int?>(null) }
    var iinError by remember { mutableStateOf<Int?>(null) }

    fun submit() {
        val digits = phone.filter { it.isDigit() }
        var valid = true
        nameError = null
        phoneError = null
        iinError = null
        if (name.isBlank()) {
            nameError = L10nR.string.dealer_employee_name_required
            valid = false
        }
        if (digits.length != 11) {
            phoneError = L10nR.string.dealer_employee_phone_format
            valid = false
        }
        if (!Regex("^\\d{12}$").matches(iin)) {
            iinError = L10nR.string.dealer_employee_iin_format
            valid = false
        }
        if (valid) onAdd(name.trim(), digits, role, iin)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(L10nR.string.dealer_employee_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AgroTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = stringResource(L10nR.string.dealer_employee_name),
                    isError = nameError != null,
                    supportingText = nameError?.let { stringResource(it) },
                )
                AgroTextField(
                    value = phone,
                    onValueChange = { phone = it; phoneError = null },
                    label = stringResource(L10nR.string.dealer_phone),
                    isError = phoneError != null,
                    supportingText = phoneError?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                AgroTextField(
                    value = iin,
                    onValueChange = { iin = it; iinError = null },
                    label = stringResource(L10nR.string.dealer_employee_iin),
                    isError = iinError != null,
                    supportingText = iinError?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(L10nR.string.dealer_employee_role),
                    style = MaterialTheme.typography.labelMedium,
                    color = extendedColors().secondaryText,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AgroChip(
                        text = stringResource(L10nR.string.dealer_role_manager),
                        selected = role == "manager",
                        onClick = { role = "manager" },
                    )
                    AgroChip(
                        text = stringResource(L10nR.string.dealer_role_financier),
                        selected = role == "financier",
                        onClick = { role = "financier" },
                    )
                }
            }
        },
        confirmButton = {
            AgroButton(
                text = stringResource(L10nR.string.dealer_employee_add),
                onClick = { submit() },
                enabled = !addInProgress,
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !addInProgress) {
                Text(stringResource(L10nR.string.common_cancel))
            }
        },
    )
}

/** dealer_role → лейбл (manager/financier/director). */
internal fun dealerRoleLabelRes(role: String): Int = when (role) {
    "financier" -> L10nR.string.dealer_role_financier
    "director" -> L10nR.string.dealer_role_director
    else -> L10nR.string.dealer_role_manager
}