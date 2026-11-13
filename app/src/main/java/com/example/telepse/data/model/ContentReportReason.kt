package com.example.telepse.data.model

/**
 * Mirrors backend `ContentReportReason` (POST `/api/v1/messages/channel/:channelId/report`).
 */
enum class ContentReportReason(val wireValue: String) {
    VerballyAbusive("verbally_abusive"),
    SexualHarassment("sexual_harassment"),
    PromotesViolence("promotes_violence"),
    Deceptive("deceptive"),
    Other("other"),
    ;

    val displayLabel: String
        get() =
            when (this) {
                VerballyAbusive -> "Verbally abusive"
                SexualHarassment -> "Sexual harassment"
                PromotesViolence -> "Promotes violence"
                Deceptive -> "Deceptive"
                Other -> "Other"
            }
}
