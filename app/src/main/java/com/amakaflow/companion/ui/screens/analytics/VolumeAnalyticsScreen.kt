package com.amakaflow.companion.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.model.VolumeDataPoint
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing
import kotlin.math.abs

// ---------------------------------------------------------------------------
// Muscle group colour palette (consistent across bar chart + breakdown list)
// ---------------------------------------------------------------------------
private val MUSCLE_COLORS = listOf(
    AmakaColors.accentBlue,
    AmakaColors.accentGreen,
    AmakaColors.accentOrange,
    AmakaColors.accentPurple,
    AmakaColors.accentRed,
    AmakaColors.accentYellow,
    Color(0xFF06B6D4), // cyan
    Color(0xFFEC4899), // pink
    Color(0xFF14B8A6), // teal
    Color(0xFFF59E0B), // amber
)

private fun muscleColor(index: Int): Color = MUSCLE_COLORS[index % MUSCLE_COLORS.size]

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeAnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: VolumeAnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Volume Analytics",
                        color = AmakaColors.textPrimary,
                        fontWeight = FontWeight.SemiBold
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
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = AmakaColors.textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AmakaColors.background,
                    titleContentColor = AmakaColors.textPrimary
                )
            )
        },
        containerColor = AmakaColors.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("volume_analytics_screen"),
            contentPadding = PaddingValues(
                horizontal = AmakaSpacing.md.dp,
                vertical = AmakaSpacing.sm.dp
            ),
            verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
        ) {
            // Period selector
            item {
                PeriodSelector(
                    selected = uiState.selectedPeriod,
                    onSelect = { viewModel.changePeriod(it) }
                )
            }

            // Loading / error
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AmakaColors.accentBlue)
                    }
                }
                return@LazyColumn
            }

            if (uiState.errorMessage != null) {
                item {
                    ErrorCard(message = uiState.errorMessage!!, onRetry = { viewModel.refresh() })
                }
                return@LazyColumn
            }

            val data = uiState.currentData
            if (data == null) {
                item {
                    EmptyCard()
                }
                return@LazyColumn
            }

            // Summary cards
            item {
                SummaryRow(uiState = uiState, viewModel = viewModel)
            }

            // Stacked bar chart
            item {
                SectionHeader("Volume Over Time")
            }
            item {
                StackedBarChartCard(dataPoints = data.data)
            }

            // Balance indicators
            item {
                SectionHeader("Training Balance")
            }

            val pushPull = viewModel.pushPullRatio(uiState)
            if (pushPull != null) {
                item {
                    BalanceCard(
                        label = "Push / Pull",
                        ratio = pushPull,
                        leftLabel = "Push",
                        rightLabel = "Pull"
                    )
                }
            }

            val upperLower = viewModel.upperLowerRatio(uiState)
            if (upperLower != null) {
                item {
                    BalanceCard(
                        label = "Upper / Lower",
                        ratio = upperLower,
                        leftLabel = "Upper",
                        rightLabel = "Lower"
                    )
                }
            }

            if (pushPull == null && upperLower == null) {
                item {
                    SurfaceCard {
                        Text(
                            "Not enough data to compute balance.",
                            color = AmakaColors.textSecondary,
                            modifier = Modifier.padding(AmakaSpacing.md.dp)
                        )
                    }
                }
            }

            // Muscle group breakdown
            val sorted = viewModel.sortedMuscleGroups(uiState)
            if (sorted.isNotEmpty()) {
                item { SectionHeader("Muscle Group Breakdown") }
                val max = sorted.first().second
                // Build a stable color map using the same alphabetical ordering the chart uses
                val chartMuscleOrder = sorted.map { it.first }.sorted()
                val muscleColorIndex = chartMuscleOrder.withIndex().associate { (idx, name) -> name to idx }
                items(sorted) { (muscle, volume) ->
                    MuscleGroupRow(
                        muscle = muscle,
                        volume = volume,
                        maxVolume = max,
                        colorIndex = muscleColorIndex[muscle] ?: 0
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp)) }
        }
    }
}

// ---------------------------------------------------------------------------
// Period selector
// ---------------------------------------------------------------------------

