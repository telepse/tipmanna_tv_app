package com.example.telepse.tips

import com.example.telepse.data.model.WalletDebitCharacterLine
import com.example.telepse.data.model.WalletDebitRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WalletDebitSubmissionPlannerTest {
    private val fingerprint =
        WalletDebitFingerprint(
            eventId = "11111111-1111-1111-1111-111111111111",
            characterId = "char-a",
            amount = 5.0,
            pin = "1234",
        )

    private fun sampleRequest(timeLabel: String = "12:00", pin: String = fingerprint.pin) =
        WalletDebitRequest(
            pin = pin,
            amount = fingerprint.amount,
            eventId = fingerprint.eventId,
            characters =
                listOf(
                    WalletDebitCharacterLine(
                        name = "Wave",
                        time = timeLabel,
                        price = fingerprint.amount,
                        characterId = fingerprint.characterId,
                    ),
                ),
        )

    @Test
    fun retryAfterFailure_reusesIdempotencyKeyAndFrozenSnapshot_appliesCurrentPin() {
        val firstRequest = sampleRequest("10:01")
        val pending =
            PendingWalletDebit(
                idempotencyKey = "key-stable",
                identity = WalletDebitTipIdentity.from(fingerprint),
                requestSnapshot = WalletDebitRequestSnapshot.from(firstRequest),
            )
        val plan =
            WalletDebitSubmissionPlanner.planSubmit(
                pending = pending,
                fingerprint = fingerprint.copy(pin = "9999"),
                buildNewRequest = { sampleRequest("10:99", pin = "9999") },
            )
        assertEquals("key-stable", plan.idempotencyKey)
        assertEquals("9999", plan.request.pin)
        assertEquals("10:01", plan.request.characters.first().time)
    }

    @Test(expected = WalletDebitPendingConflictException::class)
    fun differentTipIdentity_throwsConflict() {
        val pending =
            PendingWalletDebit(
                idempotencyKey = "key-old",
                identity = WalletDebitTipIdentity.from(fingerprint),
                requestSnapshot = WalletDebitRequestSnapshot.from(sampleRequest()),
            )
        WalletDebitSubmissionPlanner.planSubmit(
            pending = pending,
            fingerprint = fingerprint.copy(amount = 10.0),
            buildNewRequest = { sampleRequest().copy(amount = 10.0) },
        )
    }

    @Test
    fun firstSubmit_generatesKeyAndStoresPending() {
        val built = sampleRequest()
        val plan =
            WalletDebitSubmissionPlanner.planSubmit(
                pending = null,
                fingerprint = fingerprint,
                buildNewRequest = { built },
            )
        assertEquals(built, plan.request)
        assertEquals(plan.idempotencyKey, plan.pendingAfterSubmit.idempotencyKey)
        assertEquals(WalletDebitTipIdentity.from(fingerprint), plan.pendingAfterSubmit.identity)
    }

    @Test
    fun clearedPending_newSubmitGetsNewKey() {
        val built = sampleRequest("11:00")
        val plan =
            WalletDebitSubmissionPlanner.planSubmit(
                pending = null,
                fingerprint = fingerprint,
                buildNewRequest = { built },
            )
        assertNotEquals("", plan.idempotencyKey)
    }
}
