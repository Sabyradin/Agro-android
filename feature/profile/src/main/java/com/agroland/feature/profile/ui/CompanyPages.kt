package com.agroland.feature.profile.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroListTile
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroTextField
import com.agroland.core.ui.components.BottomActionContainer
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.profile.data.MultipartHelper
import kotlinx.coroutines.launch

/**
 * Компания параметрлері (CompanySettingsPage) — 4 саб-бетке навигация хабы:
 * өкіл (representative), туралы (about), байланыстар (contacts), безендіру (decor).
 * Барлық эндпоинт: POST /user/profile/company/{section}.
 */
@Composable
fun CompanySettingsPage(
    onBack: () -> Unit,
    onOpenSection: (CompanySection) -> Unit,
) {
    val viewModel = rememberProfileViewModel()
    val profile by viewModel.profile.collectAsState()
    val company = profile?.company

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(L10nR.string.profile_company_settings),
                onBack = onBack,
            )
        },
    ) { modifier ->
        Column(modifier = modifier.padding(16.dp)) {
            company?.name?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = extendedColors().primaryText,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            SectionTile(
                title = stringResource(L10nR.string.company_data_title),
                subtitle = company?.representativeName,
                icon = Icons.Rounded.Badge,
                onClick = { onOpenSection(CompanySection.DATA) },
            )
            SectionTile(
                title = stringResource(L10nR.string.company_about_title),
                subtitle = company?.about,
                icon = Icons.AutoMirrored.Rounded.Article,
                onClick = { onOpenSection(CompanySection.ABOUT) },
            )
            SectionTile(
                title = stringResource(L10nR.string.company_contacts_title),
                subtitle = company?.phone,
                icon = Icons.Rounded.Phone,
                onClick = { onOpenSection(CompanySection.CONTACTS) },
            )
            SectionTile(
                title = stringResource(L10nR.string.company_decor_title),
                icon = Icons.Rounded.Image,
                onClick = { onOpenSection(CompanySection.DECOR) },
            )
        }
    }
}

/** Компания саб-бетінің түрі — навигация маршруты үшін. */
enum class CompanySection { DATA, ABOUT, CONTACTS, DECOR }

/** Секция диспетчері — маршруттан келген section осында экранға айналады. */
@Composable
fun CompanySectionPage(section: CompanySection, onBack: () -> Unit) {
    when (section) {
        CompanySection.DATA -> CompanyDataPage(onBack)
        CompanySection.ABOUT -> CompanyAboutPage(onBack)
        CompanySection.CONTACTS -> CompanyContactsPage(onBack)
        CompanySection.DECOR -> CompanyDecorationPage(onBack)
    }
}

@Composable
private fun SectionTile(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    val ext = extendedColors()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ext.card),
    ) {
        AgroListTile(
            title = title,
            subtitle = subtitle,
            leading = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            },
            onClick = onClick,
        )
    }
}

/** Компания өкілі — POST /user/profile/company/representative {name, position}. */
@Composable
private fun CompanyDataPage(onBack: () -> Unit) {
    val viewModel = rememberProfileViewModel()
    val profile by viewModel.profile.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var name by remember(profile?.id) { mutableStateOf(profile?.company?.representativeName ?: "") }
    var position by remember(profile?.id) { mutableStateOf(profile?.company?.representativePosition ?: "") }
    var nameError by remember { mutableStateOf(false) }

    val genericError = stringResource(L10nR.string.error_generic_message)
    val networkError = stringResource(L10nR.string.error_no_internet)
    val nameEmpty = stringResource(L10nR.string.auth_name_error)
    val positionEmpty = stringResource(L10nR.string.company_position_error)

    LaunchedEffect(Unit) {
        if (profile == null) viewModel.refresh()
        viewModel.events.collect { event ->
            if (event is ProfileEvent.ShowError) {
                snackbar.showSnackbar(
                    event.error.backendMessage ?: if (event.error.isNetwork) networkError else genericError,
                )
            }
        }
    }

    CompanyFormScaffold(
        title = stringResource(L10nR.string.company_data_title),
        onBack = onBack,
        snackbar = snackbar,
        saving = saving,
        onSave = {
            if (name.isBlank()) {
                nameError = true
            } else {
                viewModel.saveCompanyRepresentative(name.trim(), position.trim())
            }
        },
    ) {
        AgroTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = false
            },
            label = stringResource(L10nR.string.company_rep_name_hint),
            isError = nameError,
            errorText = nameEmpty,
        )
        AgroTextField(
            value = position,
            onValueChange = { position = it },
            label = stringResource(L10nR.string.company_rep_position_hint),
            supportingText = positionEmpty,
        )
    }
}

/** Компания туралы — POST /user/profile/company/about {text}. */
@Composable
private fun CompanyAboutPage(onBack: () -> Unit) {
    val viewModel = rememberProfileViewModel()
    val profile by viewModel.profile.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var text by remember(profile?.id) { mutableStateOf(profile?.company?.about ?: "") }

    val genericError = stringResource(L10nR.string.error_generic_message)
    val networkError = stringResource(L10nR.string.error_no_internet)

    LaunchedEffect(Unit) {
        if (profile == null) viewModel.refresh()
        viewModel.events.collect { event ->
            if (event is ProfileEvent.ShowError) {
                snackbar.showSnackbar(
                    event.error.backendMessage ?: if (event.error.isNetwork) networkError else genericError,
                )
            }
        }
    }

    CompanyFormScaffold(
        title = stringResource(L10nR.string.company_about_title),
        onBack = onBack,
        snackbar = snackbar,
        saving = saving,
        onSave = { viewModel.saveCompanyAbout(text.trim()) },
    ) {
        AgroTextField(
            value = text,
            onValueChange = { text = it },
            label = stringResource(L10nR.string.company_about_hint),
            singleLine = false,
        )
    }
}

