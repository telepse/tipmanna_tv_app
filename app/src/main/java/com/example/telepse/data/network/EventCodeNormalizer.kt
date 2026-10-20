package com.example.telepse.data.network

import java.util.Locale

/**
 * Normalizes venue event codes for lookup: trim + locale-independent uppercase.
 * Host codes are generated uppercase; TV remotes often inject mixed case.
 */
fun normalizeEventCode(raw: String): String =
    raw.trim().uppercase(Locale.ROOT)
