package com.agroland.feature.stories.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.MediaUrlResolver
import com.agroland.core.ui.theme.AgroColors
import com.agroland.core.ui.theme.OpenSansFamily
import com.agroland.feature.stories.domain.StoryViewerStateHolder
import com.agroland.feature.stories.domain.ViewerStory
import com.agroland.feature.stories.domain.ViewerSession
import kotlinx.coroutines.launch

/**
 * Толықэкран сторис-viewer (Flutter BannerStoryViewerPage + _StoryViewerPage,
 * 1:1 UX): 96×96 баннерлер де, 80×80 admin сторилер де осы бір экранмен
 * көрсетіледі — 5 сек авто-ілгерілеу, жоғарғы прогресс-жолақтар, сол/оң
 * жарты түртуден алға/артқа, ұстап тұру → пауза, төмен қарай сермеу → жабу.
 *
 * MainActivity overlay-інде орналасады (VoiceCallScreen үлгісі), сондықтан
 * shell navbar-ы табиғи түрде жасырылады (Flutter storyViewerOpenProvider).
 */
private const val STORY_DURATION_MS = 5000
private val ViewerWhite = Color(0xFFFFFFFF)
private val ProgressTrack = Color(0x59FFFFFF)
private val ViewerBlack = Color(0xFF000000)

@Composable
fun StoryViewerScreen(
    holder: StoryViewerStateHolder,
    modifier: Modifier = Modifier,
) {
    val session by holder.session.collectAsState()
    val current = session ?: return

    BackHandler { holder.close() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ViewerBlack),
    ) {
        if (current.stories.isNotEmpty()) {
            StoryPager(session = current, onClose = holder::close)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StoryPager(
    session: ViewerSession,
    onClose: () -> Unit,
) {
    val stories = session.stories
    val pagerState = rememberPagerState(
        initialPage = session.initialIndex.coerceIn(0, stories.lastIndex),
        pageCount = { stories.size },
    )
    val scope = rememberCoroutineScope()

    // 5 сек прогресі: пауза кезінде тоқтатылып, жалғасынан жүреді.
    val progress = remember { Animatable(0f) }
    var paused by remember { mutableStateOf(false) }

    // Стори көрсетілді → көрілген деп белгілеу (DataStore / POST view).
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            session.onStoryShown(stories[page])
        }
    }

    // Бет ауысқанда прогресті қайта бастау. Бұл эффект анимация эффектінен
    // ЖОҒАРЫДА жарияланған — қайта іске қосылғанда бірінші орындалып,
    // жаңа беттің анимациясын 0-ден бастайды (иначе 1f-тен секіріп кетеді).
    LaunchedEffect(pagerState.currentPage) {
        progress.snapTo(0f)
    }

    // Авто-ілгерілеу: қалған уақыт бойы сызықтық анимация, соңы → алға/жабу.
    LaunchedEffect(pagerState.currentPage, paused) {
        if (paused) {
            progress.stop()
        } else {
            val remaining = (((1f - progress.value) * STORY_DURATION_MS).toInt()).coerceAtLeast(1)
            progress.animateTo(1f, tween(durationMillis = remaining, easing = LinearEasing))
            val page = pagerState.currentPage
            if (page >= stories.lastIndex) {
                onClose()
            } else {
                pagerState.animateScrollToPage(page + 1)
            }
        }
    }

    // Төмен қарай 60px-тен артық сермеу → жабу (Flutter swipe-down close).
    var dragAccum by remember { mutableFloatStateOf(0f) }
    val swipeClose = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragStart = { dragAccum = 0f },
            onVerticalDrag = { _, dragAmount ->
                dragAccum += dragAmount
                if (dragAccum > 60.dp.toPx()) onClose()
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .then(swipeClose),
        ) { page ->
            StoryPage(
                story = stories[page],
                onOpenDetails = { details ->
                    if (session.popBeforeDetails) onClose()
                    details()
                },
                onTapPrev = {
                    scope.launch {
                        if (pagerState.currentPage > 0) {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        } else {
                            progress.snapTo(0f)
                        }
                    }
                },
                onTapNext = {
                    scope.launch {
                        if (pagerState.currentPage < stories.lastIndex) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        } else {
                            onClose()
                        }
                    }
                },
                onPressPause = { paused = it },
            )
        }

        // Жоғарғы градиент + прогресс-жолақтар + Agroland header.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color(0xB3000000), Color(0x00000000))),
                )
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            StoryProgressRow(
                count = stories.size,
                current = pagerState.currentPage,
                progress = progress.value,
            )
            Spacer(Modifier.height(12.dp))
            StoryHeader(onClose = onClose)
        }
    }
}

