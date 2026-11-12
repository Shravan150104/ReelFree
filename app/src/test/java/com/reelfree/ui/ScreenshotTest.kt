package com.reelfree.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.reelfree.data.ProtectionSettings
import com.reelfree.ui.screens.HomeActions
import com.reelfree.ui.screens.HomeScreen
import com.reelfree.ui.screens.SettingsActions
import com.reelfree.ui.screens.SettingsScreen
import com.reelfree.ui.theme.ReelFreeTheme
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders screens to PNG for visual review. Skipped unless run with -Pscreenshots=true:
 *   ./gradlew testDebugUnitTest -Pscreenshots=true --tests "*ScreenshotTest*"
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val outDir = File(System.getProperty("reelfree.screenshotDir") ?: "build/screenshots")

    @Before
    fun gate() {
        assumeTrue(!System.getProperty("reelfree.screenshots").isNullOrEmpty())
        outDir.mkdirs()
    }

    private val homeActions = HomeActions({}, {}, {}, {}, {})
    private val settingsActions = SettingsActions({}, {}, {}, {}, {})

    private fun shoot(name: String, dark: Boolean, content: @Composable () -> Unit) {
        // The status orb pulses forever; drive the clock manually so the test can settle.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            ReelFreeTheme(darkTheme = dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        // captureToImage() relies on PixelCopy, which hangs under Robolectric; draw the view tree instead.
        val view = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private val active = HomeUiState(
        status = ProtectionStatus.ACTIVE,
        settings = ProtectionSettings(),
        permissionGranted = true,
        serviceConnected = true,
        instagramVersion = "455.0.0.51.72",
    )
    private val dryRun = active.copy(settings = ProtectionSettings(dryRun = true, debugMode = true))
    private val paused = active.copy(status = ProtectionStatus.DISABLED, settings = ProtectionSettings(protectionEnabled = false))
    private val needsPermission = HomeUiState(instagramVersion = "455.0.0.51.72")

    private fun home(s: HomeUiState) = @Composable { HomeScreen(s, homeActions, PaddingValues()) }
    private fun settings(s: HomeUiState) = @Composable { SettingsScreen(s, 12, settingsActions, PaddingValues()) }

    @Test fun homeActiveLight() = shoot("home_active_light", false, home(active))
    @Test fun homeActiveDark() = shoot("home_active_dark", true, home(active))
    @Test fun homeDryRunDark() = shoot("home_dryrun_dark", true, home(dryRun))
    @Test fun homePausedLight() = shoot("home_paused_light", false, home(paused))
    @Test fun homeSetupLight() = shoot("home_setup_light", false, home(needsPermission))
    @Test fun homeSetupDark() = shoot("home_setup_dark", true, home(needsPermission))
    @Test fun settingsLight() = shoot("settings_light", false, settings(dryRun))
    @Test fun settingsDark() = shoot("settings_dark", true, settings(dryRun))
}
