package com.example.telepse.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telepse.data.community.communityDisplayUpdatedApplies
import com.example.telepse.data.model.CommunityRoomDisplaySettings
import com.example.telepse.data.network.NetworkMonitor
import com.example.telepse.data.realtime.PusherService
import com.example.telepse.data.auth.SessionExpiryHandler
import com.example.telepse.data.network.AuthenticatedApiUserMessages
import com.example.telepse.data.repository.CommunityRoomDisplayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommunityRoomDisplayUiState(
    val communityId: String? = null,
    val settings: CommunityRoomDisplaySettings? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastRefreshAtMs: Long? = null,
)

@HiltViewModel
class CommunityRoomDisplayViewModel @Inject constructor(
    private val repository: CommunityRoomDisplayRepository,
    private val sessionExpiryHandler: SessionExpiryHandler,
    private val pusherService: PusherService,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityRoomDisplayUiState())
    val uiState: StateFlow<CommunityRoomDisplayUiState> = _uiState.asStateFlow()

    private var activeCommunityId: String? = null
    private var realtimeJobs = mutableListOf<Job>()
    private var lastRealtimeChannel: String? = null

    fun loadCommunity(communityId: String) {
        val trimmed = communityId.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = CommunityRoomDisplayUiState(error = "Enter a community ID")
            return
        }
        viewModelScope.launch {
            if (activeCommunityId != null && activeCommunityId != trimmed) {
                teardownRealtime()
            }
            activeCommunityId = trimmed
            _uiState.value =
                _uiState.value.copy(
                    communityId = trimmed,
                    isLoading = _uiState.value.settings == null,
                    error = null,
                )
            refreshFromApi(trimmed, reason = "initial")
            ensureRealtime(trimmed)
        }
    }

    private suspend fun refreshFromApi(communityId: String, reason: String) {
        if (!networkMonitor.isCurrentlyOnline()) {
            _uiState.value =
                _uiState.value.copy(
                    isLoading = false,
                    error = "No internet connection",
                )
            return
        }
        repository.fetchBootstrap(communityId).fold(
            onSuccess = { bootstrap ->
                if (bootstrap.communityId.trim() != communityId.trim()) {
                    Log.w(
                        TAG,
                        "Bootstrap communityId mismatch expected=$communityId got=${bootstrap.communityId}",
                    )
                }
                _uiState.value =
                    CommunityRoomDisplayUiState(
                        communityId = communityId,
                        settings = bootstrap.display,
                        isLoading = false,
                        error = null,
                        lastRefreshAtMs = System.currentTimeMillis(),
                    )
                pusherService.configureFromCommunityRealtime(bootstrap.realtime)
                lastRealtimeChannel = bootstrap.realtime.channel
                Log.d(TAG, "Loaded community display ($reason) channel=${bootstrap.realtime.channel}")
            },
            onFailure = { e ->
                Log.e(TAG, "Failed to load community display ($reason)", e)
                sessionExpiryHandler.invalidateSessionIfNeeded(e)
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = AuthenticatedApiUserMessages.messageFor(e),
                    )
            },
        )
    }

    private suspend fun ensureRealtime(communityId: String) {
        teardownRealtime()
        if (!pusherService.initialize(forceReconnect = false)) {
            Log.e(TAG, "Pusher initialize failed for community display")
            return
        }
        val channel = lastRealtimeChannel ?: "community-$communityId"
        if (!pusherService.subscribeToCommunityDisplayChannel(channel)) {
            Log.e(TAG, "Failed to subscribe to $channel")
            return
        }

        val displayJob =
            viewModelScope.launch {
                pusherService.observeCommunityDisplayUpdated()
                    .catch { e ->
                        if (e !is kotlinx.coroutines.CancellationException) {
                            Log.e(TAG, "display-updated flow error", e)
                        }
                    }
                    .collect { payload ->
                        val active = activeCommunityId ?: return@collect
                        if (!communityDisplayUpdatedApplies(payload, active)) {
                            Log.d(TAG, "Ignored display-updated for other community")
                            return@collect
                        }
                        Log.d(TAG, "display-updated received — refetching persisted settings")
                        launch { refreshFromApi(active, reason = "display-updated") }
                    }
            }
        realtimeJobs.add(displayJob)

        val reconnectJob =
            viewModelScope.launch {
                pusherService.observeConnectionEstablished().collect {
                    val active = activeCommunityId ?: return@collect
                    val ch = lastRealtimeChannel ?: "community-$active"
                    Log.d(TAG, "Pusher reconnected — resubscribing community channel $ch")
                    pusherService.subscribeToCommunityDisplayChannel(ch)
                }
            }
        realtimeJobs.add(reconnectJob)
    }

    private suspend fun teardownRealtime() {
        realtimeJobs.forEach { it.cancel() }
        realtimeJobs.clear()
        try {
            pusherService.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Error disconnecting pusher for community room", e)
        }
        pusherService.resetDisconnectingFlag()
    }

    fun onLeaveCommunityRoom() {
        viewModelScope.launch {
            teardownRealtime()
            activeCommunityId = null
            lastRealtimeChannel = null
            _uiState.value = CommunityRoomDisplayUiState()
        }
    }

    override fun onCleared() {
        viewModelScope.launch { teardownRealtime() }
        super.onCleared()
    }

    companion object {
        private const val TAG = "CommunityRoomDisplayVM"
    }
}
