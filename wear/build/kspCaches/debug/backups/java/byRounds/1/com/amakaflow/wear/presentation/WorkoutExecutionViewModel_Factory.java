package com.amakaflow.wear.presentation;

import android.os.Vibrator;
import androidx.lifecycle.SavedStateHandle;
import com.amakaflow.wear.data.repository.WearWorkoutRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class WorkoutExecutionViewModel_Factory implements Factory<WorkoutExecutionViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<WearWorkoutRepository> repositoryProvider;

  private final Provider<Vibrator> vibratorProvider;

  public WorkoutExecutionViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<WearWorkoutRepository> repositoryProvider, Provider<Vibrator> vibratorProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.repositoryProvider = repositoryProvider;
    this.vibratorProvider = vibratorProvider;
  }

  @Override
  public WorkoutExecutionViewModel get() {
    return newInstance(savedStateHandleProvider.get(), repositoryProvider.get(), vibratorProvider.get());
  }

  public static WorkoutExecutionViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<WearWorkoutRepository> repositoryProvider, Provider<Vibrator> vibratorProvider) {
    return new WorkoutExecutionViewModel_Factory(savedStateHandleProvider, repositoryProvider, vibratorProvider);
  }

  public static WorkoutExecutionViewModel newInstance(SavedStateHandle savedStateHandle,
      WearWorkoutRepository repository, Vibrator vibrator) {
    return new WorkoutExecutionViewModel(savedStateHandle, repository, vibrator);
  }
}
