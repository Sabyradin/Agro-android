package com.agroland.feature.marketplace.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.agroland.core.l10n.AppLocale
import com.agroland.core.l10n.R as L10nR
import com.agroland.feature.location.data.SelectedLocation
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroCheckbox
import com.agroland.core.ui.components.AgroIconButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroTextField
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.marketplace.data.AdDraft
import com.agroland.feature.marketplace.data.Announcement
import com.agroland.feature.marketplace.data.Category
import com.agroland.feature.marketplace.data.MeasurementUnit
import com.agroland.feature.profile.data.UserLocation
import kotlinx.coroutines.launch

/**
 * CreateAdPage — жарнама жасау/өңдеу формасы (create_main_info_view + image_view +
 * client_info_view + measurement_view + create_preview_page).
 * CreateAdRoute → жасау; EditAdRoute(id) → FullAnnouncement prefill (бір VM).
 * Форма толтырылған соң — ішкі «Алдын ала қарау» қадамы, сосын жіберу.
 */
@Composable
fun CreateAdPage(
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    onOpenBulkUpload: () -> Unit,
    mapSelection: SelectedLocation? = null,
    onMapSelectionConsumed: () -> Unit = {},
    onOpenMapPicker: (SelectedLocation?) -> Unit = {},
    viewModel: CreateAdViewModel = hiltViewModel(),
    categoriesViewModel: CategoriesViewModel = rememberCategoriesViewModel(),
) {
    val editMode = viewModel.editId != null
    val draft by viewModel.draft.collectAsState()
    val images by viewModel.images.collectAsState()
    val video by viewModel.video.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val submitting by viewModel.submitting.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val locations by viewModel.locations.collectAsState()
    val zones by viewModel.deliveryZones.collectAsState()
    val isBusiness by viewModel.isBusiness.collectAsState()

    val localeTag = LocalConfiguration.current.locales[0]?.toLanguageTag()
    val grouped by categoriesViewModel.grouped.collectAsState()
    val subcategories by categoriesViewModel.subcategories.collectAsState()
    val subLoading by categoriesViewModel.subLoading.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val noInternetText = stringResource(L10nR.string.error_no_internet)
    val genericErrorText = stringResource(L10nR.string.error_generic_message)
    val createdToast = stringResource(L10nR.string.ad_created_toast)
    val updatedToast = stringResource(L10nR.string.ad_updated_toast)
    var showPreview by remember { mutableStateOf(false) }

    // Қате өріс кілті → адам тіліндегі хабарлама (композиция кезінде дайындалады).
    val validationMessages = mapOf(
        AdDraft.FIELD_TITLE to stringResource(L10nR.string.validation_title),
        AdDraft.FIELD_DESCRIPTION to stringResource(L10nR.string.validation_description),
        AdDraft.FIELD_PRICE to stringResource(L10nR.string.validation_price),
        AdDraft.FIELD_CATEGORY to stringResource(L10nR.string.validation_category),
        AdDraft.FIELD_SUBCATEGORY to stringResource(L10nR.string.validation_subcategory),
        AdDraft.FIELD_PHONES to stringResource(L10nR.string.validation_phones),
        AdDraft.FIELD_LOCATION to stringResource(L10nR.string.validation_location),
        AdDraft.FIELD_PICKUP_ADDRESS to stringResource(L10nR.string.validation_pickup_address),
    )

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CreateAdViewModel.Event.ShowError ->
                    snackbar.showSnackbar(event.error.displayText(noInternetText, genericErrorText))
                is CreateAdViewModel.Event.Submitted -> {
                    snackbar.showSnackbar(if (event.updated) updatedToast else createdToast)
                    onSubmitted()
                }
            }
        }
    }

    // Фото таңдау — көп сурет; бейне — бір файл (backend ≤10MB, MultipartHelper шектейді).
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10),
    ) { uris -> if (uris.isNotEmpty()) viewModel.addImages(uris) }
    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.setVideo(uri) }

    // Категория диалогының күйі.
    var categoryPickerVisible by remember { mutableStateOf(false) }
    var unitPickerVisible by remember { mutableStateOf(false) }
    var locationPickerVisible by remember { mutableStateOf(false) }

    LaunchedEffect(draft.categoryId) {
        if (draft.categoryId != null) categoriesViewModel.loadSubcategories(draft.categoryId!!)
    }

    // Карта нәтижесі (Фаза 7): user_location_id болмағанда каталог локациясы.
    LaunchedEffect(mapSelection) {
        if (mapSelection != null) {
            viewModel.updateDraft { it.copy(userLocationId = null, location = mapSelection) }
            onMapSelectionConsumed()
        }
    }

    val invalidField = draft.validate()

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(
                    if (editMode) L10nR.string.edit_ad_title else L10nR.string.create_ad_title,
                ),
                onBack = onBack,
                actions = {
                    if (!editMode) {
                        AgroIconButton(
                            icon = Icons.Outlined.UploadFile,
                            contentDescription = stringResource(L10nR.string.bulk_upload_title),
                            onClick = onOpenBulkUpload,
                        )
                    }
                },
            )
        },
    ) { inner ->
        Box(modifier = inner.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                when {
                    loading -> Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingWidget()
                    }
                    showPreview -> {
                        // ---- Алдын ала қарау (create_preview_page) ----
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            AdPreviewCard(draft = draft, newImages = images, video = video)
                            AgroButton(
                                text = stringResource(L10nR.string.ad_publish),
                                onClick = viewModel::submit,
                                loading = submitting,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            androidx.compose.material3.TextButton(
                                onClick = { showPreview = false },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(L10nR.string.ad_preview_edit))
                            }
                        }
                    }
                    else -> {
                        // ---- Форма ----
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            MediaSection(
                                existingImages = draft.images,
                                newImages = images,
                                video = video,
                                onAddImages = {
                                    imagePicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                onRemoveExisting = viewModel::removeExistingImage,
                                onRemoveNew = viewModel::removeNewImage,
                                onAddVideo = {
                                    videoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                                    )
                                },
                                onRemoveVideo = { viewModel.setVideo(null) },
                            )

                            // Атау + AI көмекшісі.
                            Row(verticalAlignment = Alignment.Bottom) {
                                Box(modifier = Modifier.weight(1f)) {
                                    AgroTextField(
                                        value = draft.title,
                                        onValueChange = { value -> viewModel.updateDraft { it.copy(title = value.take(120)) } },
                                        label = stringResource(L10nR.string.create_field_title),
                                        isError = invalidField == AdDraft.FIELD_TITLE,
                                        singleLine = false,
                                        maxLines = 2,
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.generateAiContent(AppLocale.fromTag(localeTag).tag) },
                                    enabled = draft.title.isNotBlank() && !aiLoading,
                                ) {
                                    if (aiLoading) {
                                        LoadingWidget(Modifier.size(22.dp))
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.AutoAwesome,
                                            contentDescription = stringResource(L10nR.string.ai_generate),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }

                            AgroTextField(
                                value = draft.description,
                                onValueChange = { value ->
                                    viewModel.updateDraft { it.copy(description = value.take(4000)) }
                                },
                                label = stringResource(L10nR.string.create_field_description),
                                isError = invalidField == AdDraft.FIELD_DESCRIPTION,
                                singleLine = false,
                                maxLines = 6,
                            )

                            // Категория / сабкатегория.
                            SelectRow(
                                icon = Icons.Outlined.Category,
                                label = categoryName(grouped, draft.categoryId, localeTag)
                                    ?: stringResource(L10nR.string.create_pick_category),
                                isError = invalidField == AdDraft.FIELD_CATEGORY,
                                onClick = { categoryPickerVisible = true },
                            )
                            SelectRow(
                                icon = Icons.Outlined.Category,
                                label = subcategories.firstOrNull { it.id == draft.subcategoryId }
                                    ?.localizedName(localeTag)
                                    ?: stringResource(L10nR.string.create_pick_subcategory),
                                isError = invalidField == AdDraft.FIELD_SUBCATEGORY,
                                onClick = { categoryPickerVisible = true },
                            )

                            // Баға + валюта + келісу.
                            AgroTextField(
                                value = draft.price,
                                onValueChange = { value ->
                                    viewModel.updateDraft { it.copy(price = value.filter(Char::isDigit).take(12)) }
                                },
                                label = stringResource(L10nR.string.create_field_price),
                                isError = invalidField == AdDraft.FIELD_PRICE,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                            CurrencyChips(
                                selected = draft.currency,
                                onSelect = { currency -> viewModel.updateDraft { it.copy(currency = currency) } },
                            )
                            ToggleRow(
                                title = stringResource(L10nR.string.mp_negotiable_short),
                                checked = draft.negotiable,
                                onToggle = { checked -> viewModel.updateDraft { it.copy(negotiable = checked) } },
                            )

                            // Өлшем бірлігі.
                            SelectRow(
                                icon = Icons.Outlined.Straighten,
                                label = draft.measurementUnit?.let { unitLabel(it) }
                                    ?: stringResource(L10nR.string.create_pick_unit),
                                onClick = { unitPickerVisible = true },
                            )

                            // Мекенжай (user_location_id).
                            SelectRow(
                                icon = Icons.Outlined.LocationOn,
                                label = locations.firstOrNull { it.id == draft.userLocationId }?.fullAddress
                                    ?: stringResource(L10nR.string.create_pick_location),
                                isError = invalidField == AdDraft.FIELD_LOCATION,
                                onClick = { locationPickerVisible = true },
                            )
                            // Карта/каталог арқылы — сақталған мекенжай болмағанда
                            // country/region/district каталог ID-лері жіберіледі (Фаза 7).
                            if (draft.userLocationId == null) {
                                val pickedLabel = draft.location?.displayLabel()
                                if (!pickedLabel.isNullOrBlank()) {
                                    SelectRow(
                                        icon = Icons.Outlined.Map,
                                        label = pickedLabel,
                                        onClick = { onOpenMapPicker(draft.location) },
                                    )
                                } else {
                                    SelectRow(
                                        icon = Icons.Outlined.Map,
                                        label = stringResource(L10nR.string.create_pick_location_map),
                                        onClick = { onOpenMapPicker(draft.location) },
                                    )
                                }
                            }

                            // Байланыс нөмірлері.
                            PhonesSection(
                                phones = draft.contactNumbers,
                                isError = invalidField == AdDraft.FIELD_PHONES,
                                onEdit = { phones -> viewModel.updateDraft { it.copy(contactNumbers = phones) } },
                            )

                            // Кілт сөздер.
                            KeywordsSection(
                                keywords = draft.keywords,
                                onEdit = { keywords -> viewModel.updateDraft { it.copy(keywords = keywords) } },
                            )

                            // Жеткізу / алып кету.
                            ToggleRow(
                                title = stringResource(L10nR.string.detail_delivery),
                                checked = draft.deliveryAvailable,
                                onToggle = { checked -> viewModel.updateDraft { it.copy(deliveryAvailable = checked) } },
                            )
                            if (isBusiness && draft.deliveryAvailable && zones.isNotEmpty()) {
                                DeliveryZonesSection(
                                    zones = zones,
                                    selectedIds = draft.deliveryZoneIds,
                                    onChange = viewModel::setDeliveryZones,
                                )
                            }
                            ToggleRow(
                                title = stringResource(L10nR.string.detail_pickup),
                                checked = draft.pickupAvailable,
                                onToggle = { checked -> viewModel.updateDraft { it.copy(pickupAvailable = checked) } },
                            )
                            if (draft.pickupAvailable) {
                                AgroTextField(
                                    value = draft.pickupAddress,
                                    onValueChange = { value -> viewModel.updateDraft { it.copy(pickupAddress = value) } },
                                    label = stringResource(L10nR.string.create_field_pickup_address),
                                    isError = invalidField == AdDraft.FIELD_PICKUP_ADDRESS,
                                    singleLine = false,
                                    maxLines = 2,
                                )
                            }

                            // Бизнес өрістері: SKU, қойма, себет, маркетплейс.
                            if (isBusiness) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        AgroTextField(
                                            value = draft.sku,
                                            onValueChange = { value -> viewModel.updateDraft { it.copy(sku = value.take(64)) } },
                                            label = stringResource(L10nR.string.create_field_sku),
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        AgroTextField(
                                            value = draft.stockQuantity,
                                            onValueChange = { value ->
                                                viewModel.updateDraft {
                                                    it.copy(stockQuantity = value.filter(Char::isDigit).take(9))
                                                }
                                            },
                                            label = stringResource(L10nR.string.create_field_stock),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        )
                                    }
                                }
                                ToggleRow(
                                    title = stringResource(L10nR.string.create_field_allow_cart),
                                    checked = draft.allowCart,
                                    onToggle = { checked -> viewModel.updateDraft { it.copy(allowCart = checked) } },
                                )
                                ToggleRow(
                                    title = stringResource(L10nR.string.create_field_marketplace),
                                    checked = draft.isMarketplace,
                                    onToggle = { checked -> viewModel.updateDraft { it.copy(isMarketplace = checked) } },
                                )
                            }
                        }
                        }
                    }

                // Жіберу түймесі — әрқашан көрінеді (форма скролл болғандықтан үстінен жабылады).
                androidx.compose.material3.Surface(
                    color = extendedColors().card,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AgroButton(
                        text = stringResource(L10nR.string.ad_preview),
                        onClick = {
                            val field = draft.validate()
                            if (field == null) {
                                showPreview = true
                            } else {
                                validationMessages[field]?.let { message ->
                                    scope.launch { snackbar.showSnackbar(message) }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding(),
                    )
                }
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (categoryPickerVisible) {
        CategoryPickerDialog(
            grouped = grouped,
            subcategories = subcategories,
            subLoading = subLoading,
            localeTag = localeTag,
            selectedCategoryId = draft.categoryId,
            selectedSubcategoryId = draft.subcategoryId,
            onPickCategory = { category ->
                if (category.id != draft.categoryId) {
                    viewModel.updateDraft { it.copy(categoryId = category.id, subcategoryId = null) }
                }
                categoryPickerVisible = false
            },
            onPickSubcategory = { sub ->
                viewModel.updateDraft { it.copy(subcategoryId = sub.id) }
                categoryPickerVisible = false
            },
            onDismiss = { categoryPickerVisible = false },
        )
    }

    if (unitPickerVisible) {
        ListPickerDialog(
            title = stringResource(L10nR.string.create_pick_unit),
            items = MeasurementUnit.entries,
            label = { unitLabel(it) },
            selected = draft.measurementUnit,
            onSelect = { unit ->
                viewModel.updateDraft { it.copy(measurementUnit = unit) }
                unitPickerVisible = false
            },
            onDismiss = { unitPickerVisible = false },
        )
    }

    if (locationPickerVisible) {
        ListPickerDialog(
            title = stringResource(L10nR.string.create_pick_location),
            items = locations,
            label = { it.fullAddress },
            selected = locations.firstOrNull { it.id == draft.userLocationId },
            onSelect = { location ->
                viewModel.updateDraft { it.copy(userLocationId = location.id, location = null) }
                locationPickerVisible = false
            },
            onDismiss = { locationPickerVisible = false },
        )
    }
}

private fun categoryName(
    grouped: Map<String, List<Category>>,
    categoryId: Int?,
    localeTag: String?,
): String? {
    if (categoryId == null) return null
    grouped.values.flatten().firstOrNull { it.id == categoryId }?.let { return it.localizedName(localeTag) }
    return null
}

/** Медиа бөлімі — қолданылған + жаңа суреттер, бейне, «+» таңдау плиткасы. */
@Composable
private fun MediaSection(
    existingImages: List<String>,
    newImages: List<android.net.Uri>,
    video: android.net.Uri?,
    onAddImages: () -> Unit,
    onRemoveExisting: (String) -> Unit,
    onRemoveNew: (android.net.Uri) -> Unit,
    onAddVideo: () -> Unit,
    onRemoveVideo: () -> Unit,
) {
    val ext = extendedColors()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(L10nR.string.create_section_media),
            style = MaterialTheme.typography.bodySmall,
            color = ext.primaryText,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(existingImages) { url ->
                MediaTile(onRemove = { onRemoveExisting(url) }) {
                    CachedImage(
                        url = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            items(newImages) { uri ->
                MediaTile(onRemove = { onRemoveNew(uri) }) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                }
            }
            if (video != null) {
                item {
                    MediaTile(onRemove = onRemoveVideo) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ext.grey),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.Videocam,
                                    contentDescription = null,
                                    tint = ext.primaryText,
                                )
                                Text(
                                    text = stringResource(L10nR.string.create_video_added),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.primaryText,
                                )
                            }
                        }
                    }
                }
            }
            item {
                Column {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ext.grey)
                            .clickable(onClick = onAddImages),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(L10nR.string.create_add_images),
                            tint = ext.secondaryText,
                        )
                    }
                    androidx.compose.material3.TextButton(onClick = onAddVideo) {
                        Icon(
                            imageVector = Icons.Outlined.Videocam,
                            contentDescription = null,
                            tint = ext.secondaryText,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(L10nR.string.create_add_video),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaTile(onRemove: () -> Unit, content: @Composable () -> Unit) {
    val ext = extendedColors()
    Box(
        modifier = Modifier
            .size(92.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ext.grey),
    ) {
        Box(modifier = Modifier.fillMaxSize()) { content() }
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = stringResource(L10nR.string.common_delete),
            tint = ext.primaryText,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ext.card)
                .clickable(onClick = onRemove)
                .padding(2.dp)
                .size(14.dp),
        )
    }
}

