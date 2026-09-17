package com.agroland.feature.stories.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.MediaUrlResolver
import com.agroland.core.ui.theme.AgroColors
import com.agroland.core.ui.theme.OpenSansFamily
import com.agroland.feature.stories.domain.ViewerSession
import com.agroland.feature.stories.domain.ViewerStory

/**
 * Admin сторилер жолы (Flutter StoriesListView, 1:1): 80×80, 20dp радиус,
 * жиек 2.5dp — көрілмеген жасыл / көрілген сұр. Бос немесе қате → жол мүлдем
 * көрінбейді (SizedBox.shrink). Түртуден толықэкран ортақ StoryViewerScreen;
 * CTA түртуде маркетинг клигі тіркеліп, сыртқы сілтеме АШЫЛАДЫ, бірақ viewer
 * жабылмайды (popBeforeDetails = false).
 */
@Composable
fun StoriesRow(
    modifier: Modifier = Modifier,
    viewModel: StoriesViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val cannotOpenLink = stringResource(L10nR.string.cannot_open_link)
    val state by viewModel.state.collectAsState()
    val stories = state.stories
    if (stories.isEmpty()) return

    val locale = remember { context.resources.configuration.locales[0].language }
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant
    val titleColor = MaterialTheme.colorScheme.onSurface

    // Viewer сессиясы үшін сторилер дайын тізімге айналады.
    val viewerStories = remember(stories, locale, cannotOpenLink) {
        stories.map { item ->
            ViewerStory(
                id = item.id,
                imageUrl = item.imageUrl.takeIf { it.isNotBlank() },
                title = item.titleLocalized(locale),
                onDetails = item.ctaLink?.takeIf { it.isNotBlank() }?.let { link ->
                    {
                        // CTA: маркетинг клигі + сыртқы сілтеме, viewer ашық қалады.
                        viewModel.recordCtaClick(item.id)
                        openStoryLink(context, link, cannotOpenLink)
                    }
                },
            )
        }
    }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height(108.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(stories.size) { index ->
            val story = stories[index]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    viewModel.viewer.open(
                        ViewerSession(
                            stories = viewerStories,
                            initialIndex = index,
                            onStoryShown = { shown ->
                                // Тек көрілмеген болса белгілейміз (Flutter onPageChanged).
                                val current = stories.firstOrNull { it.id == shown.id }
                                if (current != null && !current.isViewed) {
                                    viewModel.onStoryShown(current.id)
                                }
                            },
                            popBeforeDetails = false,
                        ),
                    )
                },
            ) {
                StoryCircle(
                    imageUrl = story.imageUrl,
                    ringColor = if (story.isViewed) AgroColors.divider else AgroColors.primary,
                    placeholderColor = placeholderColor,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = story.titleLocalized(locale),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = OpenSansFamily,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(80.dp),
                )
            }
        }
    }
}

/** 80×80, 20dp радиус: жиек 2.5dp, ішінде 3dp паддинг + 17dp радиусты сурет. */
@Composable
private fun StoryCircle(
    imageUrl: String,
    ringColor: Color,
    placeholderColor: Color,
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 2.5.dp, color = ringColor, shape = RoundedCornerShape(20.dp))
            .padding(3.dp),
    ) {
        StoryImage(imageUrl = imageUrl, placeholderColor = placeholderColor)
    }
}

/** Сурет: жүктелу — спиннер, қате/бос URL — фото-иконка (тыныш, қате көрінбейді). */
@Composable
private fun StoryImage(imageUrl: String, placeholderColor: Color) {
    val context = LocalContext.current
    val isInvalid = imageUrl.isBlank() ||
        (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) ||
        imageUrl.contains("example.com")
    if (isInvalid) {
        IconPlaceholder(placeholderColor)
        return
    }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(MediaUrlResolver.resolve(imageUrl))
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(17.dp)),
        loading = { IconPlaceholder(placeholderColor, spinner = true) },
        error = { IconPlaceholder(placeholderColor) },
    )
}

@Composable
private fun IconPlaceholder(backgroundColor: Color, spinner: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        if (spinner) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

/** Сыртқы сілтемені ашу; ашылмаса — адам тіліндегі қате туралы toast. */
private fun openStoryLink(context: android.content.Context, url: String, errorText: String) {
    var opened = false
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        opened = true
    } catch (_: Exception) {
    }
    if (!opened) Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
}