/** Байланыстар — POST /user/profile/company/contacts {phone?, website?, telegram?, whatsapp?}. */
@Composable
private fun CompanyContactsPage(onBack: () -> Unit) {
    val viewModel = rememberProfileViewModel()
    val profile by viewModel.profile.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    val company = profile?.company
    var phone by remember(profile?.id) { mutableStateOf(company?.phone ?: "") }
    var website by remember(profile?.id) { mutableStateOf(company?.website ?: "") }
    var telegram by remember(profile?.id) { mutableStateOf(company?.telegram ?: "") }
    var whatsapp by remember(profile?.id) { mutableStateOf(company?.whatsapp ?: "") }

    val genericError = stringResource(L10nR.string.error_generic_message)
    val networkError = stringResource(L10nR.string.error_no_internet)

    LaunchedEffect(Unit) {
        if (profile == null) viewModel.refresh()
        viewModel.events.collect { event ->
            if (event is ProfileEvent.ShowError) {
                snackbar.showSnackbar(
                    event.error.backendMessage ?: if (event.error.isNetwork) networkError else genericError,
                )
            }
        }
    }

    CompanyFormScaffold(
        title = stringResource(L10nR.string.company_contacts_title),
        onBack = onBack,
        snackbar = snackbar,
        saving = saving,
        onSave = { viewModel.saveCompanyContacts(phone, website, telegram, whatsapp) },
    ) {
        AgroTextField(
            value = phone,
            onValueChange = { phone = it },
            label = stringResource(L10nR.string.company_phone_hint),
        )
        AgroTextField(
            value = website,
            onValueChange = { website = it },
            label = stringResource(L10nR.string.company_website_hint),
        )
        AgroTextField(
            value = telegram,
            onValueChange = { telegram = it },
            label = stringResource(L10nR.string.company_telegram_hint),
        )
        AgroTextField(
            value = whatsapp,
            onValueChange = { whatsapp = it },
            label = stringResource(L10nR.string.company_whatsapp_hint),
        )
    }
}

/** Безендіру — multipart logo + banner (POST /user/profile/company/decor). */
@Composable
private fun CompanyDecorationPage(onBack: () -> Unit) {
    val viewModel = rememberProfileViewModel()
    val profile by viewModel.profile.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingLogo by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingBanner by remember { mutableStateOf<android.net.Uri?>(null) }

    val genericError = stringResource(L10nR.string.error_generic_message)
    val networkError = stringResource(L10nR.string.error_no_internet)
    val pickFail = stringResource(L10nR.string.verification_file_error)

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        pendingLogo = uri
    }
    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        pendingBanner = uri
    }

    LaunchedEffect(Unit) {
        if (profile == null) viewModel.refresh()
        viewModel.events.collect { event ->
            if (event is ProfileEvent.ShowError) {
                snackbar.showSnackbar(
                    event.error.backendMessage ?: if (event.error.isNetwork) networkError else genericError,
                )
            }
        }
    }

    CompanyFormScaffold(
        title = stringResource(L10nR.string.company_decor_title),
        onBack = onBack,
        snackbar = snackbar,
        saving = saving,
        onSave = {
            scope.launch {
                val logoPart = pendingLogo?.let { MultipartHelper.toPart(context, it, "logo") }
                val bannerPart = pendingBanner?.let { MultipartHelper.toPart(context, it, "banner") }
                if (logoPart == null && bannerPart == null &&
                    pendingLogo == null && pendingBanner == null
                ) {
                    // Ештеңе таңдалмаған — бос жібермей-ақ қайтамыз.
                } else if ((pendingLogo != null && logoPart == null) || (pendingBanner != null && bannerPart == null)) {
                    snackbar.showSnackbar(pickFail)
                } else {
                    viewModel.saveCompanyDecor(logoPart, bannerPart)
                }
            }
        },
    ) {
        DecorPickerRow(
            label = stringResource(L10nR.string.company_decor_logo),
            currentUrl = profile?.company?.logoUrl,
            pendingUri = pendingLogo,
            onPick = { logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        )
        DecorPickerRow(
            label = stringResource(L10nR.string.company_decor_banner),
            currentUrl = profile?.company?.bannerUrl,
            pendingUri = pendingBanner,
            onPick = { bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        )
    }
}

@Composable
private fun DecorPickerRow(
    label: String,
    currentUrl: String?,
    pendingUri: android.net.Uri?,
    onPick: () -> Unit,
) {
    val ext = extendedColors()
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ext.secondaryText,
        )
        Box(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ext.grey)
                .clickable(onClick = onPick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (pendingUri != null) {
                Icon(
                    imageVector = Icons.Rounded.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            } else {
                CachedImage(
                    url = currentUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
                if (currentUrl == null) {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = null,
                        tint = ext.secondaryText,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}

/** Ортақ форма қаңқасы: AppBar + прокрутка + төменде Сақтау + snackbar. */
@Composable
private fun CompanyFormScaffold(
    title: String,
    onBack: () -> Unit,
    snackbar: SnackbarHostState,
    saving: Boolean,
    onSave: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    AgroScaffold(
        topBar = { AgroAppBar(title = title, onBack = onBack) },
    ) { modifier ->
        Box(modifier = modifier) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                content()
            }
            BottomActionContainer(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                AgroButton(
                    text = stringResource(L10nR.string.common_save),
                    onClick = onSave,
                    enabled = !saving,
                    loading = saving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}