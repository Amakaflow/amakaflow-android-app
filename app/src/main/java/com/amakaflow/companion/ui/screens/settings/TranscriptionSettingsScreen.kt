package com.amakaflow.companion.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

enum class TranscriptionProvider(
    val title: String,
    val description: String,
    val price: String,
    val priceColor: Color
) {
    ON_DEVICE("On-Device", "Privacy-first. Audio never leaves device.", "FREE", AmakaColors.accentGreen),
    DEEPGRAM("Deepgram", "Best accuracy & accent handling.", "~\$0.01/min", AmakaColors.textSecondary),
    ASSEMBLY_AI("AssemblyAI", "Good accuracy, lowest cost.", "~\$0.005/min", AmakaColors.textSecondary),
    SMART("Smart", "On-device first, cloud fallback if needed.", "AUTO", AmakaColors.textSecondary)
}

enum class AccentLanguage(val displayName: String) {
    US_ENGLISH("US English"),
    UK_ENGLISH("UK English"),
    AUSTRALIAN_ENGLISH("Australian English"),
    INDIAN_ENGLISH("Indian English"),
    SOUTH_AFRICAN_ENGLISH("South African English"),
    NIGERIAN_ENGLISH("Nigerian English"),
    OTHER_ENGLISH("Other accented English")
}

enum class FallbackProvider(val displayName: String) {
    DEEPGRAM("Deepgram (Best Accuracy)"),
    ASSEMBLY_AI("AssemblyAI (Budget)")
}

data class CorrectionEntry(
    val wrongPhrase: String,
    val correctPhrase: String
)

data class CustomTerm(
    val term: String
)

