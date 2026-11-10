package com.example.telepse.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class TipperSessionSnapshot(
    val loggedIn: Boolean,
    val tipperEmail: String?,
)

@Singleton
class SessionStore private constructor(
    private val prefs: SharedPreferences,
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(openEncryptedPrefs(context))

    private val _snapshot = MutableStateFlow(readSnapshot())
    val snapshot: StateFlow<TipperSessionSnapshot> = _snapshot.asStateFlow()

    fun accessToken(): String? = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun tipperEmail(): String? = prefs.getString(KEY_EMAIL, null)?.takeIf { it.isNotBlank() }

    fun isLoggedIn(): Boolean = accessToken() != null

    fun saveSession(token: String, email: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_EMAIL, email.trim())
            .apply()
        publishSnapshot()
    }

    fun clear() {
        prefs.edit().clear().apply()
        publishSnapshot()
    }

    private fun readSnapshot(): TipperSessionSnapshot =
        TipperSessionSnapshot(
            loggedIn = isLoggedIn(),
            tipperEmail = tipperEmail(),
        )

    private fun publishSnapshot() {
        _snapshot.value = readSnapshot()
    }

    companion object {
        private const val PREFS_NAME = "tipmanna_tv_session"
        private const val KEY_TOKEN = "access_token"
        private const val KEY_EMAIL = "tipper_email"

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
        internal fun createPlainForUnitTests(context: Context): SessionStore =
            SessionStore(
                context.getSharedPreferences(
                    "$PREFS_NAME.test",
                    Context.MODE_PRIVATE,
                ),
            )
    }
}
