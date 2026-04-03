package com.amakaflow.wear.di;

import android.content.Context;
import android.os.Vibrator;
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
public final class WearModule_ProvideVibratorFactory implements Factory<Vibrator> {
  private final Provider<Context> contextProvider;

  public WearModule_ProvideVibratorFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public Vibrator get() {
    return provideVibrator(contextProvider.get());
  }

  public static WearModule_ProvideVibratorFactory create(Provider<Context> contextProvider) {
    return new WearModule_ProvideVibratorFactory(contextProvider);
  }

  public static Vibrator provideVibrator(Context context) {
    return WearModule.INSTANCE.provideVibrator(context);
  }
}