@Composable
fun TranscriptionSettingsScreen(
    onDismiss: () -> Unit
) {
    var selectedProvider by remember { mutableStateOf(TranscriptionProvider.SMART) }
    var selectedAccent by remember { mutableStateOf(AccentLanguage.US_ENGLISH) }
    var cloudFallbackEnabled by remember { mutableStateOf(true) }
    var selectedFallbackProvider by remember { mutableStateOf(FallbackProvider.DEEPGRAM) }
    var showFallbackDropdown by remember { mutableStateOf(false) }
    var corrections by remember { mutableStateOf<List<CorrectionEntry>>(emptyList()) }
    var customTerms by remember { mutableStateOf<List<CustomTerm>>(emptyList()) }
    var showAddCorrectionDialog by remember { mutableStateOf(false) }
    var showAddTermDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("transcription_settings_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.md.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(60.dp))

            Text(
                text = "Voice Transcription",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )

            TextButton(onClick = onDismiss) {
                Text(
                    text = "Done",
                    color = AmakaColors.accentBlue,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = AmakaSpacing.md.dp),
            verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
        ) {
            // Transcription Provider section
            item {
                SectionHeader("Transcription Provider")
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AmakaColors.surface,
                    shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                ) {
                    Column {
                        TranscriptionProvider.entries.forEach { provider ->
                            ProviderOption(
                                provider = provider,
                                isSelected = selectedProvider == provider,
                                isRecommended = provider == TranscriptionProvider.SMART,
                                onClick = { selectedProvider = provider }
                            )
                            if (provider != TranscriptionProvider.SMART) {
                                HorizontalDivider(
                                    color = AmakaColors.borderLight,
                                    modifier = Modifier.padding(horizontal = AmakaSpacing.md.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Smart mode uses on-device first, then cloud if confidence is low.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textTertiary,
                    modifier = Modifier.padding(vertical = AmakaSpacing.sm.dp)
                )
            }

            // Accent / Language section
            item {
                Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
                SectionHeader("Accent / Language")
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AmakaColors.surface,
                    shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                ) {
                    Column {
                        AccentLanguage.entries.forEachIndexed { index, accent ->
                            AccentOption(
                                accent = accent,
                                isSelected = selectedAccent == accent,
                                onClick = { selectedAccent = accent }
                            )
                            if (index < AccentLanguage.entries.size - 1) {
                                HorizontalDivider(
                                    color = AmakaColors.borderLight,
                                    modifier = Modifier.padding(horizontal = AmakaSpacing.md.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Select your accent for better transcription accuracy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textTertiary,
                    modifier = Modifier.padding(vertical = AmakaSpacing.sm.dp)
                )
            }

            // Cloud Fallback section
            item {
                Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
                SectionHeader("Cloud Fallback")
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AmakaColors.surface,
                    shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                ) {
                    Column {
                        // Enable Cloud Fallback toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AmakaSpacing.md.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Enable Cloud Fallback",
                                style = MaterialTheme.typography.bodyLarge,
                                color = AmakaColors.textPrimary
                            )
                            Switch(
                                checked = cloudFallbackEnabled,
                                onCheckedChange = { cloudFallbackEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AmakaColors.textPrimary,
                                    checkedTrackColor = AmakaColors.accentBlue,
                                    uncheckedThumbColor = AmakaColors.textTertiary,
                                    uncheckedTrackColor = AmakaColors.surface
                                )
                            )
                        }

                        if (cloudFallbackEnabled) {
                            HorizontalDivider(
                                color = AmakaColors.borderLight,
                                modifier = Modifier.padding(horizontal = AmakaSpacing.md.dp)
                            )

                            // Fallback Provider dropdown
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AmakaSpacing.md.dp)
                            ) {
                                Text(
                                    text = "Fallback Provider",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmakaColors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(AmakaSpacing.xs.dp))

                                Box {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(AmakaCornerRadius.sm.dp))
                                            .clickable { showFallbackDropdown = true }
                                            .padding(vertical = AmakaSpacing.xs.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedFallbackProvider.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AmakaColors.accentBlue
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = AmakaColors.accentBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showFallbackDropdown,
                                        onDismissRequest = { showFallbackDropdown = false },
                                        containerColor = AmakaColors.surface
                                    ) {
                                        FallbackProvider.entries.forEach { provider ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (selectedFallbackProvider == provider) {
                                                            Icon(
                                                                imageVector = Icons.Filled.Check,
                                                                contentDescription = null,
                                                                tint = AmakaColors.textPrimary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                                                        }
                                                        Text(
                                                            text = provider.displayName,
                                                            color = AmakaColors.textPrimary
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectedFallbackProvider = provider
                                                    showFallbackDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "When on-device confidence is below 80%, automatically try cloud transcription for better results.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textTertiary,
                    modifier = Modifier.padding(vertical = AmakaSpacing.sm.dp)
                )
            }

            // Personal Dictionary section
            item {
                Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
                SectionHeader("Personal Dictionary")
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AmakaColors.surface,
                    shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(AmakaSpacing.md.dp)
                    ) {
                        if (corrections.isEmpty()) {
                            Text(
                                text = "No corrections added yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmakaColors.textTertiary
                            )
                        } else {
                            corrections.forEach { correction ->
                                Text(
                                    text = "${correction.wrongPhrase} → ${correction.correctPhrase}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AmakaColors.textPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(AmakaCornerRadius.sm.dp))
                                .clickable { showAddCorrectionDialog = true }
                                .padding(vertical = AmakaSpacing.xs.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = AmakaColors.accentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(AmakaSpacing.xs.dp))
                            Text(
                                text = "Add Correction",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmakaColors.accentBlue
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Add corrections for phrases that are consistently misheard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textTertiary,
                    modifier = Modifier.padding(vertical = AmakaSpacing.sm.dp)
                )
            }

            // Custom Terms section
            item {
                Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
                SectionHeader("Custom Terms")
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AmakaColors.surface,
                    shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(AmakaSpacing.md.dp)
                    ) {
                        if (customTerms.isEmpty()) {
                            Text(
                                text = "No custom terms added yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmakaColors.textTertiary
                            )
                        } else {
                            customTerms.forEach { term ->
                                Text(
                                    text = term.term,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AmakaColors.textPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(AmakaCornerRadius.sm.dp))
                                .clickable { showAddTermDialog = true }
                                .padding(vertical = AmakaSpacing.xs.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = AmakaColors.accentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(AmakaSpacing.xs.dp))
                            Text(
                                text = "Add Custom Term",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmakaColors.accentBlue
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Add exercise names or terms specific to your workouts",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textTertiary,
                    modifier = Modifier.padding(vertical = AmakaSpacing.sm.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))
            }
        }
    }

    // Add Correction Dialog
    if (showAddCorrectionDialog) {
        AddCorrectionDialog(
            onDismiss = { showAddCorrectionDialog = false },
            onAdd = { wrongPhrase, correctPhrase ->
                corrections = corrections + CorrectionEntry(wrongPhrase, correctPhrase)
                showAddCorrectionDialog = false
            }
        )
    }

    // Add Custom Term Dialog
    if (showAddTermDialog) {
        AddCustomTermDialog(
            onDismiss = { showAddTermDialog = false },
            onAdd = { term ->
                customTerms = customTerms + CustomTerm(term)
                showAddTermDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = AmakaColors.textTertiary,
        modifier = Modifier.padding(vertical = AmakaSpacing.sm.dp)
    )
}

@Composable
private fun ProviderOption(
    provider: TranscriptionProvider,
    isSelected: Boolean,
    isRecommended: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(AmakaSpacing.md.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = provider.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = AmakaColors.textPrimary
                )
                if (isRecommended) {
                    Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                    Surface(
                        color = AmakaColors.accentGreen,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Recommended",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmakaColors.background,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = provider.description,
                style = MaterialTheme.typography.bodySmall,
                color = AmakaColors.textSecondary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = provider.price,
                style = MaterialTheme.typography.bodyMedium,
                color = provider.priceColor
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = AmakaColors.accentBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AccentOption(
    accent: AccentLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(AmakaSpacing.md.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = accent.displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = AmakaColors.textPrimary
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = AmakaColors.accentBlue,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AddCorrectionDialog(
    onDismiss: () -> Unit,
    onAdd: (wrongPhrase: String, correctPhrase: String) -> Unit
) {
    var wrongPhrase by remember { mutableStateOf("") }
    var correctPhrase by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = AmakaColors.accentBlue)
                }
                Text(
                    text = "Add Correction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary
                )
                TextButton(
                    onClick = { onAdd(wrongPhrase, correctPhrase) },
                    enabled = wrongPhrase.isNotBlank() && correctPhrase.isNotBlank()
                ) {
                    Text(
                        "Add",
                        color = if (wrongPhrase.isNotBlank() && correctPhrase.isNotBlank())
                            AmakaColors.accentBlue else AmakaColors.textTertiary
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    text = "Add Correction",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmakaColors.textSecondary
                )
                Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

                OutlinedTextField(
                    value = wrongPhrase,
                    onValueChange = { wrongPhrase = it },
                    placeholder = { Text("Wrong phrase", color = AmakaColors.textTertiary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmakaColors.accentBlue,
                        unfocusedBorderColor = AmakaColors.borderLight,
                        focusedTextColor = AmakaColors.textPrimary,
                        unfocusedTextColor = AmakaColors.textPrimary,
                        cursorColor = AmakaColors.accentBlue
                    )
                )

                Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

                OutlinedTextField(
                    value = correctPhrase,
                    onValueChange = { correctPhrase = it },
                    placeholder = { Text("Correct phrase", color = AmakaColors.textTertiary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmakaColors.accentBlue,
                        unfocusedBorderColor = AmakaColors.borderLight,
                        focusedTextColor = AmakaColors.textPrimary,
                        unfocusedTextColor = AmakaColors.textPrimary,
                        cursorColor = AmakaColors.accentBlue
                    )
                )

                Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

                Text(
                    text = "When the wrong phrase is detected, it will be replaced with the correct phrase.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textTertiary
                )
            }
        },
        confirmButton = {},
        containerColor = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.lg.dp)
    )
}

@Composable
private fun AddCustomTermDialog(
    onDismiss: () -> Unit,
    onAdd: (term: String) -> Unit
) {
    var term by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = AmakaColors.accentBlue)
                }
                Text(
                    text = "Add Custom Term",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary
                )
                TextButton(
                    onClick = { onAdd(term) },
                    enabled = term.isNotBlank()
                ) {
                    Text(
                        "Add",
                        color = if (term.isNotBlank()) AmakaColors.accentBlue else AmakaColors.textTertiary
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    text = "Custom Term",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmakaColors.textSecondary
                )
                Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

                OutlinedTextField(
                    value = term,
                    onValueChange = { term = it },
                    placeholder = { Text("Exercise or term name", color = AmakaColors.textTertiary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmakaColors.accentBlue,
                        unfocusedBorderColor = AmakaColors.borderLight,
                        focusedTextColor = AmakaColors.textPrimary,
                        unfocusedTextColor = AmakaColors.textPrimary,
                        cursorColor = AmakaColors.accentBlue
                    )
                )

                Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

                Text(
                    text = "Add exercise names or terms that should be recognized accurately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textTertiary
                )
            }
        },
        confirmButton = {},
        containerColor = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.lg.dp)
    )
}
