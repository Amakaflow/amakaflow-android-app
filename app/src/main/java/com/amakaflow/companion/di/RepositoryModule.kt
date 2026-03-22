package com.amakaflow.companion.di

import android.util.Log
import com.amakaflow.companion.data.TestConfig
import com.amakaflow.companion.data.repository.CompletionRepositoryImpl
import com.amakaflow.companion.data.repository.FixtureWorkoutRepository
import com.amakaflow.companion.data.repository.PairingRepositoryImpl
import com.amakaflow.companion.data.repository.PlannerRepositoryImpl
import com.amakaflow.companion.data.repository.WorkoutRepositoryImpl
import com.amakaflow.companion.data.sync.SyncCoordinatorImpl
import com.amakaflow.companion.domain.repository.CompletionRepository
import com.amakaflow.companion.domain.repository.PairingRepository
import com.amakaflow.companion.domain.repository.PlannerRepository
import com.amakaflow.companion.domain.repository.WorkoutRepository
import com.amakaflow.companion.domain.sync.SyncCoordinator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds repository and coordinator interfaces to their implementations.
 * This enables dependency injection of domain interfaces throughout the app.
 *
 * When E2E test mode is active with UITEST_USE_FIXTURES=true, the WorkoutRepository
 * is swapped for FixtureWorkoutRepository which returns hardcoded test data.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPairingRepository(
        impl: PairingRepositoryImpl
    ): PairingRepository

    @Binds
    @Singleton
    abstract fun bindCompletionRepository(
        impl: CompletionRepositoryImpl
    ): CompletionRepository

    @Binds
    @Singleton
    abstract fun bindSyncCoordinator(
        impl: SyncCoordinatorImpl
    ): SyncCoordinator

    @Binds
    @Singleton
    abstract fun bindPlannerRepository(
        impl: PlannerRepositoryImpl
    ): PlannerRepository

    companion object {
        @Provides
        @Singleton
        fun provideWorkoutRepository(
            realImpl: WorkoutRepositoryImpl,
            testConfig: TestConfig
        ): WorkoutRepository {
            return if (testConfig.isTestModeEnabled && testConfig.useFixtures) {
                Log.d("RepositoryModule", "Using FixtureWorkoutRepository with fixtures: ${testConfig.fixtureNames}")
                FixtureWorkoutRepository(testConfig.fixtureNames)
            } else {
                realImpl
            }
        }
    }
}
