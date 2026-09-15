package com.agroland.feature.shell.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroSearchField
import com.agroland.core.ui.components.AgroIconButton
import com.agroland.core.ui.components.ShimmerCard
import com.agroland.core.ui.theme.extendedColors

/** Home лентасының үш режимі — үш tablet-selector. */
private enum class HomeFeed(val labelRes: Int) {
    ANNOUNCEMENTS(L10nR.string.home_tab_announcements),
    AGRO(L10nR.string.home_tab_agro),
    CHINA(L10nR.string.home_tab_china),
}

/**
 * Басты экран (фаза 1 қаңқасы): іздеу жолағы + QR + фильтр,
 * үш tablet-selector (Хабарламалар / Агро / Қытай), skeleton лента.
 * Фаза 5-те лента нақты API деректерімен толтырылады.
 */
@Composable
fun HomePage() {
    val ext = extendedColors()
    var query by rememberSaveable { mutableStateOf("") }
    var feed by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Жоғарғы жолақ: іздеу + QR + фильтр.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AgroSearchField(
                value = query,
                onValueChange = { query = it },
                hint = stringResource(L10nR.string.home_search_hint),
                modifier = Modifier.weight(1f),
            )
            AgroIconButton(
                icon = Icons.Outlined.QrCodeScanner,
                contentDescription = stringResource(L10nR.string.home_scan_qr),
                onClick = { /* QR сканері — фаза 18 */ },
            )
            AgroIconButton(
                icon = Icons.Outlined.FilterAlt,
                contentDescription = stringResource(L10nR.string.home_filter),
                onClick = { /* Фильтр экраны — фаза 5 */ },
            )
        }

        // Үш таблетка.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HomeFeed.entries.forEachIndexed { index, tab ->
                val selected = feed == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else ext.grey,
                        )
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    androidx.compose.material3.Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) ext.white else ext.primaryText,
                        maxLines = 1,
                    )
                }
            }
        }

        Spacer(Modifier.height(2.dp))

        // Фаза 1: skeleton лента. Фаза 5-те AnnouncementCard + Paging келеді.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items((0 until 6).toList()) {
                ShimmerCard()
            }
        }
    }
}