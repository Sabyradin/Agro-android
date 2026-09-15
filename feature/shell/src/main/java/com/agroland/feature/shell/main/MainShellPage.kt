package com.agroland.feature.shell.main

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.shell.home.HomePage

/** Төменгі навигация қойындылары. Орталық слот — "+" батырмасы (қойынды емес). */
private enum class ShellTab(val labelRes: Int) {
    HOME(L10nR.string.tab_home),
    CHAT(L10nR.string.tab_chat),
    CART(L10nR.string.tab_cart),
    SERVICES(L10nR.string.tab_services),
}

private data class TabSpec(
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

private val TAB_SPECS = listOf(
    TabSpec(Icons.Outlined.Home, Icons.Filled.Home),
    TabSpec(Icons.Outlined.ChatBubble, Icons.Filled.ChatBubble),
    TabSpec(Icons.Outlined.ShoppingCart, Icons.Filled.ShoppingCart),
    TabSpec(Icons.Outlined.Person, Icons.Filled.Person),
)

/** Басты shell — 5-slot төменгі жолақ, ортасындағы жасыл "+" батырмасымен. */
@Composable
fun MainShellPage(
    onCreateClick: () -> Unit,
    homeContent: @Composable () -> Unit = { HomePage() },
    servicesContent: @Composable () -> Unit = { ComingSoonTab() },
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (ShellTab.entries.getOrNull(selected)) {
                ShellTab.HOME -> homeContent()
                ShellTab.SERVICES -> servicesContent()
                else -> ComingSoonTab()
            }
        }
        GlassBottomBar(
            selected = selected,
            onSelect = { selected = it },
            onCreateClick = onCreateClick,
        )
    }
}

/** 72dp төменгі жолақ — ортасындағы жасыл "+" жоғары шығып тұрады. */
@Composable
private fun GlassBottomBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    onCreateClick: () -> Unit,
) {
    val ext = extendedColors()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ext.card.copy(alpha = 0.96f)),
    ) {
        val createLabel = stringResource(L10nR.string.tab_create)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-26).dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onCreateClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = createLabel,
                tint = ext.white,
                modifier = Modifier.size(30.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShellTab.entries.forEachIndexed { index, tab ->
                if (index == 2) {
                    // Орталық слот — "+" батырмасының астындағы бос орын.
                    Box(modifier = Modifier.weight(1f).height(72.dp))
                }
                BottomBarItem(
                    spec = TAB_SPECS[index],
                    label = stringResource(tab.labelRes),
                    selected = selected == index,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    spec: TabSpec,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ext = extendedColors()
    Column(
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) spec.selectedIcon else spec.icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else ext.secondaryText,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else ext.secondaryText,
            maxLines = 1,
        )
    }
}

/** Фаза 1 күйі: қойынды фазалар бойы толық экрандармен ауыстырылады (chat — фаза 12, cart — фаза 8, services — фаза 16). */
@Composable
private fun ComingSoonTab() {
    CenteredContent {
        Text(
            text = stringResource(L10nR.string.coming_soon_title),
            style = MaterialTheme.typography.bodyMedium,
            color = extendedColors().secondaryText,
        )
    }
}