/** Валюта таңдауы — chips. */
@Composable
private fun CurrencyChips(selected: String, onSelect: (String) -> Unit) {
    val currencies = listOf("₸", "$", "€", "₽")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        currencies.forEach { currency ->
            com.agroland.core.ui.components.AgroChip(
                text = currency,
                selected = selected == currency,
                onClick = { onSelect(currency) },
            )
        }
    }
}

/** Бір сөйлем + switch — келісу/жеткізу/ҚҚС қатарлары. */
@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ext.card)
            .clickable { onToggle(!checked) }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = ext.primaryText,
            modifier = Modifier.weight(1f),
        )
        com.agroland.core.ui.components.AgroSwitch(
            checked = checked,
            onCheckedChange = onToggle,
        )
    }
}

/** Таңдау жолы — иконка + мән + қате күйі. */
@Composable
private fun SelectRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isError: Boolean = false,
    onClick: () -> Unit,
) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isError) ext.grey else ext.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isError) MaterialTheme.colorScheme.error else ext.secondaryText,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.error else ext.primaryText,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowDown,
            contentDescription = null,
            tint = ext.secondaryText,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Телефондар — қатар + өңдеу/жою + жаңа қосу. */
@Composable
private fun PhonesSection(
    phones: List<String>,
    isError: Boolean,
    onEdit: (List<String>) -> Unit,
) {
    val ext = extendedColors()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(L10nR.string.create_field_phones),
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.error else ext.primaryText,
        )
        phones.forEachIndexed { index, phone ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    AgroTextField(
                        value = phone,
                        onValueChange = { value ->
                            val updated = phones.toMutableList().also {
                                if (index < it.size) it[index] = value.take(20)
                            }
                            onEdit(updated)
                        },
                        label = stringResource(L10nR.string.create_field_phone),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    )
                }
                IconButton(
                    onClick = { onEdit(phones.filterIndexed { i, _ -> i != index }) },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(L10nR.string.common_delete),
                        tint = ext.secondaryText,
                    )
                }
            }
        }
        androidx.compose.material3.TextButton(onClick = { onEdit(phones + "") }) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(stringResource(L10nR.string.create_add_phone))
        }
    }
}

