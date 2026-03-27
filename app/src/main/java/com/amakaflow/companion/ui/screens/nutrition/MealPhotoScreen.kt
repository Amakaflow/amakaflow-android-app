package com.amakaflow.companion.ui.screens.nutrition

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amakaflow.companion.data.nutrition.PhotoAnalysisResponse
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing
import java.io.ByteArrayOutputStream

/**
 * AMA-1294: Meal photo analysis screen.
 * Camera/photo picker -> sends to API -> shows estimated macros with confidence range -> one-tap add.
 */
@Composable
fun MealPhotoScreen(
    uiState: FoodLoggingUiState,
    onAnalyzePhoto: (String) -> Unit,
    onAddToDaily: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            bytes?.let { data ->
                // Resize to reduce payload
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                val scaled = Bitmap.createScaledBitmap(bitmap, 800, (800 * bitmap.height / bitmap.width), true)
                val baos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                onAnalyzePhoto(base64)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            val scaled = Bitmap.createScaledBitmap(it, 800, (800 * it.height / it.width), true)
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            onAnalyzePhoto(base64)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AmakaSpacing.md.dp)
            .testTag("meal_photo_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Snap a photo of your meal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AmakaColors.textPrimary
        )

        Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))

        // Camera / Gallery buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
        ) {
            Button(
                onClick = { cameraLauncher.launch(null) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("photo_camera_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmakaColors.accentBlue
                )
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(AmakaSpacing.sm.dp))
                Text("Camera")
            }

            OutlinedButton(
                onClick = { photoPickerLauncher.launch("image/*") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("photo_gallery_btn"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AmakaColors.accentBlue
                )
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(AmakaSpacing.sm.dp))
                Text("Gallery")
            }
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))

        // Loading state
        if (uiState.isAnalyzing) {
            CircularProgressIndicator(
                color = AmakaColors.accentBlue,
                modifier = Modifier.testTag("photo_loading")
            )
            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
            Text(
                text = "Analyzing your meal...",
                style = MaterialTheme.typography.bodyMedium,
                color = AmakaColors.textSecondary
            )
        }

        // Result
        uiState.photoResult?.let { result ->
            PhotoResultCard(result = result)

            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

            if (!uiState.addedToDaily) {
                Button(
                    onClick = onAddToDaily,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("photo_add_btn"),
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
                    modifier = Modifier.testTag("photo_added_label")
                )
            }
        }

        // Error
        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = AmakaColors.accentRed,
                modifier = Modifier.testTag("photo_error")
            )
        }
    }
}

@Composable
private fun PhotoResultCard(result: PhotoAnalysisResponse) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("photo_result_card"),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(modifier = Modifier.padding(AmakaSpacing.md.dp)) {
            Text(
                text = result.description,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )

            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

            // Confidence badge
            val confidenceColor = when (result.confidence) {
                "high" -> AmakaColors.accentGreen
                "medium" -> AmakaColors.accentOrange
                else -> AmakaColors.accentRed
            }
            Text(
                text = "Confidence: ${result.confidence}",
                style = MaterialTheme.typography.labelMedium,
                color = confidenceColor
            )

            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

            // Calorie range
            Text(
                text = "${result.caloriesRange.first()} - ${result.caloriesRange.last()} kcal",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.accentBlue
            )

            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

            // Macro breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroChip(value = "${result.proteinG.toInt()}g", label = "Protein")
                MacroChip(value = "${result.carbsG.toInt()}g", label = "Carbs")
                MacroChip(value = "${result.fatG.toInt()}g", label = "Fat")
            }
        }
    }
}

@Composable
private fun MacroChip(value: String, label: String) {
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
