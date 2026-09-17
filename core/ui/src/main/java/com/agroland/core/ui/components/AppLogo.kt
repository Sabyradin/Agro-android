package com.agroland.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.R
import com.agroland.core.ui.theme.AgroSpacing
import com.agroland.core.ui.theme.extendedColors

/**
 * Agroland логотипі — қолданбаның нақты эмблемасы (бидай + AL + AGROLAND).
 * Бұрын орнында уақытша «Agroland» жазуы тұрған (ISSUES.md #1).
 *
 * [card] = true болғанда iOS-тағыдай ақ дөңгелектелген карточка ішінде
 * көрсетіледі (кіру/тіркелу беттері).
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    card: Boolean = false,
    showTagline: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (card) {
            Box(
                modifier = Modifier
                    .size(size)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(size * 0.22f),
                        ambientColor = Color(0x1F121212),
                        spotColor = Color(0x1F121212),
                    )
                    .clip(RoundedCornerShape(size * 0.22f))
                    .background(extendedColors().white)
                    .padding(size * 0.06f),
            ) {
                LogoImage(Modifier.fillMaxSize())
            }
        } else {
            LogoImage(Modifier.size(size))
        }
        if (showTagline) {
            Spacer(Modifier.height(AgroSpacing.sm))
            Text(
                text = stringResource(L10nR.string.app_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = extendedColors().secondaryText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LogoImage(modifier: Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_agroland_logo_full),
        contentDescription = stringResource(L10nR.string.app_name),
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}
