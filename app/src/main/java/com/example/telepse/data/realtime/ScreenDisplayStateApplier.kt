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
            eventBackground =
                if (patch.eventBackgroundPresent) {
                    patch.eventBackground
                } else {
                    current.eventBackground
                },
            backgroundType =
                if (patch.backgroundTypePresent) {
                    patch.backgroundType
                } else {
                    current.backgroundType
                },
            hideProgressBarAmount =
                if (patch.hideProgressBarAmountPresent) {
                    patch.hideProgressBarAmount == true
                } else {
                    current.hideProgressBarAmount
                },
            showLatestAmount =
                if (patch.showLatestAmountPresent) {
                    patch.showLatestAmount == true
                } else {
                    current.showLatestAmount
                },
            moneyFor =
                if (patch.moneyForPresent) {
                    patch.moneyFor
                } else {
                    current.moneyFor
                },
            tipUsageDisclosure =
                if (patch.tipUsageDisclosurePresent) {
                    patch.tipUsageDisclosure
                } else {
                    current.tipUsageDisclosure
                },
            performers =
                if (patch.performersPresent) {
                    patch.performers
                } else {
                    current.performers
                },
        )
    }
}
