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
 * «Сервистер» — shell 5-табы (Flutter ServicesPage, 1:1): іздеу өрісі,
 * 4-бақаналы сервистер grid-і, PROD-та тек eGov серіктесі көрінеді.
 */
@Composable
fun ServicesPage(
    onOpenEgov: () -> Unit,
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
            // 3-бақаналы grid: 4 бақанада плитка ені ~80dp болып, ұзын
            // атаулар («Агро-консалтинг») сөздің ортасынан үзілетін.
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
        }
    }
}

/** Grid бақандарының саны. */
private const val ServiceGridColumns = 3

/** Барлық плитка бірдей биіктікте — жолдар «сатылап» кетпейді. */
private val ServiceTileHeight = 108.dp

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
            size = 52.dp,
            iconSize = 26.dp,
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        )
        Spacer(Modifier.height(AgroSpacing.sm))
        // Тек атау: 4 бақанада сипаттама бәрібір «...» болып қиылатын,
        // ал биіктік жетпей мәтіннің асты кесілетін.
        Text(
            text = stringResource(service.titleRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
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
            Text(
                text = partner.initial,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
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