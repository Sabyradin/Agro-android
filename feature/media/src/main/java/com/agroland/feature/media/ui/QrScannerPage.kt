package com.agroland.feature.media.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.agroland.core.l10n.R as L10nR
import com.agroland.feature.media.data.QrUrlParser
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * QrScannerPage (Flutter 1:1, mobile_scanner → CameraX + ML Kit):
 * қара фон, жасыл бұрыштар (250dp), «Agroland QR» тақырыбы, танылмаған
 * код — 3 секундта бір рет Toast. Жарнама id — QrUrlParser.
 */
@Composable
fun QrScannerPage(
    onBack: () -> Unit,
    onAnnouncementScanned: (Long) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanQrHint = stringResource(L10nR.string.scan_qr)
    val notRecognizedText = stringResource(L10nR.string.qr_not_recognized)
    val cameraUnavailableText = stringResource(L10nR.string.camera_unavailable)

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        permissionDenied = !granted
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var navigated by remember { mutableStateOf(false) }
    var lastUnrecognizedAt by remember { mutableStateOf(0L) }

    val onBarcode: (String?) -> Unit = remember(context) {
        callback@{ raw ->
            if (navigated || raw.isNullOrBlank()) return@callback
            val announcementId = QrUrlParser.extractAnnouncementId(raw)
            if (announcementId == null) {
                val now = System.currentTimeMillis()
                if (now - lastUnrecognizedAt > 3_000L) {
                    lastUnrecognizedAt = now
                    Toast.makeText(context, notRecognizedText, Toast.LENGTH_SHORT).show()
                }
                return@callback
            }
            navigated = true
            onAnnouncementScanned(announcementId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    startCamera(ctx, lifecycleOwner, previewView, onBarcode)
                    previewView
                },
            )
        } else {
            Text(
                text = cameraUnavailableText,
                color = Color.White,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 32.dp, vertical = 64.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Agroland QR",
                color = Green,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = scanQrHint,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
            )
            ScannerFrame(
                size = 250,
                color = Green,
            )
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 48.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                tint = Green,
            )
        }
    }
}

private val Green = Color(0xFF4CAF50)

/** CameraX Preview + ImageAnalysis → ML Kit BarcodeScanning. */
private fun startCamera(
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    onBarcode: (String?) -> Unit,
) {
    val providerFuture = ProcessCameraProvider.getInstance(context)
    providerFuture.addListener(
        {
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
                val scanner = BarcodeScanning.getClient(options)
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { image ->
                    processImage(image, scanner, onBarcode)
                }
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            } catch (_: Exception) {
                // камера қолжетімсіз — errorBuilder паритеті экран мәтінімен
            }
        },
        ContextCompat.getMainExecutor(context),
    )
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImage(
    image: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onBarcode: (String?) -> Unit,
) {
    val mediaImage = image.image
    if (mediaImage == null) {
        image.close()
        return
    }
    val input = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
    scanner.process(input)
        .addOnSuccessListener { barcodes ->
            onBarcode(barcodes.firstOrNull()?.rawValue)
        }
        .addOnCompleteListener { image.close() }
}

/** Flutter _ScannerOverlayPainter: төрт жасыл бұрыш. */
@Composable
private fun ScannerFrame(
    size: Int,
    color: Color,
) {
    val cornerLength = 36.dp
    val cornerWidth = 4.dp
    Canvas(modifier = Modifier.size(size.dp)) {
        val w = this.size.width
        val h = this.size.height
        val len = cornerLength.toPx()
        val stroke = cornerWidth.toPx()
        // Top-left
        drawLine(color, Offset(0f, len), Offset(0f, 0f), stroke, StrokeCap.Round)
        drawLine(color, Offset(0f, 0f), Offset(len, 0f), stroke, StrokeCap.Round)
        // Top-right
        drawLine(color, Offset(w - len, 0f), Offset(w, 0f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w, 0f), Offset(w, len), stroke, StrokeCap.Round)
        // Bottom-left
        drawLine(color, Offset(0f, h - len), Offset(0f, h), stroke, StrokeCap.Round)
        drawLine(color, Offset(0f, h), Offset(len, h), stroke, StrokeCap.Round)
        // Bottom-right
        drawLine(color, Offset(w, h - len), Offset(w, h), stroke, StrokeCap.Round)
        drawLine(color, Offset(w - len, h), Offset(w, h), stroke, StrokeCap.Round)
    }
}