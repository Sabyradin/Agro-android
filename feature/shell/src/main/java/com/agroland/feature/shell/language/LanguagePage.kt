package com.agroland.feature.shell.language

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.AppLocale
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.shell.ShellViewModel

/** Тіл таңдау экраны — төрт плитка, таңдалғаны жасыл белгімен. */
@Composable
fun LanguagePage(
    onSelected: () -> Unit,
    viewModel: ShellViewModel = hiltViewModel(),
) {
    val current by viewModel.localeTag.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(L10nR.string.language_title),
            style = MaterialTheme.typography.displayMedium,
            color = extendedColors().primaryText,
        )
        Text(
            text = stringResource(L10nR.string.language_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = extendedColors().secondaryText,
        )
        Spacer(Modifier.height(8.dp))
        AppLocale.entries.forEach { locale ->
            val selected = current == locale.tag
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) extendedColors().backgroundLight else extendedColors().card)
                    .clickable {
                        viewModel.selectLocale(locale.tag)
                        onSelected()
                    }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = locale.nativeName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = extendedColors().primaryText,
                    modifier = Modifier.weight(1f),
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}