package com.agroland.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agroland.core.ui.theme.extendedColors

/**
 * Уақытша AppLogo — фазалар бойы "Agroland" жазуымен көрсетіледі.
 * Нақты логотип ассесі Agroland командасынан келгенде алмастырылады (ISSUES.md #1).
 */
@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Agroland",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Маркетплейс",
            style = MaterialTheme.typography.bodySmall,
            color = extendedColors().secondaryText,
            textAlign = TextAlign.Center,
        )
    }
}