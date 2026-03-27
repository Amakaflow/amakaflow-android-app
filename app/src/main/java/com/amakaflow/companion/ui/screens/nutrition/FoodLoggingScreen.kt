package com.amakaflow.companion.ui.screens.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaSpacing

/**
 * AMA-1294: Main food logging screen with tab navigation for three entry modes:
 * 1. AI meal photo analysis
 * 2. Barcode scanner
 * 3. NLP text entry
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodLoggingScreen(
    onNavigateBack: () -> Unit,
    viewModel: FoodLoggingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("food_logging_screen")
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = "Log Food",
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AmakaColors.textPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = AmakaColors.background
            )
        )

        // Mode tabs
        TabRow(
            selectedTabIndex = uiState.mode.ordinal,
            containerColor = AmakaColors.surface,
            contentColor = AmakaColors.accentBlue,
            modifier = Modifier.testTag("food_logging_tabs")
        ) {
            Tab(
                selected = uiState.mode == FoodLoggingMode.TEXT,
                onClick = { viewModel.setMode(FoodLoggingMode.TEXT) },
                modifier = Modifier.testTag("tab_text"),
                icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                text = { Text("Text") }
            )
            Tab(
                selected = uiState.mode == FoodLoggingMode.PHOTO,
                onClick = { viewModel.setMode(FoodLoggingMode.PHOTO) },
                modifier = Modifier.testTag("tab_photo"),
                icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
                text = { Text("Photo") }
            )
            Tab(
                selected = uiState.mode == FoodLoggingMode.BARCODE,
                onClick = { viewModel.setMode(FoodLoggingMode.BARCODE) },
                modifier = Modifier.testTag("tab_barcode"),
                icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = null) },
                text = { Text("Barcode") }
            )
        }

        // Content based on selected mode
        when (uiState.mode) {
            FoodLoggingMode.TEXT -> {
                TextFoodEntryScreen(
                    uiState = uiState,
                    onTextChanged = viewModel::setTextInput,
                    onParse = viewModel::parseText,
                    onAddToDaily = viewModel::addToDaily
                )
            }
            FoodLoggingMode.PHOTO -> {
                MealPhotoScreen(
                    uiState = uiState,
                    onAnalyzePhoto = viewModel::analyzePhoto,
                    onAddToDaily = viewModel::addToDaily
                )
            }
            FoodLoggingMode.BARCODE -> {
                BarcodeScannerScreen(
                    uiState = uiState,
                    onBarcodeScanned = viewModel::lookupBarcode,
                    onAddToDaily = viewModel::addToDaily
                )
            }
        }
    }
}
