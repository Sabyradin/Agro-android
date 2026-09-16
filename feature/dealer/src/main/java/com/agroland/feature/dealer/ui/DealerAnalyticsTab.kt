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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.common.formatters.CountFormatter
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroChip
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.dealer.data.AnalyticsPoint
import com.agroland.feature.dealer.data.displayText
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

/**
 * Аналитика табы — Flutter DealerAnalyticsTab (1:1): тариф гейті
 * (locked → in-tab placeholder), bucket чиптері (күн/апта/ай), 2×2
 * статистика плиткалары, түсім сызықшасы (Vico) және үздік өнімдер.
 * Timeseries қатесі графикті құлатпайды — бос сызық ғана.
 */
@Composable
fun DealerAnalyticsTab(
    viewModel: DealerAnalyticsViewModel = hiltViewModel(),
) {
    val bucket by viewModel.bucket.collectAsState()
    val locked by viewModel.locked.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    val points by viewModel.points.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val networkError = stringResource(L10nR.string.error_no_internet)
    val genericError = stringResource(L10nR.string.error_generic_message)

    if (locked) {
        AnalyticsLockedPlaceholder()
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Bucket чиптері: күн (30d) / апта (84d) / ай (365d).
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                AgroChip(
                    text = stringResource(L10nR.string.dealer_period_day),
                    selected = bucket == "day",
                    onClick = { viewModel.setBucket("day") },
                )
            }
            item {
                AgroChip(
                    text = stringResource(L10nR.string.dealer_period_week),
                    selected = bucket == "week",
                    onClick = { viewModel.setBucket("week") },
                )
            }
            item {
                AgroChip(
                    text = stringResource(L10nR.string.dealer_period_month),
                    selected = bucket == "month",
                    onClick = { viewModel.setBucket("month") },
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                loading -> LoadingWidget()
                error != null -> CenteredContent {
                    ErrorWithRetry(onRetry = viewModel::load, message = error!!.displayText(networkError, genericError))
                }
                analytics == null -> CenteredContent { LoadingWidget() }
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val totals = analytics!!.totals
                    // ── Статистика плиткалары 2×2 ──
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatTile(
                                icon = Icons.Outlined.RemoveRedEye,
                                label = stringResource(L10nR.string.dealer_total_views),
                                value = CountFormatter.formatCompact(totals.views),
                                color = Color(0xFF2196F3),
                                modifier = Modifier.weight(1f),
                            )
                            StatTile(
                                icon = Icons.Outlined.ShoppingBag,
                                label = stringResource(L10nR.string.dealer_total_orders),
                                value = CountFormatter.formatCompact(totals.orders),
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatTile(
                                icon = Icons.Outlined.Bolt,
                                label = stringResource(L10nR.string.dealer_total_revenue),
                                value = CountFormatter.formatCompact(totals.revenue.toInt()),
                                color = Color(0xFFFF9800),
                                modifier = Modifier.weight(1f),
                            )
                            StatTile(
                                icon = Icons.Outlined.QueryStats,
                                label = stringResource(L10nR.string.dealer_conversion),
                                value = formatConversionPercent(totals.conversion),
                                color = Color(0xFF9C27B0),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    // ── Түсім диаграммасы ──
                    item { RevenueChartCard(points) }
                    // ── Үздік өнімдер ──
                    item {
                        Text(
                            text = stringResource(L10nR.string.dealer_top_products),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = extendedColors().primaryText,
                        )
                    }
                    if (analytics!!.topAnnouncements.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(L10nR.string.dealer_analytics_no_data),
                                style = MaterialTheme.typography.bodySmall,
                                color = extendedColors().secondaryText,
                            )
                        }
                    } else {
                        items(analytics!!.topAnnouncements, key = { it.announcementId }) { top ->
                            TopProductRow(top.title, top.views, top.orders, top.revenue)
                        }
                    }
                }
            }
        }
    }
}

/** Тариф гейті — аналитика Business тарифінен ғана (Flutter LockedFeaturePlaceholder). */
@Composable
private fun AnalyticsLockedPlaceholder() {
    val ext = extendedColors()
    CenteredContent {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 40.dp)) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = ext.secondaryText,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(L10nR.string.dealer_analytics_locked),
                style = MaterialTheme.typography.titleMedium,
                color = ext.primaryText,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(L10nR.string.dealer_analytics_locked_hint),
                style = MaterialTheme.typography.bodySmall,
                color = ext.secondaryText,
            )
        }
    }
}

/** Статистика плиткасы — иконка + мән + атау (Flutter stats grid). */
@Composable
private fun StatTile(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val ext = extendedColors()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.08f))
            .border(0.5.dp, color.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ext.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ext.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Түсім картасы — Vico сызықшасы (fl_chart LineChart баламасы). */
@Composable
private fun RevenueChartCard(points: List<AnalyticsPoint>) {
    val ext = extendedColors()
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(points) {
        if (points.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    series(x = points.indices.toList(), y = points.map { it.revenue })
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ext.card)
            .border(0.5.dp, ext.divider, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(L10nR.string.dealer_revenue_chart),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = ext.primaryText,
        )
        Spacer(Modifier.height(12.dp))
        if (points.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(L10nR.string.dealer_analytics_no_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.secondaryText,
                )
            }
        } else {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                ),
                modelProducer = modelProducer,
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
        }
    }
}

/** Үздік өнім жолы — атау + қаралым + тапсырыс + табыс. */
@Composable
private fun TopProductRow(title: String, views: Int, orders: Int, revenue: Double) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ext.card)
            .border(0.5.dp, ext.divider, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.RemoveRedEye,
            contentDescription = null,
            tint = ext.secondaryText,
            modifier = Modifier.size(18.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = ext.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = CountFormatter.formatCompact(views) + " · " +
                    stringResource(L10nR.string.dealer_total_orders) + ": " + CountFormatter.formatCompact(orders),
                style = MaterialTheme.typography.labelSmall,
                color = ext.secondaryText,
                maxLines = 1,
            )
        }
        Text(
            text = PriceFormatter.format(revenue),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.W600,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}