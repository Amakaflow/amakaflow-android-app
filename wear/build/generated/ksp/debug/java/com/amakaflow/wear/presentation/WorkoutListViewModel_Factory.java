package com.amakaflow.wear.presentation;

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
public final class WorkoutListViewModel_Factory implements Factory<WorkoutListViewModel> {
  private final Provider<WearWorkoutRepository> repositoryProvider;

  public WorkoutListViewModel_Factory(Provider<WearWorkoutRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public WorkoutListViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static WorkoutListViewModel_Factory create(
      Provider<WearWorkoutRepository> repositoryProvider) {
    return new WorkoutListViewModel_Factory(repositoryProvider);
  }

  public static WorkoutListViewModel newInstance(WearWorkoutRepository repository) {
    return new WorkoutListViewModel(repository);
  }
}
