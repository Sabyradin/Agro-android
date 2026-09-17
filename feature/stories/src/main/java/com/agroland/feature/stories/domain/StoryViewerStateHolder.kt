package com.agroland.feature.stories.domain

import androidx.compose.ui.graphics.vector.ImageVector
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Толықэкран сторис-viewer сессиясы (Flutter: Navigator.push(fullscreenDialog)
 * + storyViewerOpenProvider shell navbar-ды жасырады; Android портында —
 * singleton state + MainActivity overlay хосты, VoiceCallScreen үлгісі).
 *
 * Екі viewer де осы бір экранмен жұмыс істейді (Flutter BannerStoryViewerPage
 * + _StoryViewerPage — UX-ы бірдей): баннер-сторис жолы (backend баннері /
 * fallback суреті / статик промо) және admin сторилер жолы.
 */
data class ViewerStory(
    /** Көрілген күй кілті (баннер: «b{id}» / «static_*»; admin стори: id). */
    val id: String,
    /** Backend баннерінің/сторидің сурет URL-і (статик промо үшін null). */
    val imageUrl: String? = null,
    /** Қолданбаға кіріктірілген сурет (backend баннері жоқ кездегі агро-стористер). */
    val imageRes: Int? = null,
    /** Стори тақырыбы (viewer төменгі бөлігінде көрінеді). */
    val title: String? = null,
    /** Статик промо иконкасы (imageUrl == null кезде көрінеді). */
    val staticIcon: ImageVector? = null,
    /** «Толығырақ» CTA мақсаты — [ViewerSession.popBeforeDetails]-ке сай ойнатылады. */
    val onDetails: (() -> Unit)? = null,
)

data class ViewerSession(
    val stories: List<ViewerStory>,
    val initialIndex: Int,
    /** Стори көрсетілгенде шақырылады: баннер id-ін DataStore-қа / сториді POST view. */
    val onStoryShown: (ViewerStory) -> Unit,
    /** true — CTA басылғанда viewer алдымен жабылып, содан кейін мақсатқа өтеді
     *  (баннер-viewer мінез-құлқы); false — viewer ашық қалады (admin стори). */
    val popBeforeDetails: Boolean = true,
)

/**
 * Viewer сессиясын ұстайтын singleton (MainActivity overlay-і тыңдайды).
 */
@Singleton
class StoryViewerStateHolder @Inject constructor() {

    private val _session = MutableStateFlow<ViewerSession?>(null)
    val session: StateFlow<ViewerSession?> = _session.asStateFlow()

    fun open(session: ViewerSession) {
        _session.value = session
    }

    fun close() {
        _session.value = null
    }
}