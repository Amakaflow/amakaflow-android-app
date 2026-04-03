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
public final class ReadinessViewModel_Factory implements Factory<ReadinessViewModel> {
  private final Provider<WearWorkoutRepository> repositoryProvider;

  public ReadinessViewModel_Factory(Provider<WearWorkoutRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public ReadinessViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static ReadinessViewModel_Factory create(
      Provider<WearWorkoutRepository> repositoryProvider) {
    return new ReadinessViewModel_Factory(repositoryProvider);
  }

  public static ReadinessViewModel newInstance(WearWorkoutRepository repository) {
    return new ReadinessViewModel(repository);
  }
}
