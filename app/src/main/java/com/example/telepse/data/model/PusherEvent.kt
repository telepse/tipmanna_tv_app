package com.example.telepse.data.model

import kotlinx.serialization.Serializable

/**
 * Model for tip-animation Pusher event payload.
 * 
 * This event is fired when a user makes a payment/tip.
 * Contains information about the selected characters and their animations.
 */
@Serializable
data class TipAnimationEvent(
    val selectedTips: List<SelectedTip>,
    val amount: Double,
    val sender: String
)

/**
 * Represents a single selected tip/character in the tip-animation event.
 */
@Serializable
data class SelectedTip(
    val characterId: String,
    val styleName: String,
    val name: String,
    val price: Double,
    val time: String
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
    val amountRaised: Double? = null,
    val numOfTips: Int? = null,
    val hideProgressBarAmount: Boolean? = null,
    val showLatestAmount: Boolean? = null,
    val tipUsageDisclosure: List<TipUsageDisclosure>? = null,
    val backgroundType: String? = null
)

