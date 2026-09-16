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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.promo.data.AnnouncementPromotion
import com.agroland.feature.promo.data.PromoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Promotion табы — Flutter DealerPromotionTab, бірақ TODO stub-тың орнына
 * нақты деректер: белсенді промолар GET /user/announcements/promotions
 * арқылы (hasPromotion фильтрі). Пакет карталары (dev қана) жарнаманы
 * таңдау пикеріне апарады — сатып алу PROD-та жасырылған (Flutter kIsProd).
 */
@HiltViewModel
class DealerPromotionViewModel @Inject constructor(
    private val promoRepository: PromoRepository,
) : ViewModel() {

    private val _promotions = MutableStateFlow<List<AnnouncementPromotion>>(emptyList())
    val promotions = _promotions.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            when (val result = promoRepository.getUserAnnouncementsPromotions()) {
                is com.agroland.core.network.ApiResult.Success ->
                    _promotions.value = result.value.filter { it.hasPromotion && it.promotion != null }
                is com.agroland.core.network.ApiResult.Error -> _promotions.value = emptyList()
            }
            _loading.value = false
        }
    }
}

@Composable
fun DealerPromotionTab(
    onPickAnnouncement: () -> Unit,
    viewModel: DealerPromotionViewModel = hiltViewModel(),
) {
    val promotions by viewModel.promotions.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val isDev = LocalContext.current.packageName.endsWith(".dev")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Белсенді промолар (нақты деректер) ──
        item {
            Text(
                text = stringResource(L10nR.string.dealer_active_promotions),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = extendedColors().primaryText,
            )
        }
        if (loading) {
            item { LoadingWidget(modifier = Modifier.fillMaxWidth()) }
        } else if (promotions.isEmpty()) {
            item {
                val ext = extendedColors()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ext.card)
                        .border(0.5.dp, ext.divider, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Flag,
                        contentDescription = null,
                        tint = ext.secondaryText,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(L10nR.string.dealer_no_active_promotions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ext.secondaryText,
                    )
                }
            }
        } else {
            promotions.forEach { promo ->
                item(key = promo.announcementId) {
                    ActivePromotionCard(promo)
                }
            }
        }

        // ── Пакет карталары — dev flavor қана (PROD-та жасырылған) ──
        if (isDev) {
            item {
                Text(
                    text = stringResource(L10nR.string.advertise),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors().primaryText,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item {
                PackageCard(
                    icon = Icons.Outlined.ArrowUpward,
                    title = stringResource(L10nR.string.dealer_boost_to_top),
                    subtitle = stringResource(L10nR.string.dealer_active_days, "7"),
                    price = "500 ₸",
                    color = Color(0xFF2196F3),
                    onBuy = onPickAnnouncement,
                )
            }
            item {
                PackageCard(
                    icon = Icons.Outlined.Star,
                    title = "VIP",
                    subtitle = stringResource(L10nR.string.dealer_active_days, "30"),
                    price = "2 000 ₸",
                    color = Color(0xFFFF9800),
                    onBuy = onPickAnnouncement,
                )
            }
            item {
                PackageCard(
                    icon = Icons.Outlined.MilitaryTech,
                    title = "Premium",
                    subtitle = stringResource(L10nR.string.dealer_coverage_3x),
                    price = "5 000 ₸",
                    color = Color(0xFF9C27B0),
                    onBuy = onPickAnnouncement,
                )
            }
        }
    }
}

/** Белсенді промо карточкасы — сурет + атау + пакет + прогресс жолағы. */
@Composable
private fun ActivePromotionCard(item: AnnouncementPromotion) {
    val ext = extendedColors()
    val promotion = item.promotion ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ext.card)
            .border(0.5.dp, ext.divider, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ext.grey),
            contentAlignment = Alignment.Center,
        ) {
            item.announcementImageUrl?.let {
                CachedImage(url = it, contentDescription = null, modifier = Modifier.size(56.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.announcementTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.W600,
                    color = ext.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = promotion.packageType.replaceFirstChar { it.uppercase() },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFA000),
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { promotion.progressPercentage / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = ext.grey,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = promotion.progressPercentage.toString() + "% · " +
                    stringResource(L10nR.string.dealer_active_days, promotion.remainingTimeDays?.toInt()?.toString() ?: "0"),
                style = MaterialTheme.typography.labelSmall,
                color = ext.secondaryText,
                maxLines = 1,
            )
        }
    }
}

/** Пакет картасы — иконка + атау + баға батырмасы (Flutter _PackageCard). */
@Composable
private fun PackageCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    price: String,
    color: Color,
    onBuy: () -> Unit,
) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ext.card)
            .border(0.5.dp, ext.divider, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W600,
                color = ext.primaryText,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ext.secondaryText,
            )
        }
        Button(
            onClick = onBuy,
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = ext.white),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(text = price, fontSize = 12.sp, fontWeight = FontWeight.W600)
        }
    }
}