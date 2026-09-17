package com.agroland.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Headset
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.common.settings.ThemeMode
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroIconButton
import com.agroland.core.ui.components.AgroListTile
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroSwitch
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.GuestGate
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.profile.data.UserProfile

/**
 * Профиль хабы (ProfilePage баламасы).
 * Қонақ → кіру ұсынысы; дилер әлі шарттарды қабылдаған жоқ → ендірілген DealerTermsPage.
 * Дилер/жеке секциялары: info, жарнама сандары, параметрлер, биометрия, тема (жеке), шығу.
 */
@Composable
fun ProfilePage(
    isAuthorized: Boolean,
    /** Артқа қайту — бет басты беттің аватарынан ашылады. */
    onBack: (() -> Unit)? = null,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onLoginClick: () -> Unit,
    onEditProfile: () -> Unit,
    onAddresses: () -> Unit,
    onCompanySettings: () -> Unit,
    onVerification: () -> Unit,
    onOpenWallet: () -> Unit = {},
    onOpenTransactions: () -> Unit = {},
    onOpenTopUp: () -> Unit = {},
    onMyAnnouncements: (String) -> Unit = {},
    /** «Сұраныстарым» — feature:demand тізімі (Фаза 17, спек қосымшасы). */
    onMyDemands: () -> Unit = {},
    /** «Менің пікірлерім» — spec §10 (Фаза 18). */
    onOpenMyReviews: () -> Unit = {},
    /** «Таңдаулылар» — басты беттің жоғарғы жолағы жеңілдетілгенде осында көшті. */
    onOpenFavorites: () -> Unit = {},
    onOpenSupportChat: () -> Unit = {},
    onDealerProducts: (Int) -> Unit = {},
    onDealerOrders: (Int) -> Unit = {},
    onDealerSettings: () -> Unit = {},
) {
    val viewModel = rememberProfileViewModel()
    val profile by viewModel.profile.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    val genericError = stringResource(L10nR.string.error_generic_message)
    val networkError = stringResource(L10nR.string.error_no_internet)
    val termsAcceptedText = stringResource(L10nR.string.profile_terms_accepted_toast)

    LaunchedEffect(Unit) {
        if (isAuthorized && profile == null) viewModel.refresh()
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.ShowError -> {
                    val text = event.error.backendMessage
                        ?: if (event.error.isNetwork) networkError else genericError
                    snackbar.showSnackbar(text)
                }
                ProfileEvent.TermsAccepted -> snackbar.showSnackbar(termsAcceptedText)
                ProfileEvent.LoggedOut -> Unit // Сессия Guest-ке өтеді, UI өзі қайта салынады.
                else -> Unit
            }
        }
    }

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(L10nR.string.profile_title),
                onBack = onBack,
                actions = {
                    if (isAuthorized) {
                        AgroIconButton(
                            icon = Icons.Outlined.Refresh,
                            contentDescription = null,
                            onClick = { viewModel.refresh() },
                        )
                    }
                },
            )
        },
    ) { modifier ->
        Box(modifier = modifier) {
            when {
                !isAuthorized -> GuestProfile(onLoginClick = onLoginClick)
                loading && profile == null -> LoadingWidget(Modifier.fillMaxSize())
                profile == null -> CenteredContent {
                    ErrorWithRetry(onRetry = { viewModel.refresh() })
                }
                else -> AuthorizedProfileContent(
                    profile = profile!!,
                    themeMode = themeMode,
                    onThemeChange = onThemeChange,
                    viewModel = viewModel,
                    onEditProfile = onEditProfile,
                    onAddresses = onAddresses,
                    onCompanySettings = onCompanySettings,
                    onVerification = onVerification,
                    onOpenFavorites = onOpenFavorites,
                    onOpenWallet = onOpenWallet,
                    onOpenTransactions = onOpenTransactions,
                    onOpenTopUp = onOpenTopUp,
                    onMyAnnouncements = onMyAnnouncements,
                    onMyDemands = onMyDemands,
                    onOpenMyReviews = onOpenMyReviews,
                    onOpenSupportChat = onOpenSupportChat,
                    onDealerProducts = onDealerProducts,
                    onDealerOrders = onDealerOrders,
                    onDealerSettings = onDealerSettings,
                )
            }
            SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** Қонақ күйі — кіруге ұсыныс (себет/чат қойындыларымен бірдей көрініс). */
@Composable
private fun GuestProfile(onLoginClick: () -> Unit) {
    GuestGate(
        icon = Icons.Outlined.Person,
        message = stringResource(L10nR.string.profile_login_prompt),
        loginText = stringResource(L10nR.string.auth_login_title),
        onLoginClick = onLoginClick,
    )
}

@Composable
private fun AuthorizedProfileContent(
    profile: UserProfile,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    viewModel: ProfileViewModel,
    onEditProfile: () -> Unit,
    onAddresses: () -> Unit,
    onCompanySettings: () -> Unit,
    onVerification: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenTopUp: () -> Unit,
    onMyAnnouncements: (String) -> Unit,
    onMyDemands: () -> Unit,
    onOpenMyReviews: () -> Unit,
    onOpenSupportChat: () -> Unit,
    onDealerProducts: (Int) -> Unit,
    onDealerOrders: (Int) -> Unit,
    onDealerSettings: () -> Unit,
) {
    val isDealer = profile.userType.equals("dealer", ignoreCase = true) ||
        profile.userType.equals("business", ignoreCase = true)

    // DealerTerms гейт: шарттар қабылданбаған болса — ендірілген DealerTermsPage.
    if (isDealer && !profile.dealerTermsAccepted) {
        DealerTermsPage(standalone = false)
        return
    }

    val context = LocalContext.current
    val bioLockEnabled = viewModel.pinManager.biometricLockEnabled
    val isBioAvailable = remember(isDealer) {
        (context as? androidx.fragment.app.FragmentActivity)?.let {
            viewModel.isBiometricAvailable(it)
        } ?: false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ProfileInfoCard(profile) }
        item { ProfileWalletCard(balance = profile.balance, onOpenTopUp = onOpenTopUp) }
        item { ProfileAdsCard(profile, onMyAnnouncements) }
        // ── Дилер консолі — Фаза 16 (Flutter DealerSection): тауарлар хабы,
        // тапсырыстар және бизнес баптаулары (ҚҚС). ──
        if (isDealer) {
            item {
                ProfileSectionCard(title = null) {
                    AgroListTile(
                        title = stringResource(L10nR.string.dealer_my_products),
                        leading = { SectionIcon(Icons.Outlined.Inventory2) },
                        onClick = { onDealerProducts(0) },
                    )
                    AgroListTile(
                        title = stringResource(L10nR.string.dealer_my_orders),
                        leading = { SectionIcon(Icons.Outlined.ShoppingBag) },
                        onClick = { onDealerOrders(0) },
                    )
                    AgroListTile(
                        title = stringResource(L10nR.string.dealer_settings),
                        leading = { SectionIcon(Icons.Outlined.Tune) },
                        onClick = onDealerSettings,
                    )
                }
            }
        }
        item {
            ProfileSectionCard(title = null) {
                AgroListTile(
                    title = stringResource(L10nR.string.profile_edit),
                    leading = { SectionIcon(Icons.Outlined.Person) },
                    onClick = onEditProfile,
                )
                AgroListTile(
                    title = stringResource(L10nR.string.profile_addresses),
                    subtitle = profile.locations.firstOrNull()?.fullAddress?.takeIf { it.isNotBlank() },
                    leading = { SectionIcon(Icons.Outlined.LocationOn) },
                    onClick = onAddresses,
                )
                AgroListTile(
                    title = stringResource(L10nR.string.profile_verification),
                    leading = { SectionIcon(Icons.Outlined.Verified) },
                    onClick = onVerification,
                )
                // «Таңдаулылар» — Flutter-дегідей профильден ашылады.
                AgroListTile(
                    title = stringResource(L10nR.string.favorites_title),
                    leading = { SectionIcon(Icons.Outlined.FavoriteBorder) },
                    onClick = onOpenFavorites,
                )
                // «Сұраныстарым» — өз сұраныстарын басқару (Фаза 17).
                AgroListTile(
                    title = stringResource(L10nR.string.demand_list_title),
                    leading = { SectionIcon(Icons.Outlined.PostAdd) },
                    onClick = onMyDemands,
                )
                // «Менің пікірлерім» — қалдырылмаған/қалдырылған (Фаза 18, spec §10).
                AgroListTile(
                    title = stringResource(L10nR.string.my_reviews_title),
                    leading = { SectionIcon(Icons.Outlined.StarBorder) },
                    onClick = onOpenMyReviews,
                )
                if (isDealer) {
                    AgroListTile(
                        title = stringResource(L10nR.string.profile_company_settings),
                        subtitle = profile.company?.name,
                        leading = { SectionIcon(Icons.Outlined.Badge) },
                        onClick = onCompanySettings,
                    )
                    AgroListTile(
                        title = stringResource(L10nR.string.wallet_title),
                        leading = { SectionIcon(Icons.Outlined.AccountBalanceWallet) },
                        onClick = onOpenWallet,
                    )
                }
                AgroListTile(
                    title = stringResource(L10nR.string.wallet_transaction_history),
                    leading = { SectionIcon(Icons.Outlined.ReceiptLong) },
                    onClick = onOpenTransactions,
                )
            }
        }
        if (isBioAvailable && viewModel.pinManager.isPinSet) {
            item {
                ProfileSectionCard(title = null) {
                    AgroListTile(
                        title = stringResource(L10nR.string.profile_biometric_lock),
                        leading = { SectionIcon(Icons.Outlined.Verified) },
                        trailing = {
                            AgroSwitch(
                                checked = bioLockEnabled,
                                onCheckedChange = { viewModel.setBiometricLockEnabled(it) },
                            )
                        },
                        onClick = null,
                    )
                }
            }
        }
        if (!isDealer) {
            item { ProfileThemeCard(themeMode, onThemeChange) }
        }
        // Қолдау чаты (Flutter ProfileSupport / DealerSupportSection): жүйелік
        // чат 31, басқа секция тәуелсіз — барлық авторизацияланған қолданушыға.
        item {
            ProfileSectionCard(title = null) {
                AgroListTile(
                    title = stringResource(L10nR.string.support),
                    leading = { SectionIcon(Icons.Outlined.Headset) },
                    onClick = onOpenSupportChat,
                )
            }
        }
        item {
            ProfileSectionCard(title = null) {
                AgroListTile(
                    title = stringResource(L10nR.string.auth_logout),
                    leading = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    onClick = { viewModel.logout() },
                )
            }
        }
    }
}

/**
 * Әмиян қысқаша картасы (Flutter profile_info): баланс + «Әмиянды толтыру»
 * батырмасы — барлық авторизацияланған қолданушыға көрінеді.
 */
@Composable
private fun ProfileWalletCard(balance: Double, onOpenTopUp: () -> Unit) {
    val ext = extendedColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.card)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(L10nR.string.wallet_your_balance),
            style = MaterialTheme.typography.bodySmall,
            color = ext.secondaryText,
        )
        Text(
            text = PriceFormatter.format(balance),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = ext.primaryText,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )
        AgroButton(
            text = stringResource(L10nR.string.wallet_replenish),
            onClick = onOpenTopUp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Профиль картасы: avatar + аты + телефон + күй белгілері. */
@Composable
private fun ProfileInfoCard(profile: UserProfile) {
    val ext = extendedColors()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.card)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ext.grey),
                contentAlignment = Alignment.Center,
            ) {
                CachedImage(
                    url = profile.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                )
                if (profile.avatarUrl == null) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = ext.secondaryText,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name ?: stringResource(L10nR.string.guest),
                    style = MaterialTheme.typography.titleSmall,
                    color = ext.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!profile.phone.isNullOrBlank()) {
                    Text(
                        text = profile.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.secondaryText,
                        maxLines = 1,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                    if (profile.isVerified) {
                        StatusPill(text = stringResource(L10nR.string.profile_verified))
                    }
                    if (profile.isVipSeller) {
                        StatusPill(text = stringResource(L10nR.string.profile_vip))
                    }
                    if (profile.isVatPayer) {
                        StatusPill(text = stringResource(L10nR.string.profile_vat))
                    }
                }
            }
        }
    }
}

