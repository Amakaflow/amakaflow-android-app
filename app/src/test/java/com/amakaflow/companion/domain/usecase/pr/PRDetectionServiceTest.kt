package com.amakaflow.companion.domain.usecase.pr

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class PRDetectionServiceTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var service: PRDetectionService

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)

        every { mockContext.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.getString("prs", null) } returns null
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor

        service = PRDetectionService(mockContext)
    }

    @Test
    fun `detectPRs returns empty when no sets provided`() {
        val result = service.detectPRs(emptyList(), "Test Workout")
        assertThat(result.hasPRs).isFalse()
        assertThat(result.newPRs).isEmpty()
    }

    @Test
    fun `detectPRs detects heaviest weight PR`() {
        val sets = listOf(
            ExerciseSetData("Bench Press", 1, 5, 80.0),
            ExerciseSetData("Bench Press", 2, 5, 85.0),
            ExerciseSetData("Bench Press", 3, 3, 90.0)
        )

        val result = service.detectPRs(sets, "Test Workout")

        assertThat(result.hasPRs).isTrue()
        val weightPR = result.newPRs.find { it.type == PRType.HEAVIEST_WEIGHT }
        assertThat(weightPR).isNotNull()
        assertThat(weightPR!!.newValue).isEqualTo(90.0)
        assertThat(weightPR.oldValue).isNull()
        assertThat(weightPR.exerciseName).isEqualTo("Bench Press")
    }

    @Test
    fun `detectPRs detects most reps PR at heaviest weight`() {
        val sets = listOf(
            ExerciseSetData("Squat", 1, 8, 100.0),
            ExerciseSetData("Squat", 2, 10, 100.0),
            ExerciseSetData("Squat", 3, 6, 100.0)
        )

        val result = service.detectPRs(sets, "Leg Day")

        val repsPR = result.newPRs.find { it.type == PRType.MOST_REPS }
        assertThat(repsPR).isNotNull()
        assertThat(repsPR!!.newValue).isEqualTo(10.0)
        assertThat(repsPR.weight).isEqualTo(100.0)
    }

    @Test
    fun `detectPRs detects most volume PR`() {
        val sets = listOf(
            ExerciseSetData("Deadlift", 1, 5, 120.0),  // 600
            ExerciseSetData("Deadlift", 2, 5, 120.0),  // 600
            ExerciseSetData("Deadlift", 3, 3, 130.0)   // 390
        )

        val result = service.detectPRs(sets, "Pull Day")

        val volumePR = result.newPRs.find { it.type == PRType.MOST_VOLUME }
        assertThat(volumePR).isNotNull()
        assertThat(volumePR!!.newValue).isEqualTo(1590.0)
    }

    @Test
    fun `detectPRs handles multiple exercises`() {
        val sets = listOf(
            ExerciseSetData("Bench Press", 1, 5, 80.0),
            ExerciseSetData("Squat", 1, 5, 100.0),
            ExerciseSetData("Deadlift", 1, 3, 140.0)
        )

        val result = service.detectPRs(sets, "Full Body")

        // Should have PRs for all 3 exercises (heaviest weight + reps + volume each = 9 total)
        assertThat(result.newPRs.size).isAtLeast(3)
        val exercises = result.newPRs.map { it.exerciseName }.toSet()
        assertThat(exercises).containsExactly("Bench Press", "Squat", "Deadlift")
    }

    @Test
    fun `detectPRs ignores zero weight sets`() {
        val sets = listOf(
            ExerciseSetData("Push-ups", 1, 20, 0.0),
            ExerciseSetData("Push-ups", 2, 15, 0.0)
        )

        val result = service.detectPRs(sets, "Bodyweight")

        assertThat(result.hasPRs).isFalse()
    }

    @Test
    fun `detectPRs beats existing PR`() {
        // Seed with existing PR
        val existingPRs = """[{"id":"old-1","exerciseName":"Bench Press","type":"HEAVIEST_WEIGHT","value":80.0,"reps":5,"weight":null,"dateIso":"2026-01-01T00:00:00Z","workoutName":"Old Workout"}]"""
        every { mockPrefs.getString("prs", null) } returns existingPRs

        val sets = listOf(
            ExerciseSetData("Bench Press", 1, 3, 85.0)
        )

        val result = service.detectPRs(sets, "New Workout")

        val weightPR = result.newPRs.find { it.type == PRType.HEAVIEST_WEIGHT }
        assertThat(weightPR).isNotNull()
        assertThat(weightPR!!.newValue).isEqualTo(85.0)
        assertThat(weightPR.oldValue).isEqualTo(80.0)
    }

    @Test
    fun `detectPRs does not trigger when below existing PR`() {
        val existingPRs = """[{"id":"old-1","exerciseName":"Bench Press","type":"HEAVIEST_WEIGHT","value":100.0,"reps":5,"weight":null,"dateIso":"2026-01-01T00:00:00Z","workoutName":"Old Workout"}]"""
        every { mockPrefs.getString("prs", null) } returns existingPRs

        val sets = listOf(
            ExerciseSetData("Bench Press", 1, 5, 80.0)
        )

        val result = service.detectPRs(sets, "Lighter Day")

        val weightPR = result.newPRs.find { it.type == PRType.HEAVIEST_WEIGHT }
        assertThat(weightPR).isNull()
    }

    @Test
    fun `detectPRs persists new records`() {
        val sets = listOf(
            ExerciseSetData("Bench Press", 1, 5, 80.0)
        )

        service.detectPRs(sets, "Test Workout")

        verify { mockEditor.putString("prs", any()) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `loadPRs returns empty list when no data`() {
        every { mockPrefs.getString("prs", null) } returns null
        val result = service.loadPRs()
        assertThat(result).isEmpty()
    }

    @Test
    fun `prsByExercise groups correctly`() {
        val data = """[
            {"id":"1","exerciseName":"Bench Press","type":"HEAVIEST_WEIGHT","value":80.0,"dateIso":"2026-01-01T00:00:00Z"},
            {"id":"2","exerciseName":"Bench Press","type":"MOST_VOLUME","value":1200.0,"dateIso":"2026-01-01T00:00:00Z"},
            {"id":"3","exerciseName":"Squat","type":"HEAVIEST_WEIGHT","value":100.0,"dateIso":"2026-01-01T00:00:00Z"}
        ]"""
        every { mockPrefs.getString("prs", null) } returns data

        val grouped = service.prsByExercise()

        assertThat(grouped).hasSize(2)
        assertThat(grouped[0].first).isEqualTo("Bench Press")
        assertThat(grouped[0].second).hasSize(2)
        assertThat(grouped[1].first).isEqualTo("Squat")
        assertThat(grouped[1].second).hasSize(1)
    }
}
