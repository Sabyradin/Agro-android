package com.agroland.feature.shell.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.LocalShellBottomPadding
import com.agroland.core.ui.theme.AgroSpacing
import com.agroland.core.ui.theme.extendedColors

/**
 * Төменгі навигацияның 5 слоты. Ортаңғы «Қосу» — қойынды ЕМЕС, бірақ
 * iOS-тағыдай өз атауы бар толыққанды слот (жасыл дөңгелек «+»).
 */
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
    TabSpec(Icons.Outlined.ChatBubbleOutline, Icons.Filled.Chat),
    TabSpec(Icons.Outlined.ShoppingCart, Icons.Filled.ShoppingCart),
    // Фаза 17: SERVICES қойындысы — Сервистер беті (профиль Home аватарына көшті).
    TabSpec(Icons.Outlined.GridView, Icons.Filled.GridView),
)

private val NavBarHeight = 58.dp

/**
 * Басты shell — iOS нұсқасындағыдай төменгі жиекке бекітілген навигация:
 * 5 слот (Басты бет · Чат · Қосу · Себет · Сервистер), ортасында жасыл
 * дөңгелек «+», әр слоттың астында атауы.
 */
@Composable
fun MainShellPage(
    onCreateClick: () -> Unit,
    homeContent: @Composable () -> Unit,
    chatContent: @Composable () -> Unit = { ComingSoonTab() },
    cartContent: @Composable () -> Unit = { ComingSoonTab() },
    servicesContent: @Composable () -> Unit = { ComingSoonTab() },
    /** Чат қойындысының оқылмаған бейджі (Socket.IO totalUnread). */
    chatBadge: Int = 0,
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }

    // Панель контенттің астында тұрады (қалқымайды) — тізімдерге қосымша
    // соқпа қажет емес, тек кішкене «тыныс алу» орны.
    CompositionLocalProvider(LocalShellBottomPadding provides AgroSpacing.sm) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (ShellTab.entries.getOrNull(selected)) {
                    ShellTab.HOME -> homeContent()
                    ShellTab.CHAT -> chatContent()
                    ShellTab.CART -> cartContent()
                    ShellTab.SERVICES -> servicesContent()
                    else -> ComingSoonTab()
                }
            }
            BottomNavBar(
                selected = selected,
                onSelect = { selected = it },
                onCreateClick = onCreateClick,
                chatBadge = chatBadge,
            )
        }
    }
}

/** Төменгі жиектегі навигация жолағы — жоғарғы шекарасында жіңішке сызық. */
@Composable
private fun BottomNavBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    onCreateClick: () -> Unit,
    chatBadge: Int,
) {
    val ext = extendedColors()
    Column(modifier = Modifier.fillMaxWidth().background(ext.card)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ext.divider),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(NavBarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShellTab.entries.forEachIndexed { index, tab ->
                // Ортаңғы слот — «Қосу».
                if (index == 2) {
                    CreateSlot(
                        label = stringResource(L10nR.string.tab_create),
                        onClick = onCreateClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                NavBarItem(
                    spec = TAB_SPECS[index],
                    label = stringResource(tab.labelRes),
                    selected = selected == index,
                    badgeCount = if (tab == ShellTab.CHAT) chatBadge else 0,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** «Қосу» слоты: жасыл дөңгелек «+» және астында атауы. */
@Composable
private fun CreateSlot(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = false,
                onClick = onClick,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 40.dp),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = extendedColors().white,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Қойынды слоты: иконка + атауы; белсендісі жасыл әрі толтырылған. */
@Composable
private fun NavBarItem(
    spec: TabSpec,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
) {
    val ext = extendedColors()
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else ext.secondaryText,
        label = "navItemColor",
    )
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 40.dp),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            Icon(
                imageVector = if (selected) spec.selectedIcon else spec.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(26.dp),
            )
            if (badgeCount > 0) {
                UnreadBadge(
                    count = badgeCount,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Оқылмаған хабарлама саны — қызыл дөңгелек, панель түсті жиегімен. */
@Composable
private fun UnreadBadge(count: Int, modifier: Modifier = Modifier) {
    val ext = extendedColors()
    Box(
        modifier = modifier
            .offset(x = 10.dp, y = (-6).dp)
            .clip(CircleShape)
            .background(ext.card)
            .padding(1.5.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onError,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

/** Әлі толтырылмаған қойынды (shell-ді жеке қолданғанда ғана көрінеді). */
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
