package com.example.telepse.livetv

import com.example.telepse.data.model.TvScheduleItem

/**
 * Resolves tip-catalog event id for a TV schedule row.
 * Matches web `resolveTvScheduleEventId`: `channel.eventId` then `defaultChannelEventId`.
 */
object ResolveTvScheduleEventId {
    fun resolve(item: TvScheduleItem?): String {
        if (item == null) return ""
        val fromChannel = item.channel?.eventId?.trim().orEmpty()
        if (fromChannel.isNotEmpty()) return fromChannel
        return item.defaultChannelEventId?.trim().orEmpty()
    }

    fun resolveAtLiveIndex(
        items: List<TvScheduleItem>,
        scheduleIndex: Int?,
    ): String {
        if (items.isEmpty()) return ""
        val index = scheduleIndex ?: 0
        if (index !in items.indices) return ""
        return resolve(items[index])
    }
}
