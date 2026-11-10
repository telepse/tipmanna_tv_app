package com.example.telepse.data.display

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the last venue Screen Display event so a process restart / TV sleep-wake
 * can resume the HUD without re-entering an event code (venue kiosk behavior).
 *
 * Transient pause (sleep, process death) must NOT clear the session.
 * Only [clearLastEventId] (explicit Back / Hub exit) ends the session.
 */
@Singleton
class ScreenDisplaySessionStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun lastEventId(): String? = prefs.getString(KEY_LAST_EVENT_ID, null)?.takeIf { it.isNotBlank() }

    fun saveLastEventId(eventId: String) {
        // commit() so a sudden sleep/kill after Load Overlay cannot lose the session.
        val ok =
            prefs.edit()
                .putString(KEY_LAST_EVENT_ID, eventId)
                .commit()
        Log.d(LOG_TAG, "saveLastEventId eventId=$eventId committed=$ok")
    }

    fun clearLastEventId() {
        val ok = prefs.edit().remove(KEY_LAST_EVENT_ID).commit()
        Log.d(LOG_TAG, "clearLastEventId (explicit exit) committed=$ok")
    }

    companion object {
        private const val LOG_TAG = "ScreenDisplaySession"
        private const val PREFS_NAME = "tipmanna_tv_screen_display"
        private const val KEY_LAST_EVENT_ID = "last_event_id"
    }
}
