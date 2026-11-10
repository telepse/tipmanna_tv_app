package com.example.telepse.tips

import android.util.Log

/** Structured wallet paid-animation diagnostics (no tokens or secrets). */
object WalletPaidAnimationTrace {
    private const val TAG = "WalletPaidAnimationTrace"

    fun log(
        stage: String,
        transactionId: String? = null,
        result: String? = null,
        httpStatus: Int? = null,
        detail: String? = null,
    ) {
        val tx = transactionId?.trim()?.take(8)?.takeIf { it.isNotEmpty() }
        Log.d(
            TAG,
            buildString {
                append("stage=").append(stage)
                if (tx != null) append(" tx=").append(tx)
                if (result != null) append(" result=").append(result)
                if (httpStatus != null) append(" http=").append(httpStatus)
                if (detail != null) append(" detail=").append(detail.take(120))
            },
        )
    }
}
