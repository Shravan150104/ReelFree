package com.reelfree

import android.app.Application
import com.reelfree.data.ProtectionPreferencesRepository

class ReelFreeApp : Application() {
    lateinit var preferences: ProtectionPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        preferences = ProtectionPreferencesRepository(this)
    }
}
