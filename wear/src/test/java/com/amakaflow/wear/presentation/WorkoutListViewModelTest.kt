package com.amakaflow.wear.presentation

import com.amakaflow.shared.model.WearWorkout
import com.amakaflow.wear.data.connectivity.PhoneConnectivityManager
import com.amakaflow.wear.data.health.HealthServicesManager
import com.amakaflow.wear.data.repository.WearWorkoutRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutListViewModelTest {

    // UnconfinedTestDispatcher propagates stateIn values immediately
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var phoneConnectivityManager: PhoneConnectivityManager
    private lateinit var healthServicesManager: HealthServicesManager
    private lateinit var repository: WearWorkoutRepository
    private lateinit var viewModel: WorkoutListViewModel

    private val workoutsFlow = MutableStateFlow<List<WearWorkout>>(emptyList())
    private val isPhoneConnectedFlow = MutableStateFlow(false)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        phoneConnectivityManager = mockk(relaxed = true) {
            every { workouts } returns workoutsFlow
            every { isPhoneConnected } returns isPhoneConnectedFlow
            every { schedule } returns MutableStateFlow(null)
            every { readiness } returns MutableStateFlow(null)
        }
        healthServicesManager = mockk(relaxed = true) {
            every { currentHeartRate } returns MutableStateFlow(null)
        }
        repository = WearWorkoutRepository(phoneConnectivityManager, healthServicesManager)
        coEvery { phoneConnectivityManager.checkPhoneConnection() } returns true

        viewModel = WorkoutListViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty workouts`() = runTest {
        assertThat(viewModel.workouts.value).isEmpty()
    }

    @Test
    fun `workouts flow emits when phone syncs data`() = runTest {
        val testWorkouts = listOf(
            WearWorkout(
                id = "w1",
                name = "Morning HIIT",
                sport = "cardio",
                duration = 1800,
                description = "High intensity interval training"
            ),
            WearWorkout(
                id = "w2",
                name = "Upper Body Strength",
                sport = "strength",
                duration = 2700
            )
        )

        // Need to collect to activate WhileSubscribed
        val collected = mutableListOf<List<WearWorkout>>()
        val job = launch(testDispatcher) {
            viewModel.workouts.collect { collected.add(it) }
        }

        workoutsFlow.value = testWorkouts

        assertThat(collected.last()).hasSize(2)
        assertThat(collected.last()[0].name).isEqualTo("Morning HIIT")
        assertThat(collected.last()[1].name).isEqualTo("Upper Body Strength")

        job.cancel()
    }

    @Test
    fun `isPhoneConnected reflects connection state`() = runTest {
        val collected = mutableListOf<Boolean>()
        val job = launch(testDispatcher) {
            viewModel.isPhoneConnected.collect { collected.add(it) }
        }

        assertThat(collected.last()).isFalse()

        isPhoneConnectedFlow.value = true

        assertThat(collected.last()).isTrue()

        job.cancel()
    }

    @Test
    fun `requestSync calls repository`() = runTest {
        coEvery { phoneConnectivityManager.requestSync() } returns true

        viewModel.requestSync()

        coVerify { phoneConnectivityManager.requestSync() }
    }
}
