import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.time.Duration

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.sentry) apply false
}


subprojects {
    tasks.withType<Test>().configureEach {
        // AMA-1774: fail loudly instead of hanging GitHub runners indefinitely.
        timeout.set(Duration.ofMinutes(15))
        maxParallelForks = 1
        maxHeapSize = "1536m"

        testLogging {
            events(
                TestLogEvent.FAILED,
                TestLogEvent.SKIPPED,
                TestLogEvent.STANDARD_ERROR
            )
            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
    }
}
