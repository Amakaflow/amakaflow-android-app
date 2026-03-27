package com.amakaflow.companion.ui.screens.nutrition

import android.Manifest
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amakaflow.companion.data.nutrition.BarcodeProductResponse
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.concurrent.Executors

private const val TAG = "BarcodeScannerScreen"

/**
 * AMA-1294: Barcode scanner screen.
 * Uses CameraX + ZXing to scan barcodes, looks up via Open Food Facts, one-tap add.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScannerScreen(
    uiState: FoodLoggingUiState,
    onBarcodeScanned: (String) -> Unit,
    onAddToDaily: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("barcode_scanner_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (cameraPermission.status.isGranted) {
            if (uiState.barcodeResult == null && !uiState.isAnalyzing) {
                // Show camera preview with barcode scanning
                Text(
                    text = "Point camera at a barcode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary,
                    modifier = Modifier.padding(AmakaSpacing.md.dp)
                )

                BarcodeCameraPreview(
                    onBarcodeDetected = onBarcodeScanned,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("barcode_camera_preview")
                )
            } else {
                // Show results
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(AmakaSpacing.md.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.isAnalyzing) {
                        Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))
                        CircularProgressIndicator(
                            color = AmakaColors.accentBlue,
                            modifier = Modifier.testTag("barcode_loading")
                        )
                        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                        Text(
                            text = "Looking up product...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmakaColors.textSecondary
                        )
                    }

                    uiState.barcodeResult?.let { result ->
                        BarcodeResultCard(result = result)

                        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

                        if (!uiState.addedToDaily) {
                            Button(
                                onClick = onAddToDaily,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("barcode_add_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmakaColors.accentGreen
                                )
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                                Spacer(Modifier.width(AmakaSpacing.sm.dp))
                                Text("Add to Daily Totals")
                            }
                        } else {
                            Text(
                                text = "Added to daily totals!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AmakaColors.accentGreen,
                                modifier = Modifier.testTag("barcode_added_label")
                            )
                        }
                    }

                    uiState.error?.let { error ->
                        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmakaColors.accentRed,
                            modifier = Modifier.testTag("barcode_error")
                        )
                    }
                }
            }
        } else {
            // Permission not granted
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AmakaSpacing.lg.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    tint = AmakaColors.textSecondary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
                Text(
                    text = "Camera permission needed to scan barcodes",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AmakaColors.textSecondary
                )
                Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
                Button(
                    onClick = { cameraPermission.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmakaColors.accentBlue
                    )
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
private fun BarcodeCameraPreview(
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lastScannedCode by remember { mutableStateOf("") }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val executor = Executors.newSingleThreadExecutor()
                val reader = MultiFormatReader().apply {
                    val hints = mapOf(
                        DecodeHintType.POSSIBLE_FORMATS to listOf(
                            BarcodeFormat.EAN_13,
                            BarcodeFormat.EAN_8,
                            BarcodeFormat.UPC_A,
                            BarcodeFormat.UPC_E,
                            BarcodeFormat.CODE_128,
                            BarcodeFormat.CODE_39
                        )
                    )
                    setHints(hints)
                }

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    val buffer = imageProxy.planes[0].buffer
                    val data = ByteArray(buffer.remaining())
                    buffer.get(data)

                    val source = PlanarYUVLuminanceSource(
                        data,
                        imageProxy.width,
                        imageProxy.height,
                        0, 0,
                        imageProxy.width,
                        imageProxy.height,
                        false
                    )
                    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                    try {
                        val result = reader.decode(binaryBitmap)
                        val code = result.text
                        if (code != lastScannedCode) {
                            lastScannedCode = code
                            onBarcodeDetected(code)
                        }
                    } catch (_: NotFoundException) {
                        // No barcode found in this frame
                    } catch (e: Exception) {
                        Log.w(TAG, "Barcode decode error", e)
                    }

                    imageProxy.close()
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Camera binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

@Composable
private fun BarcodeResultCard(result: BarcodeProductResponse) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("barcode_result_card"),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(modifier = Modifier.padding(AmakaSpacing.md.dp)) {
            Text(
                text = result.productName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )

            Spacer(modifier = Modifier.height(AmakaSpacing.xs.dp))

            Text(
                text = "Serving: ${result.servingSize}",
                style = MaterialTheme.typography.labelMedium,
                color = AmakaColors.textTertiary
            )

            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

            Text(
                text = "${result.calories} kcal",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.accentBlue
            )

            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroItem(value = "${result.protein.toInt()}g", label = "Protein")
                MacroItem(value = "${result.carbs.toInt()}g", label = "Carbs")
                MacroItem(value = "${result.fat.toInt()}g", label = "Fat")
            }
        }
    }
}

@Composable
private fun MacroItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AmakaColors.textPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AmakaColors.textTertiary
        )
    }
}