/** Кілт сөздер — chips + қосу жолы. */
@Composable
private fun KeywordsSection(
    keywords: List<String>,
    onEdit: (List<String>) -> Unit,
) {
    val ext = extendedColors()
    var input by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(L10nR.string.create_field_keywords),
            style = MaterialTheme.typography.bodySmall,
            color = ext.primaryText,
        )
        if (keywords.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(keywords) { keyword ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(ext.grey)
                            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = keyword,
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.primaryText,
                            )
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(L10nR.string.common_delete),
                                tint = ext.secondaryText,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(14.dp)
                                    .clickable { onEdit(keywords - keyword) },
                            )
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                AgroTextField(
                    value = input,
                    onValueChange = { input = it.take(30) },
                    label = stringResource(L10nR.string.create_field_keyword),
                )
            }
            IconButton(
                onClick = {
                    val keyword = input.trim()
                    if (keyword.isNotBlank() && keyword !in keywords) {
                        onEdit(keywords + keyword)
                    }
                    input = ""
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(L10nR.string.create_add_keyword),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Жеткізу аймақтары — бизнес үшін көп таңдау. */
@Composable
private fun DeliveryZonesSection(
    zones: List<com.agroland.feature.marketplace.data.DeliveryZone>,
    selectedIds: List<Long>,
    onChange: (List<Long>) -> Unit,
) {
    val ext = extendedColors()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(L10nR.string.create_field_delivery_zones),
            style = MaterialTheme.typography.bodySmall,
            color = ext.primaryText,
        )
        zones.forEach { zone ->
            val checked = zone.id in selectedIds
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ext.card)
                    .clickable {
                        onChange(if (checked) selectedIds - zone.id else selectedIds + zone.id)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = zone.name ?: zone.regionName ?: "#${zone.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.primaryText,
                    )
                    if (zone.regionName != null && zone.name != null) {
                        Text(
                            text = zone.regionName,
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.secondaryText,
                        )
                    }
                }
                AgroCheckbox(
                    checked = checked,
                    onCheckedChange = { checkedNow ->
                        onChange(if (checkedNow) selectedIds + zone.id else selectedIds - zone.id)
                    },
                )
            }
        }
    }
}

