package com.example.telepse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telepse.analytics.TvAnalyticsClient
import com.example.telepse.analytics.TvAnalyticsEventFactory
import com.example.telepse.analytics.TvAnalyticsClipMode
import com.example.telepse.analytics.TvAnalyticsSurfaces
import com.example.telepse.data.model.ExploreFeedItem
import com.example.telepse.data.auth.SessionExpiryHandler
import com.example.telepse.data.network.AuthenticatedApiUserMessages
import com.example.telepse.data.repository.ExploreFeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ExploreUiPhase {
    Idle,
    Loading,
    Content,
    Empty,
    Error,
}

data class ExploreUiState(
    val phase: ExploreUiPhase = ExploreUiPhase.Idle,
    val items: List<ExploreFeedItem> = emptyList(),
    val seed: String? = null,
    val errorMessage: String? = null,
    val loadingMore: Boolean = false,
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: ExploreFeedRepository,
    private val sessionExpiryHandler: SessionExpiryHandler,
    private val analytics: TvAnalyticsClient,
) : ViewModel() {

    init {
        analytics.setContext(surface = TvAnalyticsSurfaces.EXPLORE_FEED)
    }

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var loadInFlight = false

    fun loadInitial() {
        if (loadInFlight) return
        loadInFlight = true
        _uiState.update {
            it.copy(
                phase = ExploreUiPhase.Loading,
                errorMessage = null,
                items = emptyList(),
            )
        }
        viewModelScope.launch {
            repository
                .fetchTrendsBatch(excludeChannelIds = emptyList(), seed = null, count = 10)
                .onSuccess { batch ->
                    _uiState.update {
                        if (batch.items.isEmpty()) {
                            it.copy(
                                phase = ExploreUiPhase.Empty,
                                seed = batch.seed,
                                errorMessage = "No trending videos right now.",
                            )
                        } else {
                            it.copy(
                                phase = ExploreUiPhase.Content,
                                items = batch.items,
                                seed = batch.seed,
                            )
                        }
                    }
                }
                .onFailure { e ->
                    sessionExpiryHandler.invalidateSessionIfNeeded(e)
                    _uiState.update {
                        it.copy(
                            phase = ExploreUiPhase.Error,
                            errorMessage = mapExploreError(e),
                        )
                    }
                }
            loadInFlight = false
        }
    }

    fun loadMore() {
        val snapshot = _uiState.value
        if (snapshot.phase != ExploreUiPhase.Content || snapshot.loadingMore || loadInFlight) return
        loadInFlight = true
        _uiState.update { it.copy(loadingMore = true, errorMessage = null) }
        val exclude = snapshot.items.map { it.id }
        viewModelScope.launch {
            repository
                .fetchTrendsBatch(
                    excludeChannelIds = exclude,
                    seed = snapshot.seed,
                    count = 8,
                )
                .onSuccess { batch ->
                    _uiState.update { current ->
                        val added =
                            batch.items.filter { new ->
                                current.items.none { it.id == new.id }
                            }
                        val merged = current.items + added
                        current.copy(
                            items = merged,
                            seed = batch.seed.ifBlank { current.seed },
                            loadingMore = false,
                            phase = if (merged.isEmpty()) ExploreUiPhase.Empty else ExploreUiPhase.Content,
                            errorMessage =
                                if (added.isEmpty() && batch.items.isNotEmpty()) {
                                    "No new items in this batch — try again later."
                                } else if (added.isEmpty()) {
                                    "End of feed for now."
                                } else {
                                    null
                                },
                        )
                    }
                }
                .onFailure { e ->
                    sessionExpiryHandler.invalidateSessionIfNeeded(e)
                    _uiState.update {
                        it.copy(
                            loadingMore = false,
                            errorMessage = mapExploreError(e),
                        )
                    }
                }
            loadInFlight = false
        }
    }

    fun retry() = loadInitial()

    private fun mapExploreError(e: Throwable): String = AuthenticatedApiUserMessages.messageFor(e)

    fun trackCardActivated(item: ExploreFeedItem) {
        analytics.track(
            TvAnalyticsEventFactory.userInteraction(
                ts = System.currentTimeMillis(),
                kind = "click",
                detail = "explore_card:${item.id}",
            ),
        )
        analytics.track(
            TvAnalyticsEventFactory.videoStart(
                ts = System.currentTimeMillis(),
                itemId = item.id,
                scheduleIndex = 0,
                mode = TvAnalyticsClipMode.EXPLORE,
                channelId = item.id,
            ),
        )
    }

    override fun onCleared() {
        analytics.flushAsync()
        super.onCleared()
    }
}
