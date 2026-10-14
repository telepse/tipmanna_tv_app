package com.example.telepse.livetv

import com.example.telepse.data.repository.TvScheduleRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvScheduleRepositorySource @Inject constructor(
    private val repository: TvScheduleRepository,
) : TvScheduleSource {
    override suspend fun fetchGlobalSchedule(ifNoneMatch: String?) =
        repository.fetchGlobalSchedule(ifNoneMatch)
}
