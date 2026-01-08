package com.example.telepse.data.model

import kotlinx.serialization.Serializable

/**
 * Model for tip-animation Pusher event payload.
 * 
 * This event is fired when a user makes a payment/tip.
 * Contains information about the selected characters and their animations.
 * 
 * Note: selectedTips is an array of character names (strings), not objects.
 * The app will match these names against the event's character list.
 */
@Serializable
data class TipAnimationEvent(
    val selectedTips: List<String>, // Array of character names
    val amount: Double,
    val sender: String
)

/**
 * Model for event-amount-updated Pusher event payload.
 * 
 * This event is fired when Stripe completes a payment.
 * Updates the amount raised and number of tips.
 */
@Serializable
data class AmountUpdatedEvent(
    val amountRaised: Double,
    val numOfTips: Int
)

/**
 * Model for screen-settings-updated Pusher event payload.
 * 
 * This event updates various UI settings for the event screen.
 */
@Serializable
data class ScreenSettingsUpdatedEvent(
    val eventBackground: String? = null,
    val backgroundType: String? = null,
    val hideProgressBarAmount: Boolean? = null,
    val showLatestAmount: Boolean? = null,
    val moneyFor: String? = null,
    val tipUsageDisclosure: List<TipUsageDisclosure>? = null,
    val amountRaised: Double? = null,
    val numOfTips: Int? = null,
    val latestTipAmount: Double? = null
)

/**
 * Model for latest-tip-updated Pusher event payload.
 * 
 * This event is fired when a tip is made and updates the latest tip amount
 * that is displayed on the screen.
 */
@Serializable
data class LatestTipUpdatedEvent(
    val latestTipAmount: Double // The amount just tipped
)