/** Прогресс-жолақтар: алдыңғылары — толық, ағымдағы — толып келе жатқан. */
@Composable
private fun StoryProgressRow(count: Int, current: Int, progress: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ProgressTrack),
            ) {
                val fraction = when {
                    index < current -> 1f
                    index == current -> progress.coerceIn(0f, 1f)
                    else -> 0f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(ViewerWhite),
                )
            }
        }
    }
}

/** Agroland аватары + атау + жабу батырмасы. */
@Composable
private fun StoryHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(AgroColors.primaryLight, AgroColors.primary))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "A",
                color = ViewerWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OpenSansFamily,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Agroland",
            color = ViewerWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = OpenSansFamily,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                tint = ViewerWhite,
            )
        }
    }
}

/** Бір сторисінің беті: сурет/статик градиент + түрту аймақтары + CTA. */
@Composable
private fun StoryPage(
    story: com.agroland.feature.stories.domain.ViewerStory,
    onOpenDetails: (() -> Unit) -> Unit,
    onTapPrev: () -> Unit,
    onTapNext: () -> Unit,
    onPressPause: (Boolean) -> Unit,
) {
    val ctaLabel = stringResource(L10nR.string.story_cta)

    Box(modifier = Modifier.fillMaxSize()) {
        if (story.imageRes != null) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(story.imageRes),
                contentDescription = story.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (story.imageUrl != null) {
            StoryImage(imageUrl = story.imageUrl, title = story.title)
        } else {
            StaticStoryContent(
                icon = story.staticIcon ?: Icons.Rounded.Storefront,
                title = story.title,
            )
        }

        // Түрту аймақтары: сол жарты — артқа, оң жарты — алға.
        // Басып ұстау → пауза (Flutter onLongPressStart/End), түрту → навигация.
        // Төмендегі CTA Column-ы кейін сызылады да түрту аймақтарын басады.
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onPressPause(true)
                                try {
                                    awaitRelease()
                                } finally {
                                    onPressPause(false)
                                }
                            },
                            onTap = { onTapPrev() },
                        )
                    },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onPressPause(true)
                                try {
                                    awaitRelease()
                                } finally {
                                    onPressPause(false)
                                }
                            },
                            onTap = { onTapNext() },
                        )
                    },
            )
        }

        // Төменгі градиент + тақырып + CTA (Flutter-дегідей төменгі 200dp).
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0x00000000), Color(0xB3000000))))
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 140.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if ((story.imageUrl != null || story.imageRes != null) && !story.title.isNullOrBlank()) {
                Text(
                    text = story.title,
                    color = ViewerWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = OpenSansFamily,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
            }
            story.onDetails?.let { details ->
                CtaPill(
                    label = ctaLabel,
                    onClick = { onOpenDetails(details) },
                )
            }
        }
    }
}

/** Backend баннері/сторидің суреті: жүктелу кезінде қара фон. */
@Composable
private fun StoryImage(imageUrl: String, title: String?) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101010)),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(MediaUrlResolver.resolve(imageUrl))
                .crossfade(true)
                .build(),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Статик промо сторисі: жасыл градиент + 72dp иконка + тақырып. */
@Composable
private fun StaticStoryContent(icon: ImageVector, title: String?) {
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ViewerWhite,
                modifier = Modifier.size(72.dp),
            )
            if (!title.isNullOrBlank()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = title,
                    color = ViewerWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OpenSansFamily,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
        }
    }
}

/** «Толығырақ» CTA: ақ пилюля, жасыл мәтін + көрсеткі. */
@Composable
private fun CtaPill(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(30.dp))
            .background(ViewerWhite)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .padding(horizontal = 26.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            color = AgroColors.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = OpenSansFamily,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = AgroColors.primary,
            modifier = Modifier.size(16.dp),
        )
    }
}