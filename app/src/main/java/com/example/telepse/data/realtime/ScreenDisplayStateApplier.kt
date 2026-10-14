package com.example.telepse.data.realtime

import com.example.telepse.data.model.Event
import com.example.telepse.data.model.ScreenDisplayStatePayload

/**
 * Merges a screen display broadcast into the loaded [Event], matching web
 * `useScreenOverlayPublicSync` patch semantics for fields the Android HUD supports.
 */
object ScreenDisplayStateApplier {
    fun apply(current: Event, patch: ScreenDisplayStatePayload): Event {
        return current.copy(
            eventName = patch.eventName ?: current.eventName,
            eventCode = patch.eventCode ?: current.eventCode,
            eventSlug = patch.hostEventSlug ?: current.eventSlug,
            scanLink = patch.scanLink ?: current.scanLink,
            amountToRaise = patch.amountToRaise ?: current.amountToRaise,
            amountRaised = patch.amountRaised ?: current.amountRaised,
            numOfTips = patch.numOfTips ?: current.numOfTips,
            latestTipAmount = patch.latestTipAmount ?: current.latestTipAmount,
            eventBackground = patch.eventBackground ?: current.eventBackground,
            backgroundType = patch.backgroundType ?: current.backgroundType,
            hideProgressBarAmount = patch.hideProgressBarAmount ?: current.hideProgressBarAmount,
            showLatestAmount = patch.showLatestAmount ?: current.showLatestAmount,
            moneyFor = patch.moneyFor ?: current.moneyFor,
            tipUsageDisclosure = patch.tipUsageDisclosure ?: current.tipUsageDisclosure,
            performers = patch.performers ?: current.performers,
        )
    }
}
