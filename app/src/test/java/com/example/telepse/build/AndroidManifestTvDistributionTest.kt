package com.example.telepse.build

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates source manifest TV-only distribution (merged manifests share the same main manifest).
 */
class AndroidManifestTvDistributionTest {
    @Test
    fun manifest_requiresLeanback_and_usesLeanbackLauncherOnly() {
        val manifest =
            File("src/main/AndroidManifest.xml").takeIf { it.isFile }?.readText()
                ?: error("AndroidManifest.xml not found from module working dir")
        assertTrue(
            manifest.contains("android:name=\"android.software.leanback\"") &&
                manifest.contains("android:required=\"true\""),
        )
        assertTrue(manifest.contains("android.intent.category.LEANBACK_LAUNCHER"))
        assertTrue(
            !manifest.contains("android.intent.category.LAUNCHER\" />"),
        )
    }
}
