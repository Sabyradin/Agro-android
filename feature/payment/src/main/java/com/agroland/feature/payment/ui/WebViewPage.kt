package com.agroland.feature.payment.ui

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroScaffold

/**
 * WebViewPage — Flutter common_webview_page: Halyk ePay сілтемесі (payment_url)
 * немесе BCC legacy 3D Secure HTML формасы. exitRedirectUrl ішіндегі URL-ға
 * (agroland.kz) жеткенде/артқа басқанда onFinished шақырылады — қоңырау шалушы
 * төлем нәтижесі бетіне өтеді. 3DS формалары JavaScript сұрайды.
 */
@Composable
fun WebViewPage(
    title: String?,
    url: String,
    html: String? = null,
    exitRedirectUrl: String? = null,
    onFinished: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }

    fun finishOnce() {
        if (!finished) {
            finished = true
            onFinished()
        }
    }

    val client = remember {
        object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val target = request.url.toString()
                if (exitRedirectUrl != null && target.contains(exitRedirectUrl)) {
                    finishOnce()
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                loading = true
            }

            override fun onPageFinished(view: WebView, url: String?) {
                loading = false
            }
        }
    }

    BackHandler(onBack = { finishOnce() })

    AgroScaffold(
        topBar = {
            AgroAppBar(title = title ?: "", onBack = { finishOnce() })
        },
    ) { inner ->
        Column(modifier = inner.fillMaxSize()) {
            if (loading) {
                LinearProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = client
                        if (!html.isNullOrBlank()) {
                            loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                        } else {
                            loadUrl(url)
                        }
                    }
                },
            )
        }
    }
}