package com.example.telepse.tips

import android.util.Log
import com.example.telepse.data.model.TipCharacterCatalogItem
import com.example.telepse.data.realtime.PusherService
import com.example.telepse.data.repository.AnimationContractRepository
import com.example.telepse.player.TipAnimationGuard
import com.example.telepse.ui.viewmodel.AnimationTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Subscribes to authoritative `tip-animation` events on `global-live-tv` (bound when
 * [TvModeEngine] starts [com.example.telepse.livetv.PusherGlobalScheduleRefreshSignals])
 * and drives LIVE TV overlay animations. Does not treat checkout "paid" as an animation signal.
 */
class LiveTvTipAnimationHost(
    private val pusherService: PusherService,
    private val animationContractRepository: AnimationContractRepository,
    private val scope: CoroutineScope,
) {
    private val tag = "LiveTvTipAnimation"
    private val guard = TipAnimationGuard(AnimationContractRepository.DEFAULT_STALE_MS)

    private val _pendingAnimations = MutableStateFlow<List<AnimationTrigger>>(emptyList())
    val pendingAnimations: StateFlow<List<AnimationTrigger>> = _pendingAnimations.asStateFlow()

    private var collectorJob: Job? = null
    private var activeEventId: String = ""
    private var catalogByLowerName: Map<String, TipCharacterCatalogItem> = emptyMap()

    fun start() {
        if (collectorJob != null) return
        collectorJob =
            scope.launch {
                launch {
                    val staleMs = animationContractRepository.fetchStaleThresholdMs()
                    guard.updateStaleThresholdMs(staleMs)
                    Log.d(tag, "Animation guard staleThresholdMs=$staleMs")
                }
                Log.d(tag, "Observing tip-animation (global-live-tv); activeEventId=$activeEventId")
                pusherService.observeTipAnimation()
                    .catch { e ->
                        if (e !is kotlinx.coroutines.CancellationException) {
                            Log.e(tag, "tip-animation flow error", e)
                        }
                    }
                    .collect { payload ->
                        val activeId = activeEventId.trim()
                        if (activeId.isEmpty()) {
                            Log.d(tag, "Ignored tip-animation — no active programme eventId")
                            return@collect
                        }
                        if (!guard.shouldAccept(payload, activeId)) {
                            WalletPaidAnimationTrace.log(
                                stage = "tv_paid_animation_event_rejected",
                                transactionId = payload.transactionId,
                                detail = "pusher guard payloadEvent=${payload.eventId?.take(8)} active=${activeId.take(8)}",
                            )
                            Log.d(
                                tag,
                                "tip-animation rejected by guard payloadEventId=${payload.eventId} active=$activeId",
                            )
                            return@collect
                        }
                        WalletPaidAnimationTrace.log(
                            stage = "tv_paid_animation_event_received",
                            transactionId = payload.transactionId,
                            detail = "channel=pusher tips=${payload.selectedTips.size}",
                        )
                        Log.d(
                            tag,
                            "tip-animation accepted eventId=$activeId tips=${payload.selectedTips.size} tx=${payload.transactionId?.take(8)}",
                        )
                        enqueue(LiveTvTipAnimationMapper.fromPaymentEvent(payload, catalogByLowerName))
                    }
            }
    }

    fun stop() {
        collectorJob?.cancel()
        collectorJob = null
        _pendingAnimations.value = emptyList()
        guard.reset()
        Log.d(tag, "Stopped tip-animation observer")
    }

    fun onProgrammeEventChanged(eventId: String) {
        val trimmed = eventId.trim()
        if (trimmed == activeEventId) return
        activeEventId = trimmed
        _pendingAnimations.update { list -> list.filterNot { it.id.startsWith("preview:") } }
        guard.reset()
        Log.d(tag, "Active programme eventId=$trimmed")
    }

    fun updateCatalog(characters: List<TipCharacterCatalogItem>) {
        catalogByLowerName = characters.associateBy { it.name.trim().lowercase() }
    }

    fun previewSelect(character: TipCharacterCatalogItem) {
        _pendingAnimations.update { current ->
            val withoutPreview = current.filterNot { it.id.startsWith("preview:") }
            withoutPreview + LiveTvTipAnimationMapper.preview(character)
        }
        Log.d(tag, "Preview animation queued character=${character.name}")
    }

    fun removeAnimation(animation: AnimationTrigger) {
        _pendingAnimations.update { list -> list.filter { it.id != animation.id } }
        Log.d(tag, "Animation removed id=${animation.id}")
    }

    /**
     * Same guard + queue path as Pusher `tip-animation`, for authoritative recovery claim payloads.
     */
    fun acceptAuthoritativePaymentAnimation(
        payload: com.example.telepse.data.model.TipAnimationEvent,
        activeEventIdOverride: String? = null,
    ) {
        val activeId = (activeEventIdOverride?.trim()?.takeIf { it.isNotEmpty() } ?: activeEventId.trim())
        if (activeId.isEmpty()) {
            Log.d(tag, "Ignored recovery animation — no active programme eventId")
            return
        }
        if (!guard.shouldAccept(payload, activeId)) {
            WalletPaidAnimationTrace.log(
                stage = "tv_paid_animation_event_rejected",
                transactionId = payload.transactionId,
                detail = "recovery guard payloadEvent=${payload.eventId?.take(8)} active=${activeId.take(8)}",
            )
            Log.d(
                tag,
                "recovery animation rejected by guard payloadEventId=${payload.eventId} active=$activeId",
            )
            return
        }
        WalletPaidAnimationTrace.log(
            stage = "tv_paid_animation_triggered",
            transactionId = payload.transactionId,
            detail = "recovery tips=${payload.selectedTips.size}",
        )
        Log.d(
            tag,
            "recovery animation accepted eventId=$activeId tips=${payload.selectedTips.size} tx=${payload.transactionId?.take(8)}",
        )
        enqueue(LiveTvTipAnimationMapper.fromPaymentEvent(payload, catalogByLowerName))
    }

    private fun enqueue(triggers: List<AnimationTrigger>) {
        if (triggers.isEmpty()) {
            Log.w(tag, "No animation triggers mapped from tip-animation payload")
            return
        }
        _pendingAnimations.update { it + triggers }
        Log.d(tag, "Queued ${triggers.size} payment animation(s); queue=${_pendingAnimations.value.size}")
    }
}

/** Collect distinct catalog rows from LIVE tip UI state. */
fun liveTvTipCatalogCharacters(tip: LiveTvTipUiState): List<TipCharacterCatalogItem> {
    val fromSets = tip.drawerSets.flatMap { it.characters }
    return (tip.stripCharacters + fromSets).distinctBy { it.id }
}
