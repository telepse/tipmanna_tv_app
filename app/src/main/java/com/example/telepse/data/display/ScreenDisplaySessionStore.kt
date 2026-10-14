package com.example.telepse.data.display

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the last venue Screen Display event so a process restart can resume the HUD
 * without re-entering an event code (venue kiosk behavior).
 */
@Singleton
class ScreenDisplaySessionStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun lastEventId(): String? = prefs.getString(KEY_LAST_EVENT_ID, null)?.takeIf { it.isNotBlank() }

    fun saveLastEventId(eventId: String) {
        prefs.edit().putString(KEY_LAST_EVENT_ID, eventId).apply()
    }

    fun clearLastEventId() {
        prefs.edit().remove(KEY_LAST_EVENT_ID).apply()
    }

    companion object {
        private const val PREFS_NAME = "tipmanna_tv_screen_display"
        private const val KEY_LAST_EVENT_ID = "last_event_id"
    }
}
