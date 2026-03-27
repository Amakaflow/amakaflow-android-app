package com.amakaflow.companion.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.model.ChallengeType
import com.amakaflow.companion.data.model.CreateChallengeRequest
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaSpacing

@Composable
fun CreateChallengeScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ChallengesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ChallengeType.VOLUME) }
    var target by remember { mutableStateOf("") }
    var targetUnit by remember { mutableStateOf("kg") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var isTeamMode by remember { mutableStateOf(false) }

    val isFormValid = title.isNotBlank() && target.isNotBlank() && target.toDoubleOrNull() != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("create_challenge_screen")
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmakaSpacing.sm.dp, vertical = AmakaSpacing.sm.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AmakaColors.textPrimary
                )
            }
            Text(
                "New Challenge",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.textPrimary
            )
        }

        HorizontalDivider(color = AmakaColors.borderLight)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(AmakaSpacing.md.dp),
            verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
        ) {
            // Title
            FormField(label = "Challenge Title") {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g., 10k Volume Week", color = AmakaColors.textTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = challengeTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }

            // Type selector
            FormField(label = "Challenge Type") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ChallengeType.entries.forEach { type ->
                        val color = challengeTypeColor(type)
                        Surface(
                            onClick = {
                                selectedType = type
                                targetUnit = when (type) {
                                    ChallengeType.VOLUME -> "kg"
                                    ChallengeType.CONSISTENCY -> "days"
                                    ChallengeType.PR -> "kg"
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedType == type) color else AmakaColors.surfaceElevated,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                type.displayName,
                                modifier = Modifier.padding(vertical = 10.dp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selectedType == type) Color.White else AmakaColors.textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Target
            FormField(label = "Target") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        placeholder = { Text("e.g., 10000", color = AmakaColors.textTertiary) },
                        modifier = Modifier.weight(1f),
                        colors = challengeTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Text(targetUnit, color = AmakaColors.textSecondary, modifier = Modifier.width(40.dp))
                }
            }

            // Description
            FormField(label = "Description (optional)") {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("What's the challenge about?", color = AmakaColors.textTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = challengeTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2,
                    maxLines = 4
                )
            }

            // Start date
            FormField(label = "Start Date (YYYY-MM-DD)") {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    placeholder = { Text("2026-04-01", color = AmakaColors.textTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = challengeTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }

            // End date
            FormField(label = "End Date (YYYY-MM-DD)") {
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    placeholder = { Text("2026-04-07", color = AmakaColors.textTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = challengeTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }

            // Team mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Team Mode", fontWeight = FontWeight.SemiBold, color = AmakaColors.textPrimary)
                    Text(
                        "Participants work together toward the target",
                        fontSize = 12.sp,
                        color = AmakaColors.textSecondary
                    )
                }
                Switch(
                    checked = isTeamMode,
                    onCheckedChange = { isTeamMode = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = AmakaColors.accentBlue)
                )
            }

            // Error
            state.createError?.let {
                Text(it, fontSize = 13.sp, color = AmakaColors.accentRed)
            }

            // Create button
            Button(
                onClick = {
                    val targetValue = target.toDoubleOrNull() ?: return@Button
                    viewModel.createChallenge(
                        CreateChallengeRequest(
                            title = title.trim(),
                            type = selectedType,
                            description = description.ifBlank { null },
                            target = targetValue,
                            targetUnit = targetUnit,
                            startDate = startDate.ifBlank { "2026-04-01" },
                            endDate = endDate.ifBlank { "2026-04-07" },
                            isTeamMode = isTeamMode
                        ),
                        onSuccess = onNavigateBack
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = isFormValid && !state.isCreating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmakaColors.accentBlue,
                    disabledContainerColor = AmakaColors.surfaceElevated
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create Challenge", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FormField(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AmakaColors.textSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        content()
    }
}

@Composable
private fun challengeTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AmakaColors.textPrimary,
    unfocusedTextColor = AmakaColors.textPrimary,
    focusedBorderColor = AmakaColors.accentBlue,
    unfocusedBorderColor = AmakaColors.borderLight,
    focusedContainerColor = AmakaColors.surfaceElevated,
    unfocusedContainerColor = AmakaColors.surfaceElevated,
    cursorColor = AmakaColors.accentBlue
)
