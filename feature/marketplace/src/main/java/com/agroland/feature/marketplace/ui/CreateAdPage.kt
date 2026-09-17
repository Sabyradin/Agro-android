package com.agroland.feature.marketplace.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.PinDrop
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.agroland.core.l10n.AppLocale
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroCheckbox
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.location.data.SelectedLocation
import com.agroland.feature.marketplace.data.AdDraft
import com.agroland.feature.marketplace.data.Announcement
import com.agroland.feature.marketplace.data.Category
import com.agroland.feature.marketplace.data.MeasurementUnit
import kotlinx.coroutines.launch

/** Жарнамаға қосылатын суреттердің шегі (iOS «0/10»). */
private const val MaxPhotos = 10

/** Сипаттаманың шегі (iOS «0/1 000»). */
private const val MaxDescription = 1000

/**
 * CreateAdPage — жарнама жасау/өңдеу формасы, iOS макеті бойынша топталған
 * карточкалар: байланыс ақпараты → фото + YouTube → негізгі ақпарат → сипаттама →
 * тегтер → баға (Бағасы | Келісімді) → өлшем бірлігі → (бизнес) жеткізу және сату.
 * Төменде «Алдын ала қарау» + «Жариялау».
 *
 * [header] берілсе (CreateHubPage — қойындылары бар тақырып) — сол көрсетіледі,
 * әйтпесе «Жабу» + атау тақырыбы (өңдеу режимі).
 */
