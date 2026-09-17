package com.agroland.feature.media.ui

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * YouTubeViewer (Flutter you_tube_player_view → IFrame API WebView):
 * videoId шығатын сілтеме — IFrame player (autoplay off); шықпаса
 * (mp4/TikTok т.б.) — VideoViewerPage fallback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeViewerPage(
    url: String,
    onBack: () -> Unit,
) {
    val videoId = YouTubeUrlParser.extractVideoId(url)
    if (videoId == null) {
        VideoViewerPage(url = url, title = null, onBack = onBack)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        TopAppBar(
            title = {},
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                factory = { ctx ->
                    createPlayerWebView(ctx, videoId)
                },
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createPlayerWebView(context: android.content.Context, videoId: String): WebView {
    return WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        webViewClient = WebViewClient()
        webChromeClient = WebChromeClient()
        setBackgroundColor(android.graphics.Color.BLACK)
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    body { margin: 0; background: #000; }
                    #player { width: 100%%; height: 100vh; }
                </style>
            </head>
            <body>
                <div id="player"></div>
                <script>
                    var player;
                    function onYouTubeIframeAPIReady() {
                        player = new YT.Player('player', {
                            videoId: '$videoId',
                            playerVars: {
                                autoplay: 0,
                                playsinline: 1,
                                rel: 0,
                                modestbranding: 1
                            },
                            events: {
                                onReady: function(e) { e.target.setPlaybackQuality('hd720'); }
                            }
                        });
                    }
                </script>
                <script src="https://www.youtube.com/iframe_api"></script>
            </body>
            </html>
        """.trimIndent()
        loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
    }
}

/** YoutubePlayer.convertUrlToId логикасы — таза функция. */
internal object YouTubeUrlParser {

    private val idPattern = Regex("""([A-Za-z0-9_-]{11})""")
    private val patterns = listOf(
        """youtu\.be/([A-Za-z0-9_-]{11})""",
        """youtube\.com/embed/([A-Za-z0-9_-]{11})""",
        """youtube\.com/shorts/([A-Za-z0-9_-]{11})""",
        """youtube\.com/live/([A-Za-z0-9_-]{11})""",
        """[?&]v=([A-Za-z0-9_-]{11})""",
    )

    /** YouTube videoId немесе null (сілтеме YouTube емес). */
    fun extractVideoId(url: String): String? {
        val trimmed = url.trim()
        if (!trimmed.contains("youtube") && !trimmed.contains("youtu.be")) return null
        for (pattern in patterns) {
            Regex(pattern).find(trimmed)?.groupValues?.getOrNull(1)?.let { return it }
        }
        return idPattern.find(trimmed)?.groupValues?.getOrNull(1)
    }
}