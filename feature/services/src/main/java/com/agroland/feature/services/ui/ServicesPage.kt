package com.agroland.feature.services.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroIconCircle
import com.agroland.core.ui.components.AgroSearchField
import com.agroland.core.ui.components.shellBottomPadding
import com.agroland.core.ui.theme.AgroSpacing
import com.agroland.core.ui.theme.extendedColors

/** Сервистер бетінің жолы: иконка + атау (+ сипаттама). */
private data class ServiceItem(
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int,
    val isEgov: Boolean = false,
)

/** Серіктес карточкасы: түс + инициал + сипаттамасы. */
private data class PartnerItem(
    val name: String,
    val descriptionRes: Int,
    val color: Color,
    val initial: String,
    val isEgov: Boolean = false,
)

/**
 * «Сервистер» — shell 5-табы (iOS макеті): іздеу өрісі, «Agro Git» ЖИ-көмекші
 * баннері, 4-бағаналы сервистер grid-і, серіктестер және «Сұраныс» карточкасы.
 * PROD-та серіктестерден тек eGov көрінеді.
 */
@Composable
fun ServicesPage(
    onOpenEgov: () -> Unit,
    /** «Agro Git» — ЖИ-гид чаты (жүйелік пайдаланушы 1003). */
    onOpenAgroGit: () -> Unit = {},
    /** «Сұраныс» — жаңа сұраныс құру. */
    onOpenDemand: () -> Unit = {},
) {
    val ext = extendedColors()
    // dev flavor applicationId «com.agroland.app.dev» — PROD-та тек eGov серіктесі.
    val isDev = LocalContext.current.packageName.endsWith(".dev")
    var query by rememberSaveable { mutableStateOf("") }

    val services = remember {
        listOf(
            ServiceItem(Icons.Outlined.Calculate, L10nR.string.service_tile_accounting, L10nR.string.service_tile_accounting_desc),
            ServiceItem(Icons.Outlined.Gavel, L10nR.string.service_tile_legal, L10nR.string.service_tile_legal_desc),
            ServiceItem(Icons.Outlined.AccountBalance, L10nR.string.service_tile_egov, L10nR.string.service_tile_egov_desc, isEgov = true),
            ServiceItem(Icons.Outlined.Computer, L10nR.string.service_tile_it, L10nR.string.service_tile_it_desc),
            ServiceItem(Icons.Outlined.LocalShipping, L10nR.string.service_tile_china_import, L10nR.string.service_tile_china_import_desc),
            ServiceItem(Icons.Outlined.QueryStats, L10nR.string.service_tile_marketing, L10nR.string.service_tile_marketing_desc),
            ServiceItem(Icons.Outlined.Agriculture, L10nR.string.service_tile_agro_consulting, L10nR.string.service_tile_agro_consulting_desc),
            ServiceItem(Icons.Outlined.Engineering, L10nR.string.service_tile_engineering, L10nR.string.service_tile_engineering_desc),
            ServiceItem(Icons.Outlined.LocalFlorist, L10nR.string.service_tile_plant_protection, L10nR.string.service_tile_plant_protection_desc),
            ServiceItem(Icons.Outlined.ShoppingBag, L10nR.string.service_tile_agro_market, L10nR.string.service_tile_agro_market_desc),
            ServiceItem(Icons.Outlined.MenuBook, L10nR.string.service_tile_agro_education, L10nR.string.service_tile_agro_education_desc),
        )
    }
    val partners = remember {
        listOf(
            PartnerItem("Egov", L10nR.string.service_partner_egov_desc, Color(0xFF1B7AC4), "E", isEgov = true),
            PartnerItem("Halyk Bank", L10nR.string.service_partner_halyk_desc, Color(0xFF00A551), "H"),
            PartnerItem("MercuryX", L10nR.string.service_partner_mercury_desc, Color(0xFF6C2BD9), "M"),
        )
    }

    // stringResource @Composable — фильтр lambda-сынан шақырылмайды, мәтіндерді алдын ала шығарамыз.
    val titleTexts = services.associate { it.titleRes to stringResource(it.titleRes) }
    val filtered = if (query.isEmpty()) services else {
        services.filter { (titleTexts[it.titleRes] ?: "").contains(query, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = shellBottomPadding()),
    ) {
        // Жоғарғы жолақ: бет атауы + іздеу (статус-барға кірмейді).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ext.card)
                .statusBarsPadding()
                .padding(horizontal = AgroSpacing.screen, vertical = AgroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.md),
        ) {
            Text(
                text = stringResource(L10nR.string.tab_services),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ext.primaryText,
            )
            AgroSearchField(
                value = query,
                onValueChange = { query = it },
                hint = stringResource(L10nR.string.search_hint),
            )
        }
        Spacer(Modifier.height(AgroSpacing.screen))

        if (query.isEmpty()) {
            AgroGitBanner(
                onClick = onOpenAgroGit,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(AgroSpacing.screen))
        }

        if (filtered.isEmpty()) {
            Text(
                text = stringResource(L10nR.string.egov_no_service),
                style = MaterialTheme.typography.bodyMedium,
                color = ext.secondaryText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                textAlign = TextAlign.Center,
            )
        } else {
            // 4-бағаналы grid (iOS): атаулар 2 жолға дейін, сөз ортасынан үзілмейді.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                filtered.chunked(ServiceGridColumns).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowItems.forEach { service ->
                            ServiceTile(
                                service = service,
                                onClick = { if (service.isEgov) onOpenEgov() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(ServiceTileHeight),
                            )
                        }
                        repeat(ServiceGridColumns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(L10nR.string.service_partners_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ext.primaryText,
                modifier = Modifier.weight(1f),
            )
            // PROD-та «Барлығы» әлі жұмыс істемейді — тек dev-те көрінеді (Flutter parity).
            if (isDev) {
                Text(
                    text = stringResource(L10nR.string.services_view_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // PROD-та тек жұмыс істейтін серіктес — eGov.
            partners
                .filter { isDev || it.isEgov }
                .forEach { partner ->
                    PartnerCard(partner = partner, onClick = { if (partner.isEgov) onOpenEgov() })
                }
            Spacer(Modifier.height(6.dp))
            PartnerCard(
                partner = PartnerItem(
                    name = stringResource(L10nR.string.services_demand_title),
                    descriptionRes = L10nR.string.services_demand_desc,
                    color = DemandOrange,
                    initial = "",
                ),
                icon = Icons.Outlined.Campaign,
                onClick = onOpenDemand,
            )
        }
    }
}

/** Grid бақандарының саны. */
private const val ServiceGridColumns = 4

/** Барлық плитка бірдей биіктікте — жолдар «сатылап» кетпейді. */
private val ServiceTileHeight = 104.dp

/** Сервис плиткасы — дөңгелек иконка + атау. */
@Composable
private fun ServiceTile(
    service: ServiceItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ext = extendedColors()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = AgroSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AgroIconCircle(
            icon = service.icon,
            size = 56.dp,
            iconSize = 26.dp,
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        )
        Spacer(Modifier.height(AgroSpacing.sm))
        // Тек атау: 4 бақанада сипаттама бәрібір «...» болып қиылатын,
        // ал биіктік жетпей мәтіннің асты кесілетін.
        Text(
            // Дефистен кейін жол бөлінсін («Агро-» / «консалтинг»), сөз ортасынан емес.
            text = stringResource(service.titleRes).replace("-", "-​"),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = ext.primaryText,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp,
        )
    }
}

/** Серіктес карточкасы — түсті инициал блогы + атау + сипаттама + chevron. */
@Composable
private fun PartnerCard(
    partner: PartnerItem,
    onClick: () -> Unit,
    /** Инициал орнына иконка (мысалы, «Сұраныс»). */
    icon: ImageVector? = null,
) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.card)
            .border(1.dp, ext.divider, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(partner.color),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            } else {
                Text(
                    text = partner.initial,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = partner.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ext.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(partner.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = ext.secondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = ext.secondaryText,
        )
    }
}

/** «Сұраныс» карточкасының қызғылт сары түсі (iOS). */
private val DemandOrange = Color(0xFFF7931E)

/** «Agro Git» баннері — жасыл градиент, жұлдызша иконка, атау + сипаттама + chevron. */
@Composable
private fun AgroGitBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF3F9A3A), Color(0xFF8CC63F))))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(L10nR.string.services_agro_git_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(L10nR.string.services_agro_git_desc),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White,
        )
    }
}
