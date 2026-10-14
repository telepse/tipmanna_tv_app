package com.example.telepse.data.model

import kotlinx.serialization.Serializable

/**
 * Payload for `screen-display-state` (public) and `screen-settings-updated` (private).
 * Mirrors `ScreenDisplayBroadcastPayload` in `backend/src/utils/screen-broadcast.util.ts`.
 * Fields are optional at parse time so partial JSON does not fail deserialization;
 * [com.example.telepse.data.realtime.ScreenDisplayStateApplier] applies only present values.
 */
@Serializable
data class ScreenDisplayStatePayload(
    val eventId: String? = null,
    val eventName: String? = null,
    val eventCode: String? = null,
    val hostEventSlug: String? = null,
    val scanLink: String? = null,
    val amountToRaise: Double? = null,
    val amountRaised: Double? = null,
    val numOfTips: Int? = null,
    val latestTipAmount: Double? = null,
    val eventBackground: String? = null,
    val backgroundType: String? = null,
    val hideProgressBarAmount: Boolean? = null,
    val showLatestAmount: Boolean? = null,
    val moneyFor: String? = null,
    val tipUsageDisclosure: List<TipUsageDisclosure>? = null,
    val performers: List<Performer>? = null,
)
