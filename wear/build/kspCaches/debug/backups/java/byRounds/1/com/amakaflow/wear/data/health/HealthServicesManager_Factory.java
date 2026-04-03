package com.amakaflow.wear.data.health;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class HealthServicesManager_Factory implements Factory<HealthServicesManager> {
  private final Provider<Context> contextProvider;

  public HealthServicesManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public HealthServicesManager get() {
    return newInstance(contextProvider.get());
  }

  public static HealthServicesManager_Factory create(Provider<Context> contextProvider) {
    return new HealthServicesManager_Factory(contextProvider);
  }

  public static HealthServicesManager newInstance(Context context) {
    return new HealthServicesManager(context);
  }
}