/** Жарнама сандары — profile.announcements; плиткалар тиісті статус бетіне ашады. */
@Composable
private fun ProfileAdsCard(profile: UserProfile, onOpenStatus: (String) -> Unit) {
    val counts = profile.announcements ?: return
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AgroListTile(
            title = stringResource(L10nR.string.my_ads_title),
            leading = { SectionIcon(Icons.Outlined.Campaign) },
            onClick = { onOpenStatus("active") },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AdCountTile(
                icon = Icons.Outlined.Campaign,
                label = stringResource(L10nR.string.profile_ads_active),
                value = counts.active,
                modifier = Modifier.weight(1f).clickable { onOpenStatus("active") },
            )
            AdCountTile(
                icon = Icons.Outlined.Refresh,
                label = stringResource(L10nR.string.profile_ads_pending),
                value = counts.pending,
                modifier = Modifier.weight(1f).clickable { onOpenStatus("pending") },
            )
        }
    }
}

@Composable
private fun AdCountTile(icon: ImageVector, label: String, value: Int, modifier: Modifier = Modifier) {
    val ext = extendedColors()
    Row(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(ext.grey).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = ext.primaryText,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = ext.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProfileThemeCard(themeMode: ThemeMode, onThemeChange: (ThemeMode) -> Unit) {
    ProfileSectionCard(title = stringResource(L10nR.string.profile_theme)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeChip(
                text = stringResource(L10nR.string.theme_system),
                selected = themeMode == ThemeMode.SYSTEM,
                onClick = { onThemeChange(ThemeMode.SYSTEM) },
                modifier = Modifier.weight(1f),
            )
            ThemeChip(
                text = stringResource(L10nR.string.theme_light),
                selected = themeMode == ThemeMode.LIGHT,
                onClick = { onThemeChange(ThemeMode.LIGHT) },
                modifier = Modifier.weight(1f),
            )
            ThemeChip(
                text = stringResource(L10nR.string.theme_dark),
                selected = themeMode == ThemeMode.DARK,
                onClick = { onThemeChange(ThemeMode.DARK) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Секция картасы — заголовоксыз тақтай тобы. */
@Composable
internal fun ProfileSectionCard(
    title: String?,
    content: @Composable () -> Unit,
) {
    val ext = extendedColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.card),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = ext.secondaryText,
                modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp),
            )
        }
        content()
    }
}

@Composable
private fun SectionIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = extendedColors().secondaryText,
        modifier = Modifier.size(24.dp),
    )
}

@Composable
private fun StatusPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(extendedColors().backgroundLight)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
    }
}

@Composable
private fun ThemeChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ext = extendedColors()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else ext.grey)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) ext.white else ext.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}