@Composable
private fun PeriodSelector(
    selected: AnalyticsPeriod,
    onSelect: (AnalyticsPeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AmakaCornerRadius.md.dp))
            .background(AmakaColors.surface),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        AnalyticsPeriod.entries.forEach { period ->
            val isSelected = period == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(AmakaCornerRadius.md.dp))
                    .background(
                        if (isSelected) AmakaColors.accentBlue.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .then(
                        if (isSelected) Modifier.border(
                            1.dp,
                            AmakaColors.accentBlue,
                            RoundedCornerShape(AmakaCornerRadius.md.dp)
                        ) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = { onSelect(period) }) {
                    Text(
                        text = period.label,
                        color = if (isSelected) AmakaColors.accentBlue else AmakaColors.textSecondary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Summary row
// ---------------------------------------------------------------------------

@Composable
private fun SummaryRow(
    uiState: VolumeAnalyticsUiState,
    viewModel: VolumeAnalyticsViewModel
) {
    val summary = uiState.currentData?.summary ?: return
    val changePct = viewModel.volumeChangePct(uiState)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
    ) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "Volume",
            value = formatVolume(summary.totalVolume),
            badge = changePct?.let { formatChangePct(it) },
            badgePositive = changePct?.let { it >= 0 }
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "Sets",
            value = summary.totalSets.toString()
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "Reps",
            value = summary.totalReps.toString()
        )
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    badge: String? = null,
    badgePositive: Boolean? = null
) {
    SurfaceCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(AmakaSpacing.md.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = AmakaColors.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.textPrimary,
                textAlign = TextAlign.Center
            )
            if (badge != null && badgePositive != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (badgePositive) AmakaColors.accentGreen else AmakaColors.accentRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Stacked bar chart
// ---------------------------------------------------------------------------

@Composable
private fun StackedBarChartCard(dataPoints: List<VolumeDataPoint>) {
    if (dataPoints.isEmpty()) {
        SurfaceCard {
            Text(
                "No workout data for this period.",
                color = AmakaColors.textSecondary,
                modifier = Modifier.padding(AmakaSpacing.md.dp)
            )
        }
        return
    }

    // Group by period, then by muscle group
    val periods = dataPoints.map { it.period }.distinct().sorted()
    val muscleGroups = dataPoints.map { it.muscleGroup }.distinct().sorted()

    // For each period, map muscle -> volume
    val byPeriod: Map<String, Map<String, Double>> = periods.associateWith { p ->
        dataPoints.filter { it.period == p }.associate { it.muscleGroup to it.totalVolume }
    }

    val maxTotal = periods.maxOfOrNull { p ->
        byPeriod[p]?.values?.sum() ?: 0.0
    } ?: 1.0

    SurfaceCard {
        Column(modifier = Modifier.padding(AmakaSpacing.md.dp)) {
            // Legend
            MuscleGroupLegend(muscleGroups = muscleGroups)
            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

            // Bar chart — fixed height, Canvas-drawn
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val barCount = periods.size
                val totalGap = (barCount - 1) * 6f
                val barWidth = if (barCount > 0) (canvasWidth - totalGap) / barCount else canvasWidth

                periods.forEachIndexed { idx, period ->
                    val x = idx * (barWidth + 6f)
                    val muscleMap = byPeriod[period] ?: emptyMap()
                    val total = muscleMap.values.sum()
                    var yOffset = canvasHeight

                    muscleGroups.forEachIndexed { mIdx, muscle ->
                        val vol = muscleMap[muscle] ?: 0.0
                        if (vol <= 0.0) return@forEachIndexed
                        val segHeight = ((vol / maxTotal) * canvasHeight).toFloat()
                        yOffset -= segHeight
                        drawRoundRect(
                            color = muscleColor(mIdx),
                            topLeft = Offset(x, yOffset),
                            size = Size(barWidth, segHeight),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                    }
                }
            }

            // X-axis labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val labelPeriods = if (periods.size <= 7) periods else {
                    // Show ~5 evenly spaced labels
                    val step = (periods.size / 4).coerceAtLeast(1)
                    periods.filterIndexed { idx, _ -> idx % step == 0 || idx == periods.lastIndex }
                }
                labelPeriods.forEach { p ->
                    Text(
                        text = formatPeriodLabel(p),
                        style = MaterialTheme.typography.labelSmall,
                        color = AmakaColors.textTertiary,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MuscleGroupLegend(muscleGroups: List<String>) {
    val chunked = muscleGroups.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        chunked.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
            ) {
                row.forEachIndexed { idx, muscle ->
                    val globalIdx = muscleGroups.indexOf(muscle)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(muscleColor(globalIdx), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = muscle.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = AmakaColors.textSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Balance indicator
// ---------------------------------------------------------------------------

@Composable
private fun BalanceCard(
    label: String,
    ratio: Double,
    leftLabel: String,
    rightLabel: String
) {
    SurfaceCard {
        Column(modifier = Modifier.padding(AmakaSpacing.md.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary
                )
                BalanceStatusBadge(ratio = ratio)
            }

            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

            // Horizontal balance bar
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                val w = size.width
                val h = size.height

                // Background
                drawRoundRect(
                    color = AmakaColors.surfaceElevated,
                    size = Size(w, h),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // Balanced zone (40–60%)
                val zoneStart = w * 0.4f
                val zoneEnd = w * 0.6f
                drawRect(
                    color = AmakaColors.accentGreen.copy(alpha = 0.15f),
                    topLeft = Offset(zoneStart, 0f),
                    size = Size(zoneEnd - zoneStart, h)
                )

                // Ratio indicator
                val indicatorX = (w * ratio.toFloat()).coerceIn(4f, w - 4f)
                drawRoundRect(
                    color = balanceColor(ratio),
                    topLeft = Offset(indicatorX - 3f, 2f),
                    size = Size(6f, h - 4f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$leftLabel ${(ratio * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmakaColors.textSecondary
                )
                Text(
                    text = "$rightLabel ${((1 - ratio) * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmakaColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun BalanceStatusBadge(ratio: Double) {
    val deviation = abs(ratio - 0.5)
    val (label, color) = when {
        deviation <= 0.1 -> "Balanced" to AmakaColors.accentGreen
        deviation <= 0.2 -> "Slightly Off" to AmakaColors.accentOrange
        else -> "Needs Attention" to AmakaColors.accentRed
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(AmakaCornerRadius.sm.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(AmakaCornerRadius.sm.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun balanceColor(ratio: Double): Color {
    val deviation = abs(ratio - 0.5)
    return when {
        deviation <= 0.1 -> AmakaColors.accentGreen
        deviation <= 0.2 -> AmakaColors.accentOrange
        else -> AmakaColors.accentRed
    }
}

// ---------------------------------------------------------------------------
// Muscle group breakdown
// ---------------------------------------------------------------------------

@Composable
private fun MuscleGroupRow(
    muscle: String,
    volume: Double,
    maxVolume: Double,
    colorIndex: Int
) {
    val progress = if (maxVolume > 0) (volume / maxVolume).toFloat() else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(muscleColor(colorIndex), RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
        Text(
            text = muscle.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodySmall,
            color = AmakaColors.textPrimary,
            modifier = Modifier.width(80.dp)
        )
        Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = muscleColor(colorIndex),
            trackColor = AmakaColors.surfaceElevated
        )
        Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
        Text(
            text = formatVolume(volume),
            style = MaterialTheme.typography.labelSmall,
            color = AmakaColors.textSecondary,
            modifier = Modifier.width(52.dp),
            textAlign = TextAlign.End
        )
    }
}

// ---------------------------------------------------------------------------
// Shared UI helpers
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = AmakaColors.textPrimary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun SurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AmakaCornerRadius.md.dp))
            .background(AmakaColors.surface)
    ) {
        content()
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    SurfaceCard {
        Column(
            modifier = Modifier.padding(AmakaSpacing.md.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                color = AmakaColors.accentRed,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
            TextButton(onClick = onRetry) {
                Text("Retry", color = AmakaColors.accentBlue)
            }
        }
    }
}

@Composable
private fun EmptyCard() {
    SurfaceCard {
        Text(
            text = "No volume data for this period.",
            color = AmakaColors.textSecondary,
            modifier = Modifier.padding(AmakaSpacing.md.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Formatting helpers
// ---------------------------------------------------------------------------

private fun formatVolume(kg: Double): String {
    return when {
        kg >= 1_000_000 -> "${"%.1f".format(kg / 1_000_000)}M kg"
        kg >= 1_000 -> "${"%.1f".format(kg / 1_000)}k kg"
        else -> "${"%.0f".format(kg)} kg"
    }
}

private fun formatChangePct(pct: Double): String {
    val sign = if (pct >= 0) "+" else ""
    return "$sign${"%.1f".format(pct)}%"
}

private fun formatPeriodLabel(period: String): String {
    // e.g. "2026-03-28" -> "Mar 28", "2026-W13" -> "W13", "2026-03" -> "Mar"
    return try {
        when {
            period.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
                val parts = period.split("-")
                val month = MONTH_ABBREV.getOrElse(parts[1].toInt() - 1) { parts[1] }
                "$month ${parts[2].trimStart('0')}"
            }
            period.matches(Regex("\\d{4}-W\\d+")) -> period.substringAfter("-")
            period.matches(Regex("\\d{4}-\\d{2}")) -> {
                val parts = period.split("-")
                MONTH_ABBREV.getOrElse(parts[1].toInt() - 1) { parts[1] }
            }
            else -> period
        }
    } catch (e: Exception) {
        period
    }
}

private val MONTH_ABBREV = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)
