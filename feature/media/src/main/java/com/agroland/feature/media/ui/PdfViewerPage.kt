package com.agroland.feature.media.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.LoadingWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * PdfViewerPage (ISSUES #27 жабылады): PDF сыртқы қосымшамен емес,
 * ішінде көрсетіледі — OkHttp жүктеу + PdfRenderer беттерді Bitmap-қа
 * айналдырып LazyColumn-да көрсетеді (Flutter pdf_viewer_page 1:1).
 */
@Composable
fun PdfViewerPage(
    url: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val errorText = stringResource(L10nR.string.pdf_load_failed)
    val documentTitle = stringResource(L10nR.string.document_title)

    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(url) {
        loading = true
        failed = false
        val rendered = PdfPageLoader.load(context, url)
        if (rendered == null) {
            failed = true
        } else {
            pages = rendered
        }
        loading = false
    }

    AgroScaffold(
        topBar = { AgroAppBar(title = documentTitle, onBack = onBack) },
    ) { modifier ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF3A3A3C)),
        ) {
            when {
                loading -> LoadingWidget()
                failed -> Text(
                    text = errorText,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp),
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    itemsIndexed(pages) { _, bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

/** PDF жүктеу + рендер — таза object, ІО-ның бәрі Dispatchers.IO-да. */
internal object PdfPageLoader {

    private val client by lazy { OkHttpClient() }

    /** Сәтсіз болса null (қате экраны — pdf_load_failed). */
    suspend fun load(context: Context, url: String): List<Bitmap>? {
        val file = download(context, url) ?: return null
        return render(file)
    }

    private suspend fun download(context: Context, url: String): File? =
        withContext(Dispatchers.IO) {
            try {
                val target = File(context.cacheDir, "pdf_${url.hashCode()}.pdf")
                if (target.exists() && target.length() > 0L) return@withContext target
                val response = client.newCall(Request.Builder().url(url).build()).execute()
                val body = response.body ?: return@withContext null
                body.byteStream().use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                target
            } catch (_: Exception) {
                null
            }
        }

    /** Әр бет — экран еніне сәйкес bitmap (PdfRenderer.Page AutoCloseable — API 35+). */
    private suspend fun render(file: File): List<Bitmap>? =
        withContext(Dispatchers.IO) {
            try {
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                val targetWidth = 1080
                val result = ArrayList<Bitmap>(renderer.pageCount)
                try {
                    for (i in 0 until renderer.pageCount) {
                        val page = renderer.openPage(i)
                        try {
                            val scale = targetWidth.toFloat() / page.width
                            val bitmap = Bitmap.createBitmap(
                                targetWidth,
                                (page.height * scale).toInt().coerceAtLeast(1),
                                Bitmap.Config.ARGB_8888,
                            )
                            // PDF мөлдір фонмен рендерленеді — ақ қағаз бояймыз.
                            Canvas(bitmap).drawColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            result.add(bitmap)
                        } finally {
                            page.close()
                        }
                    }
                } finally {
                    renderer.close()
                    fd.close()
                }
                result
            } catch (_: Exception) {
                null
            }
        }
}