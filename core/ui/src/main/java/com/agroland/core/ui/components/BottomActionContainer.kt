package com.agroland.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agroland.core.ui.theme.extendedColors

/**
 * Экранның астындағы тұрақты әрекет аймағы — CTA-батырма контейнері.
 * Flutter-дағы BottomActionContainer баламасы: фон картасы, үстінен бөлгіш сызық.
 */
@Composable
fun BottomActionContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val ext = extendedColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ext.card),
    ) {
        content()
    }
}