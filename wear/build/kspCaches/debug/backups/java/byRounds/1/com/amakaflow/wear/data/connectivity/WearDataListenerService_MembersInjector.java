package com.amakaflow.wear.data.connectivity;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class WearDataListenerService_MembersInjector implements MembersInjector<WearDataListenerService> {
  private final Provider<PhoneConnectivityManager> phoneConnectivityManagerProvider;

  public WearDataListenerService_MembersInjector(
      Provider<PhoneConnectivityManager> phoneConnectivityManagerProvider) {
    this.phoneConnectivityManagerProvider = phoneConnectivityManagerProvider;
  }

  public static MembersInjector<WearDataListenerService> create(
      Provider<PhoneConnectivityManager> phoneConnectivityManagerProvider) {
    return new WearDataListenerService_MembersInjector(phoneConnectivityManagerProvider);
  }

  @Override
  public void injectMembers(WearDataListenerService instance) {
    injectPhoneConnectivityManager(instance, phoneConnectivityManagerProvider.get());
  }

  @InjectedFieldSignature("com.amakaflow.wear.data.connectivity.WearDataListenerService.phoneConnectivityManager")
  public static void injectPhoneConnectivityManager(WearDataListenerService instance,
      PhoneConnectivityManager phoneConnectivityManager) {
    instance.phoneConnectivityManager = phoneConnectivityManager;
  }
}
