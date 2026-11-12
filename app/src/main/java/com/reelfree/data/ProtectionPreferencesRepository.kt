package com.reelfree.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.reelFreeDataStore: DataStore<Preferences> by preferencesDataStore(name = "reelfree_settings")

data class ProtectionSettings(
    /** Master switch. When off the service stays connected but never acts. */
    val protectionEnabled: Boolean = true,
    val blockInstagramReels: Boolean = true,
    /** Detect and log, but never press Back. Use this to calibrate rules safely. */
    val dryRun: Boolean = false,
    /** Record rule matches and structural screen signatures (no private text) in memory. */
    val debugMode: Boolean = false,
) {
    val reelsBlockingActive: Boolean get() = protectionEnabled && blockInstagramReels
}

class ProtectionPreferencesRepository(context: Context) {

    private val dataStore = context.applicationContext.reelFreeDataStore

    val settings: Flow<ProtectionSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            ProtectionSettings(
                protectionEnabled = prefs[PROTECTION_ENABLED] ?: true,
                blockInstagramReels = prefs[BLOCK_REELS] ?: true,
                dryRun = prefs[DRY_RUN] ?: false,
                debugMode = prefs[DEBUG_MODE] ?: false,
            )
        }
        .distinctUntilChanged()

    suspend fun setProtectionEnabled(value: Boolean) = put(PROTECTION_ENABLED, value)
    suspend fun setBlockInstagramReels(value: Boolean) = put(BLOCK_REELS, value)
    suspend fun setDryRun(value: Boolean) = put(DRY_RUN, value)
    suspend fun setDebugMode(value: Boolean) = put(DEBUG_MODE, value)

    private suspend fun put(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }

    private companion object {
        val PROTECTION_ENABLED = booleanPreferencesKey("protection_enabled")
        val BLOCK_REELS = booleanPreferencesKey("block_instagram_reels")
        val DRY_RUN = booleanPreferencesKey("dry_run")
        val DEBUG_MODE = booleanPreferencesKey("debug_mode")
    }
}
