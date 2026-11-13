package com.example.telepse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telepse.data.model.ContentReportReason
import com.example.telepse.data.repository.BlockedHostRepository
import com.example.telepse.data.repository.ContentReportRepository
import com.example.telepse.data.repository.ContentReportRepository.ContentReportException
import com.example.telepse.data.repository.HostIdentityRepository
import com.example.telepse.data.repository.HostReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TvModerationDialog {
    None,
    ReportVideo,
    ReportHost,
    BlockHostConfirm,
}

sealed interface TvModerationEffect {
    data object RefreshFeeds : TvModerationEffect
}

data class TvModerationUiState(
    val dialog: TvModerationDialog = TvModerationDialog.None,
    val activeChannelId: String? = null,
    val selectedReason: ContentReportReason = ContentReportReason.Deceptive,
    val otherText: String = "",
    val submitting: Boolean = false,
    val resolvedHostId: String? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class TvModerationViewModel @Inject constructor(
    private val contentReportRepository: ContentReportRepository,
    private val hostReportRepository: HostReportRepository,
    private val blockedHostRepository: BlockedHostRepository,
    private val hostIdentityRepository: HostIdentityRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TvModerationUiState())
    val state: StateFlow<TvModerationUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<TvModerationEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<TvModerationEffect> = _effects.asSharedFlow()

    fun openReportVideoDialog(channelId: String) {
        _state.update {
            it.copy(
                dialog = TvModerationDialog.ReportVideo,
                activeChannelId = channelId,
                successMessage = null,
                errorMessage = null,
                resolvedHostId = null,
            )
        }
        resolveHost(channelId)
    }

    fun openReportHostDialog(channelId: String) {
        _state.update {
            it.copy(
                dialog = TvModerationDialog.ReportHost,
                activeChannelId = channelId,
                successMessage = null,
                errorMessage = null,
                resolvedHostId = null,
            )
        }
        resolveHost(channelId)
    }

    fun openBlockHostDialog(channelId: String) {
        _state.update {
            it.copy(
                dialog = TvModerationDialog.BlockHostConfirm,
                activeChannelId = channelId,
                successMessage = null,
                errorMessage = null,
                resolvedHostId = null,
            )
        }
        resolveHost(channelId)
    }

    private fun resolveHost(channelId: String) {
        viewModelScope.launch {
            hostIdentityRepository
                .resolveHostIdForChannel(channelId)
                .onSuccess { hostId ->
                    _state.update { it.copy(resolvedHostId = hostId) }
                }
                .onFailure {
                    _state.update {
                        it.copy(errorMessage = "Could not resolve host for this video.")
                    }
                }
        }
    }

    fun dismissDialog() {
        _state.update {
            it.copy(
                dialog = TvModerationDialog.None,
                otherText = "",
                submitting = false,
                activeChannelId = null,
            )
        }
    }

    fun selectReason(reason: ContentReportReason) {
        _state.update { it.copy(selectedReason = reason, errorMessage = null) }
    }

    fun updateOtherText(text: String) {
        _state.update { it.copy(otherText = text, errorMessage = null) }
    }

    fun clearTransientMessage() {
        _state.update { it.copy(successMessage = null, errorMessage = null) }
    }

    fun submitVideoReport(channelId: String) {
        val current = _state.value
        if (current.submitting) return
        _state.update { it.copy(submitting = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            submitVideoReportInternal(channelId)
        }
    }

    fun submitHostReport() {
        val current = _state.value
        val hostId = current.resolvedHostId
        val channelId = current.activeChannelId
        if (current.submitting || hostId.isNullOrBlank() || channelId.isNullOrBlank()) {
            _state.update {
                it.copy(errorMessage = it.errorMessage ?: "Host identity is not available.")
            }
            return
        }
        _state.update { it.copy(submitting = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val other =
                if (current.selectedReason == ContentReportReason.Other) {
                    current.otherText
                } else {
                    null
                }
            hostReportRepository
                .reportHost(hostId, current.selectedReason, other, channelId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            submitting = false,
                            dialog = TvModerationDialog.None,
                            otherText = "",
                            successMessage = "Host report submitted for moderator review.",
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error -> handleReportFailure(error) }
        }
    }

    fun confirmBlockHost() {
        val current = _state.value
        val hostId = current.resolvedHostId
        if (current.submitting || hostId.isNullOrBlank()) {
            _state.update {
                it.copy(errorMessage = it.errorMessage ?: "Host identity is not available.")
            }
            return
        }
        _state.update { it.copy(submitting = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            blockedHostRepository
                .blockHost(hostId)
                .onSuccess {
                    _effects.tryEmit(TvModerationEffect.RefreshFeeds)
                    _state.update {
                        it.copy(
                            submitting = false,
                            dialog = TvModerationDialog.None,
                            successMessage =
                                "Host blocked. Their videos will be hidden from LIVE and Explore.",
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    val message =
                        when (error) {
                            is BlockedHostRepository.ModerationException -> error.message
                            else -> error.message ?: "Could not block host."
                        }
                    _state.update {
                        it.copy(
                            submitting = false,
                            errorMessage = message,
                        )
                    }
                }
        }
    }

    private suspend fun submitVideoReportInternal(channelId: String) {
        val current = _state.value
        val other =
            if (current.selectedReason == ContentReportReason.Other) {
                current.otherText
            } else {
                null
            }
        contentReportRepository
            .reportChannelContent(channelId, current.selectedReason, other)
            .onSuccess {
                _state.update {
                    it.copy(
                        submitting = false,
                        dialog = TvModerationDialog.None,
                        otherText = "",
                        successMessage = "Video report submitted for moderator review.",
                        errorMessage = null,
                    )
                }
            }
            .onFailure { error -> handleReportFailure(error) }
    }

    private fun handleReportFailure(error: Throwable) {
        val message =
            when (error) {
                is ContentReportException -> error.message
                else -> error.message ?: "Could not submit report."
            }
        _state.update {
            it.copy(
                submitting = false,
                errorMessage = message,
            )
        }
    }
}
