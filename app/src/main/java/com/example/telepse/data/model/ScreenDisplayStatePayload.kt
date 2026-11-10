package com.example.telepse.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Payload for `screen-display-state` (public) and `screen-settings-updated` (private).
 * Mirrors `ScreenDisplayBroadcastPayload` in `backend/src/utils/screen-broadcast.util.ts`.
 *
 * Scalar fields are optional so partial JSON does not fail. [Transient] presence flags
 * distinguish missing keys from explicit null for nullable strings/lists (web overlay sync).
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
    @Transient val eventBackgroundPresent: Boolean = false,
    @Transient val backgroundTypePresent: Boolean = false,
    @Transient val hideProgressBarAmountPresent: Boolean = false,
    @Transient val showLatestAmountPresent: Boolean = false,
    @Transient val moneyForPresent: Boolean = false,
    @Transient val tipUsageDisclosurePresent: Boolean = false,
    @Transient val performersPresent: Boolean = false,
)
