package com.agroland.feature.notifications.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.rememberLazyListState
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.notifications.data.NotificationItem
import com.agroland.feature.notifications.data.NotificationType

/**
 * Толық хабарлама беті (Flutter SingleNotificationPage):
 * full-bleed сурет басы, үстінде шынылы батырмалар (артқа + бөлісу),
 * астында — дөңгелектеуілген пәрмендер карточкасы HTML мазмұнмен.
 * Бөлісу: https://agroland.kz/news/{type}/{id}.
 */
@Composable
fun SingleNotificationPage(
    item: NotificationItem,
    type: NotificationType,
    onBack: () -> Unit,
) {
    val ext = extendedColors()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            // Hero сурет — экран еніндей (Flutter expandedHeight паритеті).
            item(key = "hero") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                ) {
                    if (item.file.isNotEmpty()) {
                        NotificationImage(
                            file = item.file,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(Modifier.fillMaxSize().background(ext.grey))
                    }
                    // Градиент қылқыпақ — батырмалар оқылымды болсын.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent),
                                ),
                            ),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.30f)),
                                ),
                            ),
                    )
                }
            }
            item(key = "body") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            androidx.compose.foundation.shape.RoundedCornerShape(
                                topStart = 24.dp, topEnd = 24.dp,
                            ),
                        )
                        .background(ext.card)
                        .padding(20.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ext.primaryText,
                    )
                }
            }
            item(key = "text") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ext.card)
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 40.dp),
                ) {
                    Spacer(Modifier.height(8.dp))
                    HtmlText(html = item.text)
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = formatNotificationDate(item.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.secondaryText,
                    )
                }
            }
        }

        // Үстіңгі батырмалар — hero үстінде жартылай шыны фонмен.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HeroIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = androidx.compose.ui.res.stringResource(L10nR.string.common_back),
                tint = Color.White,
                onClick = onBack,
            )
            HeroIconButton(
                icon = Icons.Rounded.Share,
                contentDescription = androidx.compose.ui.res.stringResource(L10nR.string.common_share),
                tint = Color.White,
                onClick = {
                    val link = "https://agroland.kz/news/${type.path}/${item.id}"
                    val send = Intent(Intent.ACTION_SEND).apply {
                        setType("text/plain")
                        putExtra(Intent.EXTRA_TEXT, link)
                    }
                    context.startActivity(Intent.createChooser(send, null))
                },
            )
        }
    }
}

/** Hero үстіндегі дөңгелек шыны батырма. */
@Composable
private fun HeroIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.clip(CircleShape),
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.30f))
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.padding(0.dp),
            )
        }
    }
}