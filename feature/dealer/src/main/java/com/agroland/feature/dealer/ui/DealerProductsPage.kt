package com.agroland.feature.dealer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroScaffold

/**
 * Дилер өнімдері хабы — Flutter DealerProductsPage (1:1): 4 таб
 * (Өнімдер / Логистика / Жарнама / Аналитика), initialTab Route-тан
 * келеді және 0..3 аралығына қысылады. Әр таб өз ViewModel-ін ұстайды.
 */
@Composable
fun DealerProductsPage(
    initialTab: Int,
    onBack: () -> Unit,
    onEditProduct: (Long) -> Unit,
    onAddZone: () -> Unit,
    onEditZone: (Long) -> Unit,
    onPickAnnouncement: () -> Unit,
) {
    var tabIndex by rememberSaveable {
        mutableIntStateOf(initialTab.coerceIn(0, 3))
    }
    val tabTitles = listOf(
        L10nR.string.dealer_tab_products,
        L10nR.string.dealer_tab_logistics,
        L10nR.string.dealer_tab_promotion,
        L10nR.string.dealer_tab_analytics,
    )

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(L10nR.string.dealer_my_products),
                onBack = onBack,
            )
        },
    ) { modifier ->
        Column(modifier = modifier) {
            TabRow(selectedTabIndex = tabIndex) {
                tabTitles.forEachIndexed { index, titleRes ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        text = { Text(stringResource(titleRes)) },
                    )
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when (tabIndex) {
                    0 -> DealerProductsTab(onEditProduct = onEditProduct)
                    1 -> DealerLogisticsTab(onAddZone = onAddZone, onEditZone = onEditZone)
                    2 -> DealerPromotionTab(onPickAnnouncement = onPickAnnouncement)
                    else -> DealerAnalyticsTab()
                }
            }
        }
    }
}