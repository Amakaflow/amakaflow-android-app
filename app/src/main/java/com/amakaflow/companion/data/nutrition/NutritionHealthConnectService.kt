package com.amakaflow.companion.data.nutrition

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Volume
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AMA-1290: Health Connect nutrition sync service.
 * Reads daily nutrition (calories, protein, carbs, fat) and hydration from Health Connect.
 * Writes protein/water entries for manual tracking (AMA-1291).
 */
@Singleton
class NutritionHealthConnectService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val healthConnectClient: HealthConnectClient? by lazy {
        try {
            if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else {
                Log.w(TAG, "Health Connect SDK not available")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Health Connect client", e)
            null
        }
    }

    /**
     * Required permissions for nutrition features.
     */
    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getWritePermission(NutritionRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getWritePermission(HydrationRecord::class)
    )

    /**
     * Check if Health Connect is available on this device.
     */
    fun isAvailable(): Boolean {
        return try {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Health Connect availability", e)
            false
        }
    }

    /**
     * Check if all required permissions are granted.
     */
    suspend fun hasPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            requiredPermissions.all { it in granted }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions", e)
            false
        }
    }

    /**
     * Read daily nutrition summary from Health Connect using aggregate queries.
     */
    suspend fun getDailyNutrition(date: LocalDate = LocalDate.now()): DailyNutritionSummary {
        val client = healthConnectClient ?: return DailyNutritionSummary()

        return try {
            val zone = ZoneId.systemDefault()
            val startTime = date.atStartOfDay(zone).toInstant()
            val endTime = date.plusDays(1).atStartOfDay(zone).toInstant()

            val timeRange = TimeRangeFilter.between(startTime, endTime)

            val nutritionResponse = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        NutritionRecord.ENERGY_TOTAL,
                        NutritionRecord.PROTEIN_TOTAL,
                        NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL,
                        NutritionRecord.TOTAL_FAT_TOTAL
                    ),
                    timeRangeFilter = timeRange
                )
            )

            val hydrationResponse = client.aggregate(
                AggregateRequest(
                    metrics = setOf(HydrationRecord.VOLUME_TOTAL),
                    timeRangeFilter = timeRange
                )
            )

            DailyNutritionSummary(
                calories = nutritionResponse[NutritionRecord.ENERGY_TOTAL]
                    ?.inKilocalories ?: 0.0,
                proteinGrams = nutritionResponse[NutritionRecord.PROTEIN_TOTAL]
                    ?.inGrams ?: 0.0,
                carbsGrams = nutritionResponse[NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL]
                    ?.inGrams ?: 0.0,
                fatGrams = nutritionResponse[NutritionRecord.TOTAL_FAT_TOTAL]
                    ?.inGrams ?: 0.0,
                waterMl = hydrationResponse[HydrationRecord.VOLUME_TOTAL]
                    ?.inMilliliters ?: 0.0,
                source = "Health Connect"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading daily nutrition", e)
            DailyNutritionSummary()
        }
    }

    /**
     * AMA-1291: Write a protein entry to Health Connect.
     */
    suspend fun addProtein(grams: Double): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val now = java.time.Instant.now()
            val record = NutritionRecord(
                startTime = now,
                endTime = now.plusSeconds(1),
                startZoneOffset = java.time.ZoneOffset.systemDefault().rules.getOffset(now),
                endZoneOffset = java.time.ZoneOffset.systemDefault().rules.getOffset(now),
                protein = Mass.grams(grams)
            )
            client.insertRecords(listOf(record))
            Log.d(TAG, "Added ${grams}g protein to Health Connect")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error writing protein record", e)
            false
        }
    }

    /**
     * AMA-1291: Write a hydration entry to Health Connect.
     */
    suspend fun addWater(ml: Double): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val now = java.time.Instant.now()
            val record = HydrationRecord(
                startTime = now,
                endTime = now.plusSeconds(1),
                startZoneOffset = java.time.ZoneOffset.systemDefault().rules.getOffset(now),
                endZoneOffset = java.time.ZoneOffset.systemDefault().rules.getOffset(now),
                volume = Volume.milliliters(ml)
            )
            client.insertRecords(listOf(record))
            Log.d(TAG, "Added ${ml}ml water to Health Connect")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error writing hydration record", e)
            false
        }
    }

    /**
     * AMA-1292: Delete all AmakaFlow nutrition records from Health Connect.
     */
    suspend fun deleteAllNutritionData(): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            client.deleteRecords(
                NutritionRecord::class,
                TimeRangeFilter.between(
                    java.time.Instant.EPOCH,
                    java.time.Instant.now()
                )
            )
            client.deleteRecords(
                HydrationRecord::class,
                TimeRangeFilter.between(
                    java.time.Instant.EPOCH,
                    java.time.Instant.now()
                )
            )
            Log.d(TAG, "Deleted all nutrition data from Health Connect")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting nutrition data", e)
            false
        }
    }

    companion object {
        private const val TAG = "NutritionHC"
    }
}
