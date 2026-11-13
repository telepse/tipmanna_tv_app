package com.example.telepse.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ContentReportReasonTest {
    @Test
    fun wireValues_matchBackendContentReportReasonEnum() {
        assertEquals("verbally_abusive", ContentReportReason.VerballyAbusive.wireValue)
        assertEquals("sexual_harassment", ContentReportReason.SexualHarassment.wireValue)
        assertEquals("promotes_violence", ContentReportReason.PromotesViolence.wireValue)
        assertEquals("deceptive", ContentReportReason.Deceptive.wireValue)
        assertEquals("other", ContentReportReason.Other.wireValue)
    }
}