@Composable
fun CreateAdPage(
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    onOpenBulkUpload: () -> Unit = {},
    mapSelection: SelectedLocation? = null,
    onMapSelectionConsumed: () -> Unit = {},
    onOpenMapPicker: (SelectedLocation?) -> Unit = {},
    header: (@Composable () -> Unit)? = null,
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
    val scope = rememberCoroutineScope()
    val noInternetText = stringResource(L10nR.string.error_no_internet)
    val genericErrorText = stringResource(L10nR.string.error_generic_message)
    val createdToast = stringResource(L10nR.string.ad_created_toast)
    val updatedToast = stringResource(L10nR.string.ad_updated_toast)
    var showPreview by remember { mutableStateOf(false) }
    // Қате өрісі тек «Жариялау»/«Алдын ала қарау» басылғаннан кейін қызыл болады.
    var showErrors by remember { mutableStateOf(false) }

    val validationMessages = mapOf(
        AdDraft.FIELD_TITLE to stringResource(L10nR.string.validation_title),
        AdDraft.FIELD_DESCRIPTION to stringResource(L10nR.string.validation_description),
        AdDraft.FIELD_PRICE to stringResource(L10nR.string.validation_price),
        AdDraft.FIELD_CATEGORY to stringResource(L10nR.string.validation_category),
        AdDraft.FIELD_SUBCATEGORY to stringResource(L10nR.string.validation_subcategory),
        AdDraft.FIELD_PHONES to stringResource(L10nR.string.validation_phones),
        AdDraft.FIELD_LOCATION to stringResource(L10nR.string.validation_location),
        AdDraft.FIELD_PICKUP_ADDRESS to stringResource(L10nR.string.validation_pickup_address),
        AdDraft.FIELD_VIDEO to stringResource(L10nR.string.validation_video_link),
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

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = MaxPhotos),
    ) { uris -> if (uris.isNotEmpty()) viewModel.addImages(uris) }

    var categoryPickerVisible by remember { mutableStateOf(false) }
    var unitPickerVisible by remember { mutableStateOf(false) }
    var addressPickerVisible by remember { mutableStateOf(false) }

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

    val invalidField = if (showErrors) draft.validate() else null

    /** Тексеріп, жарамды болса [onValid]; әйтпесе қате өрісті көрсетіп, хабарлайды. */
    fun validateThen(onValid: () -> Unit) {
        val field = draft.validate()
        if (field == null) {
            onValid()
        } else {
            showErrors = true
            validationMessages[field]?.let { message -> scope.launch { snackbar.showSnackbar(message) } }
        }
    }

    AgroScaffold(
        topBar = {
            if (header != null) {
                header()
            } else {
                CreateHeader(
                    title = stringResource(if (editMode) L10nR.string.edit_ad_title else L10nR.string.create_tab_ad),
                    closeLabel = stringResource(L10nR.string.common_close),
                    onClose = onBack,
                )
            }
        },
    ) { inner ->
        Box(modifier = inner.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().imePadding()) {
                when {
                    loading -> Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingWidget()
                    }
                    showPreview -> Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        AdPreviewCard(draft = draft, newImages = images, video = video)
                    }
                    else -> Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        ContactSection(
                            phones = draft.contactNumbers,
                            phonesError = invalidField == AdDraft.FIELD_PHONES,
                            addressLabel = addressLabel(draft, locations),
                            addressError = invalidField == AdDraft.FIELD_LOCATION,
                            onEditPhones = { phones -> viewModel.updateDraft { it.copy(contactNumbers = phones) } },
                            onPickAddress = { addressPickerVisible = true },
                        )

                        PhotosSection(
                            existingImages = draft.images,
                            newImages = images,
                            videoLink = draft.videoLink,
                            videoError = invalidField == AdDraft.FIELD_VIDEO,
                            onAddImages = {
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            onRemoveExisting = viewModel::removeExistingImage,
                            onRemoveNew = viewModel::removeNewImage,
                            onVideoLinkChange = { link -> viewModel.updateDraft { it.copy(videoLink = link.trim()) } },
                        )

                        FormSection(title = stringResource(L10nR.string.create_section_main)) {
                            FormTextRow(
                                value = draft.title,
                                onValueChange = { value -> viewModel.updateDraft { it.copy(title = value.take(120)) } },
                                placeholder = stringResource(L10nR.string.create_title_hint),
                                isError = invalidField == AdDraft.FIELD_TITLE,
                                trailing = {
                                    // AI көмекшісі — атау бойынша сипаттама мен санатты толтырады.
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable(enabled = draft.title.isNotBlank() && !aiLoading) {
                                                viewModel.generateAiContent(AppLocale.fromTag(localeTag).tag)
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (aiLoading) {
                                            LoadingWidget(Modifier.size(20.dp))
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.AutoAwesome,
                                                contentDescription = stringResource(L10nR.string.ai_generate),
                                                tint = MaterialTheme.colorScheme.primary.copy(
                                                    alpha = if (draft.title.isNotBlank()) 1f else 0.4f,
                                                ),
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                    }
                                },
                            )
                            FormDivider()
                            FormSelectRow(
                                label = stringResource(L10nR.string.create_category),
                                value = categoryValue(grouped, subcategories, draft, localeTag)
                                    ?: stringResource(L10nR.string.create_choose_category),
                                valueIsPlaceholder = draft.categoryId == null,
                                isError = invalidField == AdDraft.FIELD_CATEGORY ||
                                    invalidField == AdDraft.FIELD_SUBCATEGORY,
                                onClick = { categoryPickerVisible = true },
                            )
                        }

                        FormSection(
                            title = stringResource(L10nR.string.create_section_description),
                            trailing = "${draft.description.length}/${formatThousands(MaxDescription)}",
                        ) {
                            FormTextRow(
                                value = draft.description,
                                onValueChange = { value ->
                                    viewModel.updateDraft { it.copy(description = value.take(MaxDescription)) }
                                },
                                placeholder = stringResource(L10nR.string.create_description_hint),
                                singleLine = false,
                                minHeight = 130.dp,
                                isError = invalidField == AdDraft.FIELD_DESCRIPTION,
                            )
                        }

                        FormSection(title = stringResource(L10nR.string.create_section_tags)) {
                            TagsRow(
                                keywords = draft.keywords,
                                onChange = { keywords -> viewModel.updateDraft { it.copy(keywords = keywords) } },
                            )
                        }

                        FormSection(title = stringResource(L10nR.string.create_section_price)) {
                            FormSegmented(
                                options = listOf(
                                    stringResource(L10nR.string.create_price_fixed),
                                    stringResource(L10nR.string.create_price_negotiable),
                                ),
                                selectedIndex = if (draft.negotiable) 1 else 0,
                                onSelect = { index -> viewModel.updateDraft { it.copy(negotiable = index == 1) } },
                                modifier = Modifier.padding(12.dp),
                            )
                            FormDivider()
                            FormTextRow(
                                value = draft.price,
                                onValueChange = { value ->
                                    viewModel.updateDraft { it.copy(price = value.filter(Char::isDigit).take(12)) }
                                },
                                placeholder = stringResource(L10nR.string.create_price_hint),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = invalidField == AdDraft.FIELD_PRICE,
                                trailing = {
                                    CurrencyMenu(
                                        selected = draft.currency,
                                        onSelect = { currency -> viewModel.updateDraft { it.copy(currency = currency) } },
                                    )
                                },
                            )
                        }

                        FormCard {
                            FormSelectRow(
                                label = stringResource(L10nR.string.create_unit),
                                value = draft.measurementUnit?.let { unitLabel(it) }
                                    ?: stringResource(L10nR.string.create_choose),
                                valueIsPlaceholder = true,
                                updown = true,
                                onClick = { unitPickerVisible = true },
                            )
                        }

                        if (isBusiness) {
                            SalesSection(
                                draft = draft,
                                zones = zones,
                                pickupError = invalidField == AdDraft.FIELD_PICKUP_ADDRESS,
                                onUpdate = viewModel::updateDraft,
                                onZonesChange = viewModel::setDeliveryZones,
                            )
                        }
                    }
                }

                if (!loading) {
                    CreateBottomBar {
                        if (showPreview) {
                            CreateBarButton(
                                text = stringResource(L10nR.string.ad_preview_edit),
                                icon = Icons.Rounded.Edit,
                                primary = false,
                                onClick = { showPreview = false },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            CreateBarButton(
                                text = stringResource(L10nR.string.ad_preview),
                                icon = Icons.Rounded.Visibility,
                                primary = false,
                                onClick = { validateThen { showPreview = true } },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        CreateBarButton(
                            text = stringResource(L10nR.string.create_publish),
                            icon = Icons.AutoMirrored.Rounded.Send,
                            loading = submitting,
                            onClick = { validateThen(viewModel::submit) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
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

    if (addressPickerVisible) {
        AddressPickerSheet(
            locations = locations,
            selectedId = draft.userLocationId,
            onPickSaved = { location ->
                viewModel.updateDraft { it.copy(userLocationId = location.id, location = null) }
                addressPickerVisible = false
            },
            onPickMap = {
                addressPickerVisible = false
                onOpenMapPicker(draft.location)
            },
            onDismiss = { addressPickerVisible = false },
        )
    }
}

/** Таңдалған мекенжайдың мәтіні: сақталған мекенжай → карта/каталог → null. */
private fun addressLabel(
    draft: AdDraft,
    locations: List<com.agroland.feature.profile.data.UserLocation>,
): String? {
    locations.firstOrNull { it.id == draft.userLocationId }?.fullAddress?.takeIf { it.isNotBlank() }?.let { return it }
    if (draft.userLocationId == null) {
        draft.location?.displayLabel()?.takeIf { it.isNotBlank() }?.let { return it }
    }
    return null
}

/** «Санат» мәні: «Категория · Сабкатегория» немесе тек категория. */
private fun categoryValue(
    grouped: Map<String, List<Category>>,
    subcategories: List<Category>,
    draft: AdDraft,
    localeTag: String?,
): String? {
    val category = categoryName(grouped, draft.categoryId, localeTag) ?: return null
    val sub = subcategories.firstOrNull { it.id == draft.subcategoryId }?.localizedName(localeTag)
    return if (sub != null) "$category · $sub" else category
}

internal fun categoryName(
    grouped: Map<String, List<Category>>,
    categoryId: Int?,
    localeTag: String?,
): String? {
    if (categoryId == null) return null
    return grouped.values.flatten().firstOrNull { it.id == categoryId }?.localizedName(localeTag)
}

/** 1000 → «1 000» (iOS санауышы). */
internal fun formatThousands(value: Int): String =
    value.toString().reversed().chunked(3).joinToString(" ").reversed()

/** Байланыс ақпараты: телефон нөмірлері + «Тағы қосу» + мекенжай. */
@Composable
private fun ContactSection(
    phones: List<String>,
    phonesError: Boolean,
    addressLabel: String?,
    addressError: Boolean,
    onEditPhones: (List<String>) -> Unit,
    onPickAddress: () -> Unit,
) {
    FormSection(title = stringResource(L10nR.string.create_section_contact)) {
        phones.forEachIndexed { index, phone ->
            FormTextRow(
                value = phone,
                onValueChange = { value ->
                    onEditPhones(phones.toMutableList().also { if (index < it.size) it[index] = value.take(20) })
                },
                placeholder = stringResource(L10nR.string.create_phone_hint),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = phonesError && phone.isBlank(),
                trailing = {
                    Icon(
                        imageVector = Icons.Rounded.RemoveCircleOutline,
                        contentDescription = stringResource(L10nR.string.common_delete),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable { onEditPhones(phones.filterIndexed { i, _ -> i != index }) },
                    )
                },
            )
            FormDivider()
        }
        FormActionRow(
            icon = Icons.Rounded.AddCircleOutline,
            text = stringResource(L10nR.string.create_add_more),
            onClick = { onEditPhones(phones + "") },
        )
        FormDivider(start = 54.dp)
        FormSelectRow(
            label = null,
            value = addressLabel ?: stringResource(L10nR.string.create_specify_address),
            valueIsPlaceholder = addressLabel == null,
            leadingIcon = Icons.Rounded.PinDrop,
            isError = addressError,
            onClick = onPickAddress,
        )
        if (phonesError && phones.none { it.isNotBlank() }) {
            Text(
                text = stringResource(L10nR.string.validation_phones),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
        }
    }
}

/** Жарнама фотосы: «Қосу» плиткасы + суреттер (n/10) + YouTube сілтемесі. */
@Composable
private fun PhotosSection(
    existingImages: List<String>,
    newImages: List<android.net.Uri>,
    videoLink: String,
    videoError: Boolean,
    onAddImages: () -> Unit,
    onRemoveExisting: (String) -> Unit,
    onRemoveNew: (android.net.Uri) -> Unit,
    onVideoLinkChange: (String) -> Unit,
) {
    val count = existingImages.size + newImages.size
    FormSection(
        title = stringResource(L10nR.string.create_section_photos),
        trailing = "$count/$MaxPhotos",
        footer = stringResource(L10nR.string.create_first_photo_hint),
    ) {
        LazyRow(
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (count < MaxPhotos) {
                item(key = "add") { AddPhotoTile(onClick = onAddImages) }
            }
            items(existingImages, key = { it }) { url ->
                PhotoTile(onRemove = { onRemoveExisting(url) }) {
                    CachedImage(url = url, contentDescription = null, modifier = Modifier.fillMaxSize())
                }
            }
            items(newImages, key = { it.toString() }) { uri ->
                PhotoTile(onRemove = { onRemoveNew(uri) }) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        FormDivider()
        FormTextRow(
            value = videoLink,
            onValueChange = onVideoLinkChange,
            placeholder = stringResource(L10nR.string.create_youtube_hint),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            isError = videoError,
        )
    }
}

/** Пунктирлі жасыл «Қосу» плиткасы. */
@Composable
private fun AddPhotoTile(onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(primary.copy(alpha = 0.08f))
            .drawBehind {
                drawRoundRect(
                    color = primary.copy(alpha = 0.55f),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                    ),
                )
            }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = primary, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(6.dp))
        Text(text = stringResource(L10nR.string.create_add_short), fontSize = 13.sp, color = primary)
    }
}

@Composable
private fun PhotoTile(onRemove: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(extendedColors().grey),
    ) {
        content()
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(5.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(L10nR.string.common_delete),
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/**
 * Тегтер — үтір арқылы бір жолда. Мәтін жергілікті сақталады (соңғы «, » жоғалмас
 * үшін), ал draft.keywords-ке таза тізім жазылады; өңдеу режимінде сырттан
 * келген тізім мәтінге синхрондалады.
 */
@Composable
private fun TagsRow(keywords: List<String>, onChange: (List<String>) -> Unit) {
    var text by remember { mutableStateOf(keywords.joinToString(", ")) }
    fun parse(value: String) = value.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    LaunchedEffect(keywords) {
        if (parse(text) != keywords) text = keywords.joinToString(", ")
    }
    FormTextRow(
        value = text,
        onValueChange = { value ->
            text = value.take(300)
            onChange(parse(text))
        },
        placeholder = stringResource(L10nR.string.create_tags_hint),
    )
}

/** Валюта белгісі (₸) — басқанда мәзірден өзгертіледі. */
@Composable
private fun CurrencyMenu(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Text(
            text = selected,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = extendedColors().secondaryText,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("₸", "$", "€", "₽").forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency) },
                    onClick = {
                        onSelect(currency)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Бизнес өрістері: жеткізу (+аймақтар), өзі алу (+мекенжай), SKU, қойма, себет, Agro Market. */
@Composable
private fun SalesSection(
    draft: AdDraft,
    zones: List<com.agroland.feature.marketplace.data.DeliveryZone>,
    pickupError: Boolean,
    onUpdate: ((AdDraft) -> AdDraft) -> Unit,
    onZonesChange: (List<Long>) -> Unit,
) {
    val ext = extendedColors()
    FormSection(title = stringResource(L10nR.string.create_section_sales)) {
        FormToggleRow(
            title = stringResource(L10nR.string.detail_delivery),
            checked = draft.deliveryAvailable,
            onToggle = { checked -> onUpdate { it.copy(deliveryAvailable = checked) } },
        )
        if (draft.deliveryAvailable && zones.isNotEmpty()) {
            zones.forEach { zone ->
                val checked = zone.id in draft.deliveryZoneIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onZonesChange(if (checked) draft.deliveryZoneIds - zone.id else draft.deliveryZoneIds + zone.id)
                        }
                        .padding(start = 32.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = zone.name ?: zone.regionName ?: "#${zone.id}",
                        fontSize = 15.sp,
                        color = ext.primaryText,
                        modifier = Modifier.weight(1f),
                    )
                    AgroCheckbox(
                        checked = checked,
                        onCheckedChange = { now ->
                            onZonesChange(if (now) draft.deliveryZoneIds + zone.id else draft.deliveryZoneIds - zone.id)
                        },
                    )
                }
            }
        }
        FormDivider()
        FormToggleRow(
            title = stringResource(L10nR.string.detail_pickup),
            checked = draft.pickupAvailable,
            onToggle = { checked -> onUpdate { it.copy(pickupAvailable = checked) } },
        )
        if (draft.pickupAvailable) {
            FormDivider()
            FormTextRow(
                value = draft.pickupAddress,
                onValueChange = { value -> onUpdate { it.copy(pickupAddress = value) } },
                placeholder = stringResource(L10nR.string.create_field_pickup_address),
                isError = pickupError,
            )
        }
        FormDivider()
        FormTextRow(
            value = draft.sku,
            onValueChange = { value -> onUpdate { it.copy(sku = value.take(64)) } },
            placeholder = stringResource(L10nR.string.create_field_sku),
        )
        FormDivider()
        FormTextRow(
            value = draft.stockQuantity,
            onValueChange = { value -> onUpdate { it.copy(stockQuantity = value.filter(Char::isDigit).take(9)) } },
            placeholder = stringResource(L10nR.string.create_field_stock),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        FormDivider()
        FormToggleRow(
            title = stringResource(L10nR.string.create_field_allow_cart),
            checked = draft.allowCart,
            onToggle = { checked -> onUpdate { it.copy(allowCart = checked) } },
        )
        FormDivider()
        FormToggleRow(
            title = stringResource(L10nR.string.create_field_marketplace),
            checked = draft.isMarketplace,
            onToggle = { checked -> onUpdate { it.copy(isMarketplace = checked) } },
        )
    }
}

/** Мекенжай таңдау парағы: сақталған мекенжайлар + «Карта арқылы таңдау». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddressPickerSheet(
    locations: List<com.agroland.feature.profile.data.UserLocation>,
    selectedId: Long?,
    onPickSaved: (com.agroland.feature.profile.data.UserLocation) -> Unit,
    onPickMap: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val ext = extendedColors()
    val primary = MaterialTheme.colorScheme.primary
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(L10nR.string.create_address),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = ext.primaryText,
                modifier = Modifier.padding(start = 4.dp),
            )
            FormCard {
                if (locations.isEmpty()) {
                    Text(
                        text = stringResource(L10nR.string.create_no_locations),
                        fontSize = 15.sp,
                        color = ext.secondaryText,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                locations.forEachIndexed { index, location ->
                    if (index > 0) FormDivider(start = 54.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPickSaved(location) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.PinDrop, contentDescription = null, tint = primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = location.fullAddress,
                            fontSize = 15.sp,
                            color = if (location.id == selectedId) primary else ext.primaryText,
                            fontWeight = if (location.id == selectedId) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            if (onPickMap != null) {
                FormCard {
                    FormActionRow(
                        icon = Icons.Rounded.Map,
                        text = stringResource(L10nR.string.create_pick_location_map),
                        onClick = onPickMap,
                    )
                }
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
            .clip(RoundedCornerShape(20.dp))
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
