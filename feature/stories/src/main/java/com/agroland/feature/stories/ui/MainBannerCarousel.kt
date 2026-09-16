package com.agroland.feature.stories.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.theme.AgroColors
import com.agroland.core.ui.theme.OpenSansFamily
import com.agroland.feature.stories.domain.ViewerSession
import com.agroland.feature.stories.domain.ViewerStory

/**
 * Басты беттің баннер-каруселі (Flutter MainBannerCarousel, 1:1):
 * Instagram-сторис үлгісіндегі 96×96 шаршы карточкалар жолы.
 * Көрінбегендері — жасыл жиек, көрінгендері — сұр (DataStore-та тұрақтайды).
 * Backend баннерлері алдымен; бос болса — Wikimedia fallback суреттері;
 * соңынан 4 статик промо-карточка (құру/жарнама/Қытай/көтерілгендер)
 * қосылады, сондықтан жол ешқашан бос қалмайды.
 */
private const val CARD_SIZE = 96
private val CardTitleWhite = Color(0xFFFFFFFF)

/** Backend тізімі бос болғанда көрсетілетін fallback суреттері (CC0, Wikimedia). */
private data class FallbackBanner(val id: String, val imageUrl: String)

private val FALLBACK_BANNERS = listOf(
    FallbackBanner(
        id = "fb_wheat",
        imageUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/24/" +
            "Tree_and_wheat_field_in_R%C3%B6e_1.jpg/1280px-Tree_and_wheat_field_in_R%C3%B6e_1.jpg",
    ),
    FallbackBanner(
        id = "fb_tractor",
        imageUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d6/" +
            "Tractor_cultivating_a_field_in_Brittany.jpg/" +
            "1280px-Tractor_cultivating_a_field_in_Brittany.jpg",
    ),
    FallbackBanner(
        id = "fb_sunflower",
        imageUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/43/" +
            "Sunflower_field_at_sunset.jpg/1280px-Sunflower_field_at_sunset.jpg",
    ),
    FallbackBanner(
        id = "fb_apple",
        imageUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/37/" +
            "Apple_harvest_in_South_Tyrol_2015.jpg/" +
            "1280px-Apple_harvest_in_South_Tyrol_2015.jpg",
    ),
    FallbackBanner(
        id = "fb_cows",
        imageUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/03/" +
            "Cows_at_the_Preval%28a%29_pasture.jpg/" +
            "1280px-Cows_at_the_Preval%28a%29_pasture.jpg",
    ),
)

/** Статик промо карточкасының метасы (viewer-де бірдей көрінеді). */
private data class StaticPromo(
    val id: String,
    val icon: ImageVector,
    val label: String,
    val onOpen: () -> Unit,
)

