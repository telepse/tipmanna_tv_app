package com.example.telepse.tips

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class PersistedWalletDebitPending(
    val idempotencyKey: String,
    val eventId: String,
    val characterId: String,
    val amount: Double,
    val requestSnapshot: WalletDebitRequestSnapshot,
    val savedAtEpochMs: Long,
)

interface WalletDebitPendingPersistence {
    fun load(): PersistedWalletDebitPending?

    fun save(record: PersistedWalletDebitPending)

    fun clear()
}

/** In-memory pending store for JVM unit tests (no Android prefs). */
class MemoryWalletDebitPendingPersistence : WalletDebitPendingPersistence {
    @Volatile
    private var record: PersistedWalletDebitPending? = null

    override fun load(): PersistedWalletDebitPending? = record

    override fun save(record: PersistedWalletDebitPending) {
        this.record = record
    }

    override fun clear() {
        record = null
    }
}

/**
 * Durable unresolved wallet debit (no PIN). Cleared on authoritative success or explicit dismiss.
 */
@Singleton
class WalletDebitPendingStore private constructor(
    private val prefs: SharedPreferences,
) : WalletDebitPendingPersistence {
    @Inject
    constructor(@ApplicationContext context: Context) : this(openEncryptedPrefs(context))

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    override fun load(): PersistedWalletDebitPending? {
        val raw = prefs.getString(KEY_PENDING_JSON, null)?.trim().orEmpty()
        if (raw.isEmpty()) return null
        return runCatching {
            json.decodeFromString(PersistedWalletDebitPending.serializer(), raw)
        }.getOrElse {
            clear()
            null
        }
    }

    override fun save(record: PersistedWalletDebitPending) {
        prefs.edit()
            .putString(KEY_PENDING_JSON, json.encodeToString(PersistedWalletDebitPending.serializer(), record))
            .apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY_PENDING_JSON).apply()
    }

    internal fun peekRawJsonForTests(): String? = prefs.getString(KEY_PENDING_JSON, null)

    companion object {
        private const val PREFS_NAME = "tipmanna_tv_wallet_debit_pending"
        private const val KEY_PENDING_JSON = "pending_v1"

        private fun openEncryptedPrefs(context: Context): SharedPreferences =
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )

        @VisibleForTesting
        internal fun createPlainForUnitTests(context: Context): WalletDebitPendingStore =
            WalletDebitPendingStore(
                context.getSharedPreferences("$PREFS_NAME.test", Context.MODE_PRIVATE),
            )
    }
}
