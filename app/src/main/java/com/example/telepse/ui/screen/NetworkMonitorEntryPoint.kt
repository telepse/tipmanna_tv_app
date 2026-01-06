package com.example.telepse.ui.screen

import com.example.telepse.data.network.NetworkMonitor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@EntryPoint
@InstallIn(ActivityComponent::class)
interface NetworkMonitorEntryPoint {
    fun networkMonitor(): NetworkMonitor
}

