package com.example.telepse.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val id: String,
    val eventName: String,
    val organizationName: String? = null,
    val hostName: String,
    val eventType: String? = null,
    val eventDate: String,
    val startTime: String,
    val endTime: String? = null,
    val venueName: String? = null,
    val venueLocation: String,
    val activities: List<String>? = null,
    val cohosts: List<Cohost>? = null,
    val sessions: List<Session>? = null,
    val performers: List<Performer>? = null,
    val tipUsageDisclosure: List<TipUsageDisclosure>? = null,
    val liveVideoLink: String? = null,
    val resourcesRequested: List<String>? = null,
    val amountToRaise: Double? = null,
    val bankAccountDetails: String? = null,
    val createdAt: String? = null,
    val approved: Boolean? = null,
    val rejected: Boolean? = null,
    val featured: Boolean? = null,
    val position: Int? = null,
    val eventCode: String,
    val eventSlug: String? = null,
    val eventBanner: String? = null,
    val backgroundType: String? = null,
    val eventBackground: String? = null,
    val eventDescription: String? = null,
    val amountRaised: Double? = null,
    val numOfTips: Int? = null,
    val numOfAttendees: Int? = null,
    val moneyFor: String? = null,
    val hostCredit: Double? = null,
    val hostBonus: Double? = null,
    val bonusAdded: Double? = null,
    val bonusRemoved: Double? = null,
    val feesPaid: Double? = null,
    val hostCreditAfterApproval: Double? = null,
    val hostBonusAfterApproval: Double? = null,
    val scanLink: String? = null,
    val characterSets: List<CharacterSet>? = null,
    val showLatestAmount: Boolean? = null,
    val hideProgressBarAmount: Boolean? = null
)

@Serializable
data class Cohost(
    val id: String,
    val email: String
)

@Serializable
data class Session(
    val id: String,
    val title: String,
    val description: String? = null,
    val startTime: String,
    val endTime: String
)

@Serializable
data class Performer(
    val id: String,
    val name: String,
    val description: String? = null
)

@Serializable
data class TipUsageDisclosure(
    val id: String,
    val usageType: String,
    val percentage: Int,
    val customLabel: String? = null,
    val eventId: String? = null
)

@Serializable
data class CharacterSet(
    val id: String,
    val name: String,
    val createdAt: String? = null,
    val characters: List<Character>? = null
)

@Serializable
data class Character(
    val id: String,
    val name: String,
    val iconUrl: String,
    val price: Double,
    val createdAt: String? = null,
    val styleName: String? = null
)
