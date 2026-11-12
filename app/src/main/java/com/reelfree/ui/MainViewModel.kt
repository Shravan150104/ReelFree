package com.reelfree.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reelfree.BuildConfig
import com.reelfree.ReelFreeApp
import com.reelfree.data.ProtectionSettings
import com.reelfree.detection.instagram.InstagramReelsRules
import com.reelfree.diagnostics.DiagnosticsLog
import com.reelfree.service.AccessibilityPermission
import com.reelfree.service.ServiceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProtectionStatus {
    /** The user has not enabled the accessibility service. */
    PERMISSION_REQUIRED,

    /** Enabled in Settings, but Android has not (re)bound the service, e.g. after a crash or update. */
    SERVICE_NOT_RUNNING,

    /** Service is running but the user switched blocking off. */
    DISABLED,

    /** Service running and Reels blocking on. */
    ACTIVE,
}

data class HomeUiState(
    val status: ProtectionStatus = ProtectionStatus.PERMISSION_REQUIRED,
    val settings: ProtectionSettings = ProtectionSettings(),
    val permissionGranted: Boolean = false,
    val serviceConnected: Boolean = false,
    val instagramVersion: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as ReelFreeApp).preferences
    private val permissionGranted = MutableStateFlow(AccessibilityPermission.isEnabled(application))
    private val instagramVersion = MutableStateFlow(
        AccessibilityPermission.installedVersion(application, InstagramReelsRules.PACKAGE),
    )

    val uiState: StateFlow<HomeUiState> = combine(
        repository.settings,
        permissionGranted,
        ServiceStatus.connected,
        instagramVersion,
    ) { settings, granted, connected, igVersion ->
        val status = when {
            !granted -> ProtectionStatus.PERMISSION_REQUIRED
            !connected -> ProtectionStatus.SERVICE_NOT_RUNNING
            !settings.reelsBlockingActive -> ProtectionStatus.DISABLED
            else -> ProtectionStatus.ACTIVE
        }
        HomeUiState(status, settings, granted, connected, igVersion)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val diagnosticEntries = DiagnosticsLog.entries
    val diagnosticScreens = DiagnosticsLog.screens

    /** Called on every resume: the user may have just come back from system Settings. */
    fun refresh() {
        val app = getApplication<Application>()
        permissionGranted.value = AccessibilityPermission.isEnabled(app)
        instagramVersion.value = AccessibilityPermission.installedVersion(app, InstagramReelsRules.PACKAGE)
    }

    fun setProtectionEnabled(value: Boolean) = viewModelScope.launch { repository.setProtectionEnabled(value) }
    fun setBlockReels(value: Boolean) = viewModelScope.launch { repository.setBlockInstagramReels(value) }
    fun setDryRun(value: Boolean) = viewModelScope.launch { repository.setDryRun(value) }
    fun setDebugMode(value: Boolean) = viewModelScope.launch {
        repository.setDebugMode(value)
        if (!value) DiagnosticsLog.clear()
    }

    fun clearDiagnostics() = DiagnosticsLog.clear()

    fun diagnosticsReport(): String =
        DiagnosticsLog.exportText(BuildConfig.VERSION_NAME, instagramVersion.value)
}
