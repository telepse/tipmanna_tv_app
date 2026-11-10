package com.example.telepse.data.network

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.telepse.di.RecoveryAuthorizedClient
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideHttpClient(ktorClient: KtorClient): HttpClient {
        return ktorClient.httpClient
    }

    @Provides
    @Singleton
    @RecoveryAuthorizedClient
    fun provideRecoveryHttpClient(ktorClient: KtorClient): HttpClient {
        return ktorClient.recoveryHttpClient
    }
    
    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context
    ): NetworkMonitor {
        return NetworkMonitor(context)
    }
}

