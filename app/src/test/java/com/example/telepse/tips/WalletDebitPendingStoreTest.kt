package com.example.telepse.tips

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.telepse.data.model.WalletDebitCharacterLine
import com.example.telepse.data.model.WalletDebitRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class WalletDebitPendingStoreTest {
    private lateinit var store: WalletDebitPendingStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = WalletDebitPendingStore.createPlainForUnitTests(context)
        store.clear()
    }

    @Test
    fun persistedJson_neverContainsPin() {
        val request =
            WalletDebitRequest(
                pin = "secret-pin",
                amount = 5.0,
                eventId = "11111111-1111-1111-1111-111111111111",
                characters =
                    listOf(
                        WalletDebitCharacterLine(
                            name = "Wave",
                            time = "12:00",
                            price = 5.0,
                            characterId = "char-a",
                        ),
                    ),
            )
        store.save(
            PersistedWalletDebitPending(
                idempotencyKey = "key-1",
                eventId = request.eventId,
                characterId = "char-a",
                amount = 5.0,
                requestSnapshot = WalletDebitRequestSnapshot.from(request),
                savedAtEpochMs = 1L,
            ),
        )
        val raw = store.peekRawJsonForTests().orEmpty()
        assertFalse(raw.contains("secret-pin"))
        assertFalse(raw.contains("\"pin\""))
    }

    @Test
    fun roundTrip_restoreRequiresPinFromUi() {
        val snapshot =
            WalletDebitRequestSnapshot(
                amount = 5.0,
                eventId = "11111111-1111-1111-1111-111111111111",
                characters =
                    listOf(
                        WalletDebitCharacterLine(
                            name = "Wave",
                            time = "09:00",
                            price = 5.0,
                            characterId = "char-a",
                        ),
                    ),
            )
        store.save(
            PersistedWalletDebitPending(
                idempotencyKey = "idem-abc",
                eventId = snapshot.eventId,
                characterId = "char-a",
                amount = 5.0,
                requestSnapshot = snapshot,
                savedAtEpochMs = 2L,
            ),
        )
        val loaded = store.load()
        assertNotNull(loaded)
        assertEquals("idem-abc", loaded!!.idempotencyKey)
        val rebuilt = loaded.requestSnapshot.toRequest("fresh-pin")
        assertEquals("fresh-pin", rebuilt.pin)
    }

    @Test
    fun malformedJson_clearsAndReturnsNull() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context
            .getSharedPreferences("tipmanna_tv_wallet_debit_pending.test", Context.MODE_PRIVATE)
            .edit()
            .putString("pending_v1", "{broken")
            .apply()
        val fresh = WalletDebitPendingStore.createPlainForUnitTests(context)
        assertNull(fresh.load())
        assertNull(fresh.peekRawJsonForTests())
    }

    @Test
    fun clear_removesPending() {
        store.save(
            PersistedWalletDebitPending(
                idempotencyKey = "k",
                eventId = "e",
                characterId = "c",
                amount = 1.0,
                requestSnapshot =
                    WalletDebitRequestSnapshot(
                        amount = 1.0,
                        eventId = "e",
                        characters = emptyList(),
                    ),
                savedAtEpochMs = 0L,
            ),
        )
        assertTrue(store.peekRawJsonForTests().orEmpty().isNotEmpty())
        store.clear()
        assertNull(store.load())
    }
}