@Composable
fun MainBannerCarousel(
    onOpenAnnouncement: (announcementId: Long) -> Unit,
    onOpenCreate: () -> Unit = {},
    onOpenAdvertise: () -> Unit = {},
    onOpenChinaCatalog: () -> Unit = {},
    onOpenPromoted: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MainBannerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val cannotOpenLink = stringResource(L10nR.string.cannot_open_link)
    val createLabel = stringResource(L10nR.string.create_ad_title)
    val advertiseLabel = stringResource(L10nR.string.story_advertise)
    val chinaLabel = stringResource(L10nR.string.home_tab_china)
    val promotedLabel = stringResource(L10nR.string.story_promoted)

    val bannersState by viewModel.banners.collectAsState()
    val viewed by viewModel.viewedIds.collectAsState()

    // Backend баннерлері жоқ болса ғана fallback суреттерін қосамыз.
    val fallbacks = if (bannersState.banners.isEmpty()) FALLBACK_BANNERS else emptyList()

    val statics = remember(createLabel, advertiseLabel, chinaLabel, promotedLabel) {
        listOf(
            StaticPromo("static_create", Icons.Filled.AddCircle, createLabel, onOpenCreate),
            StaticPromo("static_advertise", Icons.Filled.Bolt, advertiseLabel, onOpenAdvertise),
            StaticPromo("static_china_catalog", Icons.Filled.ShoppingCart, chinaLabel, onOpenChinaCatalog),
            StaticPromo("static_promoted", Icons.Filled.LocalFireDepartment, promotedLabel, onOpenPromoted),
        )
    }

    // Viewer-ге берілетін тізім: backend баннерлері → fallback суреттері → статик промо.
    val stories = remember(bannersState.banners, fallbacks, statics) {
        buildList {
            for (banner in bannersState.banners) {
                add(
                    ViewerStory(
                        id = "b${banner.id}",
                        imageUrl = banner.imageUrl,
                        title = banner.title,
                        onDetails = {
                            // Viewer жабылғаннан кейін: хабарлама беті НЕМЕСЕ сыртқы cta_url.
                            val target = banner.announcementId
                            if (target != null) {
                                onOpenAnnouncement(target)
                            } else if (!banner.ctaUrl.isNullOrBlank()) {
                                openExternalLink(context, banner.ctaUrl!!, cannotOpenLink)
                            }
                        },
                    ),
                )
            }
            for (fb in fallbacks) {
                // Тек сурет: «Толығырақ» батырмасы жоқ.
                add(ViewerStory(id = fb.id, imageUrl = fb.imageUrl))
            }
            for (promo in statics) {
                add(
                    ViewerStory(
                        id = promo.id,
                        title = promo.label,
                        staticIcon = promo.icon,
                        onDetails = promo.onOpen,
                    ),
                )
            }
        }
    }

    val openViewer: (Int) -> Unit = { index ->
        viewModel.viewer.open(
            ViewerSession(
                stories = stories,
                initialIndex = index,
                onStoryShown = { story -> viewModel.markViewed(story.id) },
                popBeforeDetails = true,
            ),
        )
    }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height((CARD_SIZE + 16).dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexedCompat(stories) { index, story ->
            StoryCard(
                viewed = viewed.contains(story.id),
                onClick = { openViewer(index) },
            ) {
                if (!story.imageUrl.isNullOrBlank()) {
                    ImageCard(imageUrl = story.imageUrl!!, title = story.title)
                } else {
                    StaticCard(icon = story.staticIcon!!, label = story.title!!)
                }
            }
        }
    }
}

/** Сыртқы cta_url ашу (Flutter url_launcher externalApplication). */
private fun openExternalLink(context: android.content.Context, url: String, errorText: String) {
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

/** Көрілмеген — жасыл, көрінген — сұр жиек, ішінде 13dp радиусты контент. */
@Composable
private fun StoryCard(
    viewed: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(CARD_SIZE.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (viewed) AgroColors.divider else AgroColors.primary)
            .clickable(onClick = onClick)
            .padding(2.5.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(13.dp)),
        ) {
            content()
        }
    }
}

/** Суретті карточка: сурет + (title бар болса) төменгі градиенттегі ақ мәтін. */
@Composable
private fun ImageCard(imageUrl: String, title: String?) {
    Box(modifier = Modifier.fillMaxSize()) {
        CachedImage(url = imageUrl, contentDescription = title, modifier = Modifier.fillMaxSize())
        if (!title.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0x8C000000), Color(0x00000000))))
                    .padding(8.dp),
            ) {
                Text(
                    text = title,
                    color = CardTitleWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = OpenSansFamily,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Статик промо: жасыл градиент + 28dp иконка + ақ қысқа атау. */
@Composable
private fun StaticCard(icon: ImageVector, label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(AgroColors.primary, AgroColors.primary.copy(alpha = 0.72f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = label,
                tint = CardTitleWhite,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                color = CardTitleWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = OpenSansFamily,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** LazyRow itemsIndexed қысқартуы. */
private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedCompat(
    items: List<ViewerStory>,
    itemContent: @Composable (index: Int, story: ViewerStory) -> Unit,
) {
    items(items.size) { index -> itemContent(index, items[index]) }
}