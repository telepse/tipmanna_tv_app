package com.example.telepse.livetv

import com.example.telepse.data.model.TvScheduleResponse
import com.example.telepse.player.YoutubeVideoIdUtil

data class LiveScheduleTarget(
    val videoId: String,
    val startSeconds: Float,
    val scheduleIndex: Int,
    val scheduleState: String?,
)

object LiveScheduleTargetResolver {
    fun resolveAtIndex(
        schedule: TvScheduleResponse,
        index: Int,
        startSeconds: Float,
    ): Result<LiveScheduleTarget> {
        if (schedule.items.isEmpty()) {
            return Result.failure(IllegalStateException(schedule.message ?: "Schedule has no items"))
        }
        if (index !in schedule.items.indices) {
            return Result.failure(IllegalStateException("Index $index out of range"))
        }
        val url =
            schedule.items[index].channel?.url
                ?: return Result.failure(IllegalStateException("No channel URL at index $index"))
        val videoId =
            YoutubeVideoIdUtil.fromUrl(url)
                ?: return Result.failure(IllegalStateException("Unplayable URL at index $index"))
        return Result.success(
            LiveScheduleTarget(
                videoId = videoId,
                startSeconds = startSeconds,
                scheduleIndex = index,
                scheduleState = schedule.scheduleState,
            ),
        )
    }

    fun resolve(schedule: TvScheduleResponse): Result<LiveScheduleTarget> {
        if (schedule.items.isEmpty()) {
            return Result.failure(
                IllegalStateException(
                    schedule.message ?: "Schedule has no items",
                ),
            )
        }
        val index = schedule.livePosition?.scheduleIndex ?: 0
        if (index !in schedule.items.indices) {
            return Result.failure(
                IllegalStateException("Live index $index out of range (${schedule.items.size} items)"),
            )
        }
        val url =
            schedule.items[index].channel?.url
                ?: return Result.failure(
                    IllegalStateException("No channel URL at live index $index"),
                )
        val videoId =
            YoutubeVideoIdUtil.fromUrl(url)
                ?: return Result.failure(
                    IllegalStateException("Unplayable or malformed URL at index $index"),
                )
        val offset = (schedule.livePosition?.offsetSeconds ?: 0).toFloat()
        return Result.success(
            LiveScheduleTarget(
                videoId = videoId,
                startSeconds = offset,
                scheduleIndex = index,
                scheduleState = schedule.scheduleState,
            ),
        )
    }
}
