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
public final class TodayScheduleViewModel_Factory implements Factory<TodayScheduleViewModel> {
  private final Provider<WearWorkoutRepository> repositoryProvider;

  public TodayScheduleViewModel_Factory(Provider<WearWorkoutRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public TodayScheduleViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static TodayScheduleViewModel_Factory create(
      Provider<WearWorkoutRepository> repositoryProvider) {
    return new TodayScheduleViewModel_Factory(repositoryProvider);
  }

  public static TodayScheduleViewModel newInstance(WearWorkoutRepository repository) {
    return new TodayScheduleViewModel(repository);
  }
}
