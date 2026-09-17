package com.agroland.feature.shell.language

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.AppLocale
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AppLogo
import com.agroland.core.ui.theme.AgroRadius
import com.agroland.core.ui.theme.AgroSpacing
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.shell.ShellViewModel

/** Тіл таңдау экраны — төрт плитка, таңдалғаны жасыл белгімен. */
@Composable
fun LanguagePage(
    onSelected: () -> Unit,
    viewModel: ShellViewModel = hiltViewModel(),
) {
    val current by viewModel.localeTag.collectAsState()

    val ext = extendedColors()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AgroSpacing.xl, vertical = AgroSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AgroSpacing.md),
    ) {
        Spacer(Modifier.height(AgroSpacing.xl))
        AppLogo(size = 96.dp)
        Spacer(Modifier.height(40.dp))
        Text(
            text = stringResource(L10nR.string.language_title),
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            color = ext.primaryText,
        )
        Text(
            text = stringResource(L10nR.string.language_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = ext.secondaryText,
        )
        Spacer(Modifier.height(AgroSpacing.sm))
        AppLocale.entries.forEach { locale ->
            val selected = current == locale.tag
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AgroRadius.field)
                    .background(if (selected) ext.backgroundLight else ext.card)
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else ext.divider,
                        shape = AgroRadius.field,
                    )
                    .clickable {
                        viewModel.selectLocale(locale.tag)
                        onSelected()
                    }
                    .padding(horizontal = AgroSpacing.lg, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.md),
            ) {
                Text(
                    text = locale.nativeName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (selected) MaterialTheme.colorScheme.primary else ext.primaryText,
                    modifier = Modifier.weight(1f),
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}