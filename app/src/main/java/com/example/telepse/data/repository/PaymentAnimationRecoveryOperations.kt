package com.example.telepse.data.repository

import com.example.telepse.data.model.AnimationRecoveryClaimResponse
import com.example.telepse.data.model.AnimationRecoveryExchangeResponse
import com.example.telepse.data.model.AnimationRecoveryProgressRequest

interface PaymentAnimationRecoveryOperations {
    suspend fun exchangeAnimationRecovery(
        recoveryState: String,
        transactionId: String,
        stripePaymentIntentId: String,
        eventId: String,
    ): Result<AnimationRecoveryExchangeResponse>

    suspend fun claimAnimationRecovery(
        recoverySessionToken: String,
    ): Result<AnimationRecoveryClaimResponse>

    suspend fun progressAnimationRecovery(
        recoverySessionToken: String,
        body: AnimationRecoveryProgressRequest,
    ): Result<Unit>
}