/** Алдын ала қарау карточкасы — AnnouncementCard қайта қолданылады. */
@Composable
private fun AdPreviewCard(
    draft: AdDraft,
    newImages: List<android.net.Uri>,
    video: android.net.Uri?,
) {
    val preview = Announcement(
        id = 0L,
        title = draft.title,
        price = draft.priceValue,
        currency = draft.currency,
        city = null,
        district = null,
        createdAt = null,
        imageUrl = draft.images.firstOrNull(),
        imageUrls = draft.images,
        isFavorite = false,
        authorId = null,
        status = "pending",
        measurementUnit = draft.measurementUnit?.queryKey,
        viewsCount = 0,
        callsCount = 0,
        favoritesCount = 0,
        messagesCount = 0,
        rating = null,
        reviewsCount = 0,
        negotiable = draft.negotiable,
        isVip = false,
        isHot = false,
        boostMultiplier = null,
        autoRenewEnabled = false,
        allowCart = draft.allowCart,
        isMarketplace = draft.isMarketplace,
        typeAd = null,
        deliveryAvailable = draft.deliveryAvailable,
        pickupAvailable = draft.pickupAvailable,
        pickupAddress = draft.pickupAddress.takeIf { it.isNotBlank() },
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(extendedColors().card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (draft.images.isEmpty() && newImages.isEmpty() && video == null) {
            Text(
                text = stringResource(L10nR.string.create_preview_no_media),
                style = MaterialTheme.typography.labelMedium,
                color = extendedColors().secondaryText,
            )
        }
        AnnouncementCard(
            item = preview,
            onClick = {},
            onToggleFavorite = {},
        )
        Text(
            text = draft.description,
            style = MaterialTheme.typography.bodySmall,
            color = extendedColors().primaryText,
        )
    }
}