package com.radianceph.pos.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.radianceph.pos.Words
import java.util.concurrent.Executors

/** What the camera reads: the owner's link QR, or the shop's item barcodes */
enum class Codes { QR, ITEMS }

/**
 * The camera reading codes on the phone itself (ML Kit's bundled model: offline, no Google Play): [onFound] gets each
 * code once; the same one held under the camera is not sent again for [again] ms. [paused] stops reading (a result on
 * screen). Asks for the camera once.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun Scanner(codes: Codes, onFound: (String) -> Unit, modifier: Modifier = Modifier, paused: Boolean = false, again: Long = 1500) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var allowed by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var asked by remember { mutableStateOf(false) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it; asked = true }
    LaunchedEffect(Unit) { if (!allowed) ask.launch(Manifest.permission.CAMERA) }
    val found by rememberUpdatedState(onFound)
    val stopped by rememberUpdatedState(paused)
    Box(modifier.clip(RoundedCornerShape(24.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
        if (!allowed) {
            Text(Words.t(if (asked) "camera_denied" else "camera_ask"), color = Color.White, modifier = Modifier.padding(24.dp))
            return@Box
        }
        val worker = remember { Executors.newSingleThreadExecutor() }
        val reader = remember {
            val formats = if (codes == Codes.QR) intArrayOf(Barcode.FORMAT_QR_CODE) else intArrayOf(
                Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39, Barcode.FORMAT_CODE_93, Barcode.FORMAT_ITF, Barcode.FORMAT_QR_CODE, Barcode.FORMAT_DATA_MATRIX,
            )
            BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(formats.first(), *formats.drop(1).toIntArray()).build())
        }
        DisposableEffect(Unit) { onDispose { worker.shutdown(); reader.close() } }
        var last by remember { mutableStateOf("") }
        var lastAt by remember { mutableStateOf(0L) }
        AndroidView(factory = { c ->
            val view = PreviewView(c)
            val future = ProcessCameraProvider.getInstance(c)
            future.addListener({
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(worker) { proxy ->
                    val media = proxy.image
                    if (media == null || stopped) {
                        proxy.close()
                        return@setAnalyzer
                    }
                    reader.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                        .addOnSuccessListener { list ->
                            val text = list.firstNotNullOfOrNull { it.rawValue?.trim()?.takeIf { v -> v.isNotEmpty() } }
                            val now = System.currentTimeMillis()
                            if (text != null && !stopped && (text != last || now - lastAt > again)) {
                                last = text
                                lastAt = now
                                ContextCompat.getMainExecutor(c).execute { found(text) }
                            }
                        }
                        .addOnCompleteListener { proxy.close() }
                }
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }
            }, ContextCompat.getMainExecutor(c))
            view
        }, onRelease = { runCatching { ProcessCameraProvider.getInstance(it.context).get().unbindAll() } }, modifier = Modifier.fillMaxSize())
    }
}
