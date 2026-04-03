package com.amakaflow.wear.data.repository;

import com.amakaflow.wear.data.connectivity.PhoneConnectivityManager;
import com.amakaflow.wear.data.health.HealthServicesManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class WearWorkoutRepository_Factory implements Factory<WearWorkoutRepository> {
  private final Provider<PhoneConnectivityManager> phoneConnectivityManagerProvider;

  private final Provider<HealthServicesManager> healthServicesManagerProvider;

  public WearWorkoutRepository_Factory(
      Provider<PhoneConnectivityManager> phoneConnectivityManagerProvider,
      Provider<HealthServicesManager> healthServicesManagerProvider) {
    this.phoneConnectivityManagerProvider = phoneConnectivityManagerProvider;
    this.healthServicesManagerProvider = healthServicesManagerProvider;
  }

  @Override
  public WearWorkoutRepository get() {
    return newInstance(phoneConnectivityManagerProvider.get(), healthServicesManagerProvider.get());
  }

  public static WearWorkoutRepository_Factory create(
      Provider<PhoneConnectivityManager> phoneConnectivityManagerProvider,
      Provider<HealthServicesManager> healthServicesManagerProvider) {
    return new WearWorkoutRepository_Factory(phoneConnectivityManagerProvider, healthServicesManagerProvider);
  }

  public static WearWorkoutRepository newInstance(PhoneConnectivityManager phoneConnectivityManager,
      HealthServicesManager healthServicesManager) {
    return new WearWorkoutRepository(phoneConnectivityManager, healthServicesManager);
  }
}
