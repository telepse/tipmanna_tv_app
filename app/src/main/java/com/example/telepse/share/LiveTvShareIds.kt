package com.example.telepse.share

import com.example.telepse.data.model.TvScheduleResponse
import com.example.telepse.livetv.LiveScheduleTarget

data class TipmannaShareIds(
    val eventId: String,
    val channelId: String,
)

/**
 * Resolves Tipmanna identifiers for LIVE share QR from schedule + active target.
 * Channel id is the schedule row id (web `?v=` channel selector).
 */
fun resolveLiveTvShareIds(
    schedule: TvScheduleResponse?,
    target: LiveScheduleTarget?,
): TipmannaShareIds? {
    val index = target?.scheduleIndex ?: schedule?.livePosition?.scheduleIndex ?: return null
    val item = schedule?.items?.getOrNull(index) ?: return null
    val channelFromNested = item.channel?.id?.trim().orEmpty()
    val channelFromRow = item.id?.trim().orEmpty()
    val channelId =
        when {
            TipmannaShareUrl.isLikelyUuid(channelFromNested) -> channelFromNested
            TipmannaShareUrl.isLikelyUuid(channelFromRow) -> channelFromRow
            else -> ""
        }
    val eventId = target?.eventId?.trim().orEmpty().ifBlank { item.channel?.eventId?.trim().orEmpty() }
    if (!TipmannaShareUrl.isLikelyUuid(eventId) || !TipmannaShareUrl.isLikelyUuid(channelId)) {
        return null
    }
    return TipmannaShareIds(eventId = eventId, channelId = channelId)
}
