package com.example.telepse.di

import com.example.telepse.data.repository.AuthRepository
import com.example.telepse.data.repository.TipperSession
import com.example.telepse.data.repository.WalletOperations
import com.example.telepse.data.repository.WalletRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindWalletOperations(impl: WalletRepository): WalletOperations

    @Binds
    @Singleton
    abstract fun bindTipperSession(impl: AuthRepository): TipperSession
}
