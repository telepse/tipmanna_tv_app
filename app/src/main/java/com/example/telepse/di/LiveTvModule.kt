package com.example.telepse.di

import com.example.telepse.livetv.PusherGlobalScheduleRefreshSignals
import com.example.telepse.livetv.TvScheduleRefreshSignals
import com.example.telepse.livetv.TvScheduleRepositorySource
import com.example.telepse.livetv.TvScheduleSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LiveTvModule {
    @Binds
    @Singleton
    abstract fun bindTvScheduleSource(impl: TvScheduleRepositorySource): TvScheduleSource

    @Binds
    @Singleton
    abstract fun bindTvScheduleRefreshSignals(
        impl: PusherGlobalScheduleRefreshSignals,
    ): TvScheduleRefreshSignals
}
