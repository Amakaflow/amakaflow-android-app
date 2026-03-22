package com.amakaflow.companion.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.model.DayStatus
import com.amakaflow.companion.data.model.DayWorkoutSummary
import com.amakaflow.companion.data.model.ScheduleConflict
import com.amakaflow.companion.data.model.ConflictSeverity
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun CalendarScreen(
    onNavigateToWorkouts: () -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showMonthPicker by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val selectedDate = state.selectedDate

    // Get the week start (Sunday)
    val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

    // Get current day's data
    val currentDayState = state.dayStates[selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("calendar_screen")
    ) {
        // Header with title and action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.sm.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(80.dp))

            Text(
                text = "Calendar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showMonthPicker = true },
                    color = AmakaColors.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarViewWeek,
                            contentDescription = "Open calendar",
                            tint = AmakaColors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateToWorkouts() },
                    color = AmakaColors.accentBlue,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add workout",
                            tint = AmakaColors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

        // Month navigation with arrows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmakaSpacing.md.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateWeek(false) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous week",
                    tint = AmakaColors.textPrimary
                )
            }

            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )

            IconButton(onClick = { viewModel.navigateWeek(true) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next week",
                    tint = AmakaColors.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

        // Week view - Sunday to Saturday with DayState indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmakaSpacing.md.dp)
                .testTag("calendar_view"),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in 0..6) {
                val date = weekStart.plusDays(i.toLong())
                val isSelected = date == selectedDate
                val isToday = date == today
                val dayStatus = viewModel.getDayStatus(date)
                val workoutCount = viewModel.getDayWorkoutCount(date)

                WeekDayCell(
                    dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    dayNumber = date.dayOfMonth,
                    isSelected = isSelected,
                    isToday = isToday,
                    dayStatus = dayStatus,
                    hasWorkouts = workoutCount > 0,
                    onClick = { viewModel.selectDate(date) }
                )
            }
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))

        // Generate My Week button
        Button(
            onClick = { viewModel.generateWeekPlan() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmakaSpacing.md.dp)
                .testTag("generate_week_button"),
            enabled = !state.isGeneratingPlan,
            colors = ButtonDefaults.buttonColors(
                containerColor = AmakaColors.accentBlue,
                contentColor = AmakaColors.textPrimary
            ),
            shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
        ) {
            if (state.isGeneratingPlan) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = AmakaColors.textPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
            } else {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
            }
            Text(
                text = if (state.isGeneratingPlan) "Generating..." else "Generate my week",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))

        // Conflict indicators
        if (state.conflicts.isNotEmpty()) {
            ConflictSection(conflicts = state.conflicts)
            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
        }

        // Day's workouts section
        Text(
            text = if (selectedDate == today) "Today's Workouts" else "Workouts",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = AmakaColors.textPrimary,
            modifier = Modifier.padding(horizontal = AmakaSpacing.md.dp)
        )

        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

        val dayWorkouts = currentDayState?.workouts ?: emptyList()

        if (dayWorkouts.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AmakaSpacing.md.dp),
                color = AmakaColors.surface,
                shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
            ) {
                Text(
                    text = "No scheduled workouts",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AmakaColors.textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AmakaSpacing.lg.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = AmakaSpacing.md.dp),
                verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
            ) {
                items(dayWorkouts) { workout ->
                    DayWorkoutCard(workout = workout)
                }
            }
        }

        // Error message
        state.error?.let { error ->
            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = AmakaColors.accentRed,
                modifier = Modifier.padding(horizontal = AmakaSpacing.md.dp)
            )
        }
    }

    // Month picker dialog
    if (showMonthPicker) {
        MonthPickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                viewModel.selectDate(date)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }
}

@Composable
private fun WeekDayCell(
    dayName: String,
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    dayStatus: DayStatus,
    hasWorkouts: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(AmakaCornerRadius.md.dp))
            .clickable(onClick = onClick)
            .padding(AmakaSpacing.xs.dp)
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) AmakaColors.textPrimary else AmakaColors.textSecondary
        )
        Spacer(modifier = Modifier.height(AmakaSpacing.xs.dp))
        Surface(
            modifier = Modifier.size(40.dp),
            color = when {
                isSelected -> AmakaColors.accentBlue
                else -> AmakaColors.background
            },
            shape = CircleShape
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = dayNumber.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isSelected -> AmakaColors.textPrimary
                        isToday -> AmakaColors.accentBlue
                        else -> AmakaColors.textPrimary
                    }
                )
            }
        }
        // Day status indicator dot
        if (hasWorkouts) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        color = dayStatusColor(dayStatus),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun DayWorkoutCard(workout: DayWorkoutSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.md.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sport color indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(
                        color = AmakaColors.accentBlue,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = AmakaColors.textPrimary
                )
                Text(
                    text = "${workout.duration / 60} min - ${workout.sport.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textSecondary
                )
            }
            if (workout.completed) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmakaColors.accentGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ConflictSection(conflicts: List<ScheduleConflict>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AmakaSpacing.md.dp)
    ) {
        conflicts.forEach { conflict ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .testTag("conflict_indicator"),
                color = when (conflict.severity) {
                    ConflictSeverity.CRITICAL -> AmakaColors.accentRed.copy(alpha = 0.15f)
                    ConflictSeverity.WARNING -> AmakaColors.accentOrange.copy(alpha = 0.15f)
                    ConflictSeverity.INFO -> AmakaColors.accentBlue.copy(alpha = 0.15f)
                },
                shape = RoundedCornerShape(AmakaCornerRadius.sm.dp)
            ) {
                Row(
                    modifier = Modifier.padding(AmakaSpacing.sm.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = when (conflict.severity) {
                            ConflictSeverity.CRITICAL -> AmakaColors.accentRed
                            ConflictSeverity.WARNING -> AmakaColors.accentOrange
                            ConflictSeverity.INFO -> AmakaColors.accentBlue
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                    Text(
                        text = conflict.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textPrimary
                    )
                }
            }
        }
    }
}

private fun dayStatusColor(status: DayStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        DayStatus.REST -> AmakaColors.textTertiary
        DayStatus.EASY -> AmakaColors.accentGreen
        DayStatus.MODERATE -> AmakaColors.accentBlue
        DayStatus.HARD -> AmakaColors.accentOrange
        DayStatus.RACE -> AmakaColors.accentRed
    }
}

@Composable
private fun MonthPickerDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var currentSelectedDate by remember { mutableStateOf(selectedDate) }
    val today = LocalDate.now()
    val currentYearMonth = YearMonth.from(selectedDate)

    val months = remember {
        (-3..12).map { currentYearMonth.plusMonths(it.toLong()) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            color = AmakaColors.background,
            shape = RoundedCornerShape(AmakaCornerRadius.lg.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.sm.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Cancel", color = AmakaColors.textPrimary)
                    }
                    Text(
                        text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AmakaColors.textPrimary
                    )
                    TextButton(onClick = { onDateSelected(today) }) {
                        Text(text = "Today", color = AmakaColors.accentBlue)
                    }
                }

                HorizontalDivider(color = AmakaColors.borderLight)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = rememberLazyListState(initialFirstVisibleItemIndex = 3)
                ) {
                    months.forEach { yearMonth ->
                        item {
                            MonthCalendar(
                                yearMonth = yearMonth,
                                selectedDate = currentSelectedDate,
                                today = today,
                                onDateSelected = { date ->
                                    currentSelectedDate = date
                                    onDateSelected(date)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AmakaSpacing.md.dp)
    ) {
        Text(
            text = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
            style = MaterialTheme.typography.titleSmall,
            color = AmakaColors.textSecondary,
            modifier = Modifier.padding(vertical = AmakaSpacing.md.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
            ).forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = AmakaColors.textTertiary,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

        val firstDayOfMonth = yearMonth.atDay(1)
        val startDayOffset = (firstDayOfMonth.dayOfWeek.value - 1)
        val daysInMonth = yearMonth.lengthOfMonth()
        val totalCells = startDayOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayOfMonth = cellIndex - startDayOffset + 1

                    if (dayOfMonth in 1..daysInMonth) {
                        val date = yearMonth.atDay(dayOfMonth)
                        val isSelected = date == selectedDate
                        val isToday = date == today

                        DayCell(
                            day = dayOfMonth,
                            isSelected = isSelected,
                            isToday = isToday,
                            onClick = { onDateSelected(date) }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(40.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(AmakaSpacing.xs.dp))
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
    }
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> AmakaColors.accentBlue
                    else -> AmakaColors.background
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isSelected -> AmakaColors.textPrimary
                isToday -> AmakaColors.accentBlue
                else -> AmakaColors.textPrimary
            },
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
