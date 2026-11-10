package com.example.telepse.tips

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.telepse.data.model.WalletDebitCharacterLine
import com.example.telepse.data.model.WalletDebitRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class WalletDebitSubmitSessionTest {
    private lateinit var context: Context
    private lateinit var store: WalletDebitPendingStore

    private val fingerprint =
        WalletDebitFingerprint(
            eventId = "11111111-1111-1111-1111-111111111111",
            characterId = "char-a",
            amount = 5.0,
            pin = "1234",
        )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        store = WalletDebitPendingStore.createPlainForUnitTests(context)
        store.clear()
    }

    private fun newSession(): WalletDebitSubmitSession = WalletDebitSubmitSession(store)

    private fun request(time: String) =
        WalletDebitRequest(
            pin = "1234",
            amount = 5.0,
            eventId = fingerprint.eventId,
            characters =
                listOf(
                    WalletDebitCharacterLine(
                        name = "Wave",
                        time = time,
                        price = 5.0,
                        characterId = fingerprint.characterId,
                    ),
                ),
        )

    @Test
    fun inFlightGuard_allowsOnlyOneConcurrentSubmit() {
        val session = newSession()
        assertTrue(session.tryBeginSubmit())
        assertFalse(session.tryBeginSubmit())
        session.finishSubmit()
        assertTrue(session.tryBeginSubmit())
    }

    @Test
    fun failureThenRetry_reusesKey_appliesNewPinFromFingerprint() {
        val session = newSession()
        session.tryBeginSubmit()
        val first =
            session.planSubmit(fingerprint) { request("09:00") }
        session.finishSubmit()

        session.tryBeginSubmit()
        val retry =
            session.planSubmit(fingerprint.copy(pin = "5678")) { request("09:59") }
        session.finishSubmit()

        assertEquals(first.idempotencyKey, retry.idempotencyKey)
        assertEquals("5678", retry.request.pin)
        assertEquals("09:00", retry.request.characters.first().time)
    }

    @Test
    fun success_clearsPendingAndStore() {
        val session = newSession()
        session.tryBeginSubmit()
        session.planSubmit(fingerprint) { request("09:00") }
        session.clearAfterAuthoritativeSuccess()
        session.finishSubmit()
        assertNull(session.pendingWalletDebit)
        assertNull(store.load())

        session.tryBeginSubmit()
        val next = session.planSubmit(fingerprint) { request("10:00") }
        session.finishSubmit()
        assertEquals(request("10:00"), next.request)
    }

    @Test
    fun sessionSurvivesRecreationFromStore() {
        val session1 = newSession()
        session1.tryBeginSubmit()
        val plan = session1.planSubmit(fingerprint) { request("08:00") }
        session1.finishSubmit()

        val session2 = newSession()
        assertEquals(plan.idempotencyKey, session2.pendingWalletDebit?.idempotencyKey)
    }

    @Test
    fun corruptPersistedRecord_clearsStoreOnRestore() {
        context
            .getSharedPreferences("tipmanna_tv_wallet_debit_pending.test", Context.MODE_PRIVATE)
            .edit()
            .putString("pending_v1", "{not-json")
            .apply()
        val session = newSession()
        assertNull(session.pendingWalletDebit)
        assertNull(store.load())
    }
}
