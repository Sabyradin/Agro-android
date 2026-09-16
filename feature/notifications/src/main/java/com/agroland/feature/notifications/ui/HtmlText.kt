package com.agroland.feature.notifications.ui

import android.text.Html
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Text

/**
 * HTML мәтінін Compose-та көрсету (Flutter flutter_html баламасы):
 * Android Html.fromHtml → Spanned span-дарын AnnotatedString-ке аударады
 * (қалың/жанған/асты сызылған/сілтемелер/моно). Сілтемелер LinkAnnotation
 * арқылы кликтеледі — браузерде ашылады.
 */
object HtmlTextParser {

    /** Карта превьюі үшін — тегтерді алып тастаған жай мәтін. */
    fun toPlain(html: String): String =
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()

    fun toAnnotated(html: String, linkColor: Color, onLinkClick: (String) -> Unit): AnnotatedString {
        val spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY) as Spanned
        return buildAnnotatedString {
            append(spanned.toString())
            val text = spanned.toString()
            for (span in spanned.getSpans(0, text.length, Any::class.java)) {
                val start = spanned.getSpanStart(span).coerceIn(0, length)
                val end = spanned.getSpanEnd(span).coerceIn(0, length)
                if (start >= end) continue
                when (span) {
                    is StyleSpan -> when (span.style) {
                        android.graphics.Typeface.BOLD,
                        android.graphics.Typeface.BOLD_ITALIC,
                        -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                        android.graphics.Typeface.ITALIC,
                        -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                    }
                    is UnderlineSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                    is StrikethroughSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
                    is TypefaceSpan -> if (span.family == "monospace") {
                        addStyle(SpanStyle(fontFamily = FontFamily.Monospace), start, end)
                    }
                    is URLSpan -> {
                        val url = span.url
                        addStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline), start, end)
                        addLink(
                            LinkAnnotation.Clickable(
                                tag = url,
                                linkInteractionListener = { onLinkClick(url) },
                            ),
                            start,
                            end,
                        )
                    }
                }
            }
        }
    }
}

/** Толық HTML хабарлама мәтіні (SingleNotificationPage). */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    ellipsis: Boolean = false,
) {
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(html) { HtmlTextParser.toAnnotated(html, linkColor) { uriHandler.openUri(it) } }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
        maxLines = maxLines,
        overflow = if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip,
    )
}

/** Превью — тегтерсіз жай мәтін (карточкаларда 2 жол ellipsis). */
@Composable
fun PlainHtmlText(
    html: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = remember(html) { HtmlTextParser.toPlain(html) },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}