package com.amakaflow.companion.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarViewWeek
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
    onNavigateToWorkouts: () -> Unit = {}
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showMonthPicker by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    // Get the week start (Sunday)
    val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

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
            Spacer(modifier = Modifier.width(80.dp)) // Balance for centering

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
                // Calendar grid icon - opens month picker
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

                // Add button - navigates to workouts
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
            IconButton(
                onClick = { selectedDate = selectedDate.minusWeeks(1) }
            ) {
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

            IconButton(
                onClick = { selectedDate = selectedDate.plusWeeks(1) }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next week",
                    tint = AmakaColors.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

        // Week view - Sunday to Saturday
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

                WeekDayCell(
                    dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    dayNumber = date.dayOfMonth,
                    isSelected = isSelected,
                    isToday = isToday,
                    onClick = { selectedDate = date }
                )
            }
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))

        // Upcoming Workouts section
        Text(
            text = "Upcoming Workouts",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = AmakaColors.textPrimary,
            modifier = Modifier.padding(horizontal = AmakaSpacing.md.dp)
        )

        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

        // Empty state
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
    }

    // Month picker dialog
    if (showMonthPicker) {
        MonthPickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                selectedDate = date
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

    // Generate months to display (3 months before + current + 12 months ahead)
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
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with Cancel, Month/Year, and Today
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.sm.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancel",
                            color = AmakaColors.textPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Text(
                        text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AmakaColors.textPrimary
                    )

                    TextButton(onClick = {
                        onDateSelected(today)
                    }) {
                        Text(
                            text = "Today",
                            color = AmakaColors.accentBlue,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                HorizontalDivider(color = AmakaColors.borderLight)

                // Scrollable calendar months
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
        // Month header
        Text(
            text = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
            style = MaterialTheme.typography.titleSmall,
            color = AmakaColors.textSecondary,
            modifier = Modifier.padding(vertical = AmakaSpacing.md.dp)
        )

        // Day of week headers (M T W T F S S)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
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

        // Calendar grid
        val firstDayOfMonth = yearMonth.atDay(1)
        // Calculate offset for Monday-starting week (Monday = 0)
        val startDayOffset = (firstDayOfMonth.dayOfWeek.value - 1) // Monday = 0, Sunday = 6
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
                        // Empty cell
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
