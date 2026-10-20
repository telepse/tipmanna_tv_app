package com.example.telepse.tips

import java.util.UUID

object WalletTipValidation {
    fun normalizeEventId(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return runCatching { UUID.fromString(trimmed); trimmed }.getOrNull()
    }

    fun isValidPin(pin: String): Boolean = pin.matches(Regex("^\\d{4}$"))

    fun amountsMatch(debitAmount: Double, characterPrice: Double): Boolean {
        return kotlin.math.abs(debitAmount - characterPrice) < 0.001
    }